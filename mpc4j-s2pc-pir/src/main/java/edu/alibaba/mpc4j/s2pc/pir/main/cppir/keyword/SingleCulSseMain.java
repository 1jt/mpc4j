package edu.alibaba.mpc4j.s2pc.pir.main.cppir.keyword;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.AbstractMainTwoPartyPto;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirServer;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * client-preprocessing KSPIR main.
 *
 * @author Liqiang Peng
 * @date 2023/9/27
 */
public class SingleCulSseMain extends AbstractMainTwoPartyPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(SingleCulSseMain.class);
    /**
     * protocol name
     */
    public static final String PTO_NAME_KEY = "single_cp_ks_pir_pto_name";
    /**
     * type name
     */
    public static final String PTO_TYPE_NAME = "SINGLE_CUL_SSE";
    /**
     * keyword num
     */
    private int keywordNum;
    /**
     * parallel
     */
    private final boolean parallel;
    /**
     * max value length
     */
    private final int maxValueLength = 100;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * config
     */
    private final CpKsPirConfig config;

    public SingleCulSseMain(Properties properties, String ownName) {
        super(properties, ownName);
        // read common config
        LOGGER.info("{} read common config", ownRpc.ownParty().getPartyName());
        parallel = PropertiesUtils.readBoolean(properties, "parallel");
        queryNum = PropertiesUtils.readInt(properties, "query_num");
        // read PTO config
        LOGGER.info("{} read PTO config", ownRpc.ownParty().getPartyName());
        config = SingleCpKsPirConfigUtils.createConfig(properties);
    }

    @Override
    public void runParty1(Rpc serverRpc, Party clientParty) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + keywordNum
            + "_" + maxValueLength
            + "_" + serverRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Num\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        serverRpc.connect();
        int taskId = 0;

        Map<String, byte[]> keywordValueMap = readServerDatabase();
        runServer(serverRpc, clientParty, config, taskId, parallel, keywordValueMap, queryNum, printWriter);

        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Map<String, byte[]> readServerDatabase() throws IOException {
        LOGGER.info("Server read database");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream("cul_data/inverted_index1.csv"),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Map<String, byte[]> keywordValueMap = bufferedReader.lines()
            .map(SingleCulSseMain::parseLineToEntry)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
        ));

        keywordNum = keywordValueMap.size();

        bufferedReader.close();
        inputStreamReader.close();
        return keywordValueMap;
    }
    private static Map.Entry<String, byte[]> parseLineToEntry(String line) {
        if (line == null || line.isEmpty()) {
            return null;
        }

        String[] parts = line.split(",");
        if (parts.length < 2) {
            return null; // 必须至少有关键词 + 一个数据
        }

        String keyword = parts[0].trim();
        byte[] values = new byte[(parts.length - 1) * 2];

        for (int i = 1; i < parts.length; i++) {
            String part = parts[i].trim();
            int num = Integer.parseInt(part, 16);
            if (num < 0 || num > 0xFFFF) {
                throw new IllegalArgumentException("数值超出范围: " + num);
            }
            // 写入高位、低位
            values[(i - 1) * 2] = (byte) ((num >> 8) & 0xFF);    // 高8位
            values[(i - 1) * 2 + 1] = (byte) (num & 0xFF);
        }

        return new AbstractMap.SimpleEntry<>(keyword, values);
    }

    private void runServer(Rpc serverRpc, Party clientParty, CpKsPirConfig config, int taskId,
                           boolean parallel, Map<String, byte[]> keywordValueMap, int queryNum,
                           PrintWriter printWriter)
        throws MpcAbortException {
        LOGGER.info(
            "{}: keywordNum = {}, maxValueLength = {}, queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), keywordNum, maxValueLength, queryNum, parallel
        );

        CpKsPirServer<String> server = CpKsPirFactory.createServer(serverRpc, clientParty, config);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        // TODO : right server.init(keywordValueMap); // need new interface
        server.init(keywordValueMap, maxValueLength * Byte.SIZE);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = server.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = server.getRpc().getPayloadByteLength();
        long initSendByteLength = server.getRpc().getSendByteLength();
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} execute", server.ownParty().getPartyName());
        stopWatch.start();
        for (int i = 0; i < queryNum; i++) {
            server.pir();
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = server.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = server.getRpc().getPayloadByteLength();
        long ptoSendByteLength = server.getRpc().getSendByteLength();
        String info = server.ownParty().getPartyId()
            + "\t" + keywordValueMap.size()
            + "\t" + queryNum
            + "\t" + server.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("{} finish", server.ownParty().getPartyName());
    }

    @Override
    public void runParty2(Rpc clientRpc, Party serverParty) throws IOException, MpcAbortException {
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        if (keywordNum == 0){
            keywordNum = 318;
        }
        PirUtils.generateIndexInputFiles(keywordNum, queryNum);
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
            + "_" + config.getPtoType().name()
            + "_" + appendString
            + "_" + keywordNum
            + "_" + maxValueLength
            + "_" + clientRpc.ownParty().getPartyId()
            + "_" + ForkJoinPool.getCommonPoolParallelism()
            + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Num\tIs Parallel\tThread Num"
            + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
            + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        clientRpc.connect();
        int taskId = 0;

        List<Integer> indexList = readClientRetrievalIndexList(queryNum, keywordNum);
        runClient(clientRpc, serverParty, config, taskId, indexList, keywordNum, parallel, printWriter);

        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private List<Integer> readClientRetrievalIndexList(int retrievalSize, int keywordNum) throws IOException {
        LOGGER.info("Client read retrieval list");
        InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(PirUtils.getClientFileName(PirUtils.BYTES_CLIENT_PREFIX, retrievalSize, keywordNum)),
                CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        List<Integer> indexList = bufferedReader.lines()
                .map(Hex::decode)
                .map(IntUtils::byteArrayToInt)
                .collect(Collectors.toCollection(ArrayList::new));
        bufferedReader.close();
        inputStreamReader.close();
        return indexList;
    }

    private void runClient(Rpc clientRpc, Party serverParty, CpKsPirConfig config, int taskId,
                           List<Integer> indexList, int serverSetSize, boolean parallel,
                           PrintWriter printWriter)
            throws MpcAbortException, IOException {
        int queryNum = indexList.size();
        LOGGER.info(
            "{}: keywordNum = {}, queryNum = {}, parallel = {}",
            clientRpc.ownParty().getPartyName(), keywordNum, queryNum, parallel
        );
        CpKsPirClient<String> client = CpKsPirFactory.createClient(clientRpc, serverParty, config);
        client.setTaskId(taskId);
        client.setParallel(parallel);
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} init", client.ownParty().getPartyName());
        stopWatch.start();
        client.init(serverSetSize, keywordNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long initDataPacketNum = client.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = client.getRpc().getPayloadByteLength();
        long initSendByteLength = client.getRpc().getSendByteLength();
        client.getRpc().synchronize();
        client.getRpc().reset();
        LOGGER.info("{} execute", client.ownParty().getPartyName());
        stopWatch.start();
        for (String keyword : readClientRetrievalKeywordList(indexList)) {
            client.pir(String.valueOf(keyword));
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        long ptoDataPacketNum = client.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = client.getRpc().getPayloadByteLength();
        long ptoSendByteLength = client.getRpc().getSendByteLength();
        String info = client.ownParty().getPartyId()
            + "\t" + serverSetSize
            + "\t" + queryNum
            + "\t" + client.getParallel()
            + "\t" + ForkJoinPool.getCommonPoolParallelism()
            + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
            + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        client.getRpc().synchronize();
        client.getRpc().reset();
        client.destroy();
        LOGGER.info("{} finish", client.ownParty().getPartyName());
    }

    private static List<String> readClientRetrievalKeywordList(List<Integer> indexList) throws IOException {
        LOGGER.info("Client read retrieval keyword list");
        List<String> retrievalkeywordList = new ArrayList<>();

        InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream("cul_data/inverted_index1.csv"),
                CommonConstants.DEFAULT_CHARSET);
         BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String line;
        int currentLine = 0;
        // 使用HashSet提高查找效率（如果需要处理大量索引）
        Set<Integer> indexSet = new HashSet<>(indexList);
        int maxIndex = Collections.max(indexList); // 找到最大的索引，避免读取不必要的内容

        while ((line = bufferedReader.readLine()) != null && currentLine <= maxIndex) {
            if (indexSet.contains(currentLine)) {
                // 分割行，取第一个元素作为keyword
                String[] parts = line.split(",");
                if (parts.length > 0) {
                    retrievalkeywordList.add(parts[0]);
                }
            }
            currentLine++;
        }


        return retrievalkeywordList;
    }
}
