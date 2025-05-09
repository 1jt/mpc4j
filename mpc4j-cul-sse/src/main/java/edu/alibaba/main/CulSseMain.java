package edu.alibaba.main;

import edu.alibaba.*;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CulSseMain extends AbstractMainMultiPartyPto{
    private static final Logger LOGGER = LoggerFactory.getLogger(CulSseMain.class);
    /**
     * protocol name
     */
    public static final String PTO_NAME_KEY = "single_cul_sse_pto_name";
    /**
     * type name
     */
    public static final String PTO_TYPE_NAME = "SINGLE_CUL_SSE";
    /**
     * parallel
     */
    private final boolean parallel;
    /**
     * database paths
     */
    private final String[] data_paths;
    /**
     * keyword paths
     */
    private final String[] keyword_paths;
    /**
     * keyword set sizes
     */
    private final int[] keywordSetSizes;
    /**
     * keyword set size num
     */
    private final int keywordSetSizeNum;
    /**
     * query num
     */
    private final int queryNum;
    /**
     * config
     */
    private final SseConfig config;

    public CulSseMain(Properties properties, String ownName){
        super(properties, ownName);
        // read common config
        LOGGER.info("{} read common config", ownRpc.ownParty().getPartyName());
        parallel = PropertiesUtils.readBoolean(properties, "parallel");
        data_paths = PropertiesUtils.readTrimStringArray(properties, "data_path");
        keyword_paths = PropertiesUtils.readTrimStringArray(properties, "keyword_path");
        keywordSetSizes = Arrays.stream(data_paths)
            .mapToInt(CulSseMain::countLines)
            .toArray();
        keywordSetSizeNum = keywordSetSizes.length;
        queryNum = PropertiesUtils.readInt(properties, "query_num");
        // read PTO config
        LOGGER.info("{} read PTO config", ownRpc.ownParty().getPartyName());
        config = SseConfigUtils.createConfig(properties);
    }

    private static int countLines(String filePath) {
        try (var lines = Files.lines(Paths.get(filePath))) {
            return (int) lines.count();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }

    @Override
    public void runParty1(Rpc doRpc, Party... otherParties) throws IOException, MpcAbortException {
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
                + "_" + config.getPtoType().name()
                + "_" + appendString
                + "_" + doRpc.ownParty().getPartyId()
                + "_" + ForkJoinPool.getCommonPoolParallelism()
                + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Num\tIs Parallel\tThread Num"
                + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
                + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", doRpc.ownParty().getPartyName());
        doRpc.connect();
        int taskId = 0;
        for (int i = 0; i < keywordSetSizeNum; i++) {
            int keywordSetSize = keywordSetSizes[i];
            Map<String, byte[]> keywordValueMap = readServerDatabase(data_paths[i]);
            runDO(doRpc, config, taskId, parallel, keywordValueMap, queryNum, keywordSetSize, printWriter, otherParties);
            taskId++;
        }

        doRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Map<String, byte[]> readServerDatabase(String path) throws IOException {
        LOGGER.info("Server read database");
        InputStreamReader inputStreamReader = new InputStreamReader(
            new FileInputStream(path),
            CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Map<String, byte[]> keywordValueMap = bufferedReader.lines()
            .map(CulSseMain::parseLineToEntry)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue
            ));

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

    private void runDO(Rpc doRpc, SseConfig config, int taskId, boolean parallel,
                        Map<String, byte[]> keywordValueMap, int queryNum, int keywordNum,
                        PrintWriter printWriter, Party... otherParties)
            throws MpcAbortException {
        System.out.println("runDO");
        LOGGER.info(
            "{}: keywordNum = {}, queryNum = {}, parallel = {}",
            doRpc.ownParty().getPartyName(), keywordNum, queryNum, parallel
        );
        CulSseDataOwner<String> dataOwner = SseFactory.createDataOwner(doRpc, config, otherParties[0], otherParties[1]);
        dataOwner.setTaskId(taskId);
        dataOwner.setParallel(parallel);
        dataOwner.getRpc().synchronize();
        dataOwner.getRpc().reset();
        LOGGER.info("{} init", dataOwner.ownParty().getPartyName());

        stopWatch.start();
        dataOwner.init(keywordValueMap);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        dataOwner.getRpc().synchronize();
        dataOwner.getRpc().reset();
        dataOwner.destroy();
        LOGGER.info("{} finish", dataOwner.ownParty().getPartyName());
    }

    @Override
    public void runParty2(Rpc serverRpc, Party... otherParties) throws IOException, MpcAbortException {
        System.out.println("runParty2");
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
                + "_" + config.getPtoType().name()
                + "_" + appendString
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
        for (int i = 0; i < keywordSetSizeNum; i++) {
            int keywordSetSize = keywordSetSizes[i];
            runServer(serverRpc, config, taskId, parallel, queryNum, keywordSetSize, printWriter, otherParties);
            taskId++;
        }

        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private void runServer(Rpc serverRpc, SseConfig config, int taskId, boolean parallel,
                           int queryNum, int keywordNum,PrintWriter printWriter, Party... otherParties)
            throws MpcAbortException {
        System.out.println("runServer");
        LOGGER.info(
            "{}: queryNum = {}, parallel = {}",
            serverRpc.ownParty().getPartyName(), queryNum, parallel
        );
        CulSseServer<String> server = SseFactory.createServer(serverRpc, config, otherParties[0], otherParties[1]);
        server.setTaskId(taskId);
        server.setParallel(parallel);
        server.getRpc().synchronize();
        server.getRpc().reset();
        LOGGER.info("{} init", server.ownParty().getPartyName());
        stopWatch.start();
        server.init(keywordNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        LOGGER.info("{} execute", server.ownParty().getPartyName());
        stopWatch.start();
        for (int i = 0; i < queryNum; i++) {
            server.sse();
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        server.getRpc().synchronize();
        server.getRpc().reset();
        server.destroy();
        LOGGER.info("{} finish", server.ownParty().getPartyName());
    }

    @Override
    public void runParty3(Rpc duRpc, Party... otherParties) throws IOException, MpcAbortException {
        System.out.println("runParty3");
        String filePath = MainPtoConfigUtils.getFileFolderName() + File.separator + PTO_TYPE_NAME
                + "_" + config.getPtoType().name()
                + "_" + appendString
                + "_" + duRpc.ownParty().getPartyId()
                + "_" + ForkJoinPool.getCommonPoolParallelism()
                + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        String tab = "Party ID\tServer Set Size\tQuery Num\tIs Parallel\tThread Num"
                + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
                + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", duRpc.ownParty().getPartyName());
        duRpc.connect();

        int taskId = 0;
        for (int i = 0; i < keywordSetSizeNum; i++) {
            int keywordSetSize = keywordSetSizes[i];
            List<String> keywordList = readClientRetrievalKeywordList(readClientRetrievalIndexList(queryNum, keywordSetSize), keyword_paths[i]);
            runDU(duRpc, config, taskId, parallel, keywordList, keywordSetSize ,printWriter, otherParties);
            taskId++;
        }

        duRpc.disconnect();
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
    private static List<String> readClientRetrievalKeywordList(List<Integer> indexList, String path) throws IOException {
        LOGGER.info("Client read retrieval keyword list");
        List<String> retrievalkeywordList = new ArrayList<>();

        InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(path),
                CommonConstants.DEFAULT_CHARSET);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String line;
        int currentLine = 0;
        // 使用HashSet提高查找效率（如果需要处理大量索引）
        Set<Integer> indexSet = new HashSet<>(indexList);

        while ((line = bufferedReader.readLine()) != null) {
            if (indexSet.contains(currentLine)) {
                retrievalkeywordList.add(line);
            }
            currentLine++;
        }

        return retrievalkeywordList;
    }

    private void runDU(Rpc duRpc, SseConfig config, int taskId, boolean parallel,
                       List<String> keywordList, int keywordNUm ,PrintWriter printWriter, Party... otherParties)
            throws MpcAbortException {
        System.out.println("runDU");
        LOGGER.info(
            "{}: keywordNum = {}, queryNum = {}, parallel = {}",
            duRpc.ownParty().getPartyName(), keywordNUm, queryNum, parallel
        );
        CulSseDataUser<String> dataUser = SseFactory.createDataUser(duRpc, config, otherParties[0], otherParties[1]);
        dataUser.setTaskId(taskId);
        dataUser.setParallel(parallel);
        dataUser.getRpc().synchronize();
        dataUser.getRpc().reset();
        LOGGER.info("{} init", dataUser.ownParty().getPartyName());

        stopWatch.start();
        dataUser.init(keywordNUm);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        LOGGER.info("{} execute", dataUser.ownParty().getPartyName());
        stopWatch.start();
        for (String keyword : keywordList) {
            dataUser.sse(String.valueOf(keyword));
        }
        stopWatch.stop();
        long ptoTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();

        dataUser.getRpc().synchronize();
        dataUser.getRpc().reset();
        dataUser.destroy();
        LOGGER.info("{} finish", dataUser.ownParty().getPartyName());
    }
}
