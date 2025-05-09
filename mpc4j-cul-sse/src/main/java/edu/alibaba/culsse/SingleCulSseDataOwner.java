package edu.alibaba.culsse;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Bytes;
import edu.alibaba.AbstractCulSseDataOwner;
import edu.alibaba.AbstractCulSseServer;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilter;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.ConfigurableBloomFilter;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFuseFilter;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.culsse.SingleCulSseDesc.*;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static edu.alibaba.culsse.SingleCulSseDesc.getInstance;
import static edu.alibaba.culsse.SingleCulSseDesc.*;

public class SingleCulSseDataOwner<T> extends AbstractCulSseDataOwner<T> {
    /**
     * bloom filter key
     */
    private byte[] seed;
    /**
     * bloom filter type
     */
    private final BloomFilterFactory.BloomFilterType bloomFilterType = BloomFilterFactory.BloomFilterType.CONFIGURABLE_BLOOM_FILTER;
    /**
     * max size
     */
    private final int maxSize = 100;
    /**
     * value byte length
     */
    protected int byteL = CommonUtils.getByteLength(ConfigurableBloomFilter.bitSize(maxSize));


    // keyword pir params
    /**
     * database size
     */
    protected int ks_n;
    /**
     * value bit length
     */
    protected int ks_l;
    /**
     * value byte length
     */
    protected int ks_byteL;
    /**
     * ByteBuffer for ⊥
     */
    protected ByteBuffer ks_botByteBuffer;
    /**
     * max batch num
     */
    private int ks_maxBatchNum;
    /**
     * batch num
     */
    protected int ks_batchNum;
    /**
     * arity
     */
    private int ks_arity;


    // index pir params
    /**
     * database size
     */
    protected int idx_n;
    /**
     * value bit length
     */
    protected int idx_l;
    /**
     * value byte length
     */
    protected int idx_byteL;
    /**
     * mat batch num
     */
    private int idx_maxBatchNum;
    /**
     * LWE dimension
     */
    private final int idx_dimension;
    /**
     * columns
     */
    private int idx_columns;
    /**
     * transpose database database
     */
    private IntMatrix[] idx_tdbs;

    public SingleCulSseDataOwner(Rpc doRpc, SingleCulSseConfig config, Party serverParty, Party duParty) {
        super(getInstance(), config, doRpc, serverParty, duParty);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        idx_dimension = gaussianLweParam.getDimension();
    }

    @Override
    public Party getDataOwner() {
        return null;
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int matchBatchNum) throws MpcAbortException{
        setInitInput(keyValueMap, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // Creates an empty bloom filter
        seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        sendDataUserPayload(PtoStep.DATA_OWNER_SEND_BLOOM_FILTER_SEED.ordinal(), Collections.singletonList(seed), 0);
        Map<T, byte[]> keyBloomMap = new HashMap<>();

        keyValueMap.keySet().forEach(key ->{
            BloomFilter<byte[]> bloomFilter = BloomFilterFactory.createBloomFilter(
                envType, bloomFilterType, maxSize, seed
            );
            addAll(bloomFilter,keyValueMap.get(key));
            keyBloomMap.put(key, bloomFilter.getStorage());
        });
        stopWatch.stop();
        long bloomFilterTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, bloomFilterTime, "DataOwner generates bloom filter");

        stopWatch.start();
        simpleNaiveInit(keyBloomMap, byteL * Byte.SIZE, matchBatchNum);
        stopWatch.stop();
        long initSimplePirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 1, initSimplePirTime, "  DataOwner initializes Simple PIR");

        logPhaseInfo(PtoState.INIT_END);

    }

    public void addAll(BloomFilter<byte[]> bloomFilter, byte[] values) {
        for (int i = 0; i < values.length; i += 2) {
            byte[] pair = new byte[2];
            pair[0] = values[i];
            pair[1] = values[i + 1];
            bloomFilter.put(pair);
        }
    }

    private void simpleNaiveInit(Map<T, byte[]> keyValueMap,int l, int matchBatchNum) throws MpcAbortException{
        SimpleNaiveSetInitInput(keyValueMap, l , matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // concat Hash(key) and value
        Hash hash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        Map<T, byte[]> keyValueConcatMap = new HashMap<>();
        keyValueMap.keySet().forEach(key -> {
            byte[] digest = hash.digestToBytes(ObjectUtils.objectToByteArray(key));
            keyValueConcatMap.put(key, Bytes.concat(digest, keyValueMap.get(key)));
        });
        Arity3ByteFuseFilter<T> fuseFilter = new Arity3ByteFuseFilter<>(
            envType, keyValueConcatMap, byteL + DIGEST_BYTE_L, secureRandom
        );
        ks_arity = fuseFilter.arity();
        sendDataUserPayload(
            PtoStep.DATA_OWNER_SEND_FUSE_FILTER_SEED.ordinal(), Collections.singletonList(fuseFilter.seed()),0
        );
        stopWatch.stop();
        long initSimplePirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initSimplePirTime, "DataOwner generates fuse filter");

        stopWatch.start();
        SimplePirInit(NaiveDatabase.create((ks_byteL+DIGEST_BYTE_L) * Byte.SIZE, fuseFilter.storage()), ks_maxBatchNum);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "DataOwner generates hints");

        logPhaseInfo(PtoState.INIT_END);

    }
    protected void SimpleNaiveSetInitInput(Map<T, byte[]> keyValueMap, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("l", l);
        ks_l = l;
        ks_byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositive("n", keyValueMap.size());
        ks_n = keyValueMap.size();
        byte[] bot = new byte[ks_byteL];
        Arrays.fill(bot, (byte) 0xFF);
        BytesUtils.reduceByteArray(bot, ks_l);
        ks_botByteBuffer = ByteBuffer.wrap(bot);
        // TODO: It makes no sense to judge k_i here
        keyValueMap.forEach((keyword, value) -> {
            ByteBuffer keywordByteBuffer = ByteBuffer.wrap(ObjectUtils.objectToByteArray(keyword));
            Preconditions.checkArgument(!keywordByteBuffer.equals(ks_botByteBuffer), "k_i must not equal ⊥");
            Preconditions.checkArgument(BytesUtils.isFixedReduceByteArray(value, byteL, ks_l));
        });
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        ks_maxBatchNum = maxBatchNum;
    }

    private void SimplePirInit(NaiveDatabase database, int matchBatchNum) throws MpcAbortException{
        SimplePirSetInitInput(database, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // server generates and sends the seed for the random matrix A.
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        sendDataUserPayload(PtoStep.DATA_OWNER_SEND_MATRIX_SEED.ordinal(), seedPayload, 0);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, seedTime, "DataOwner generates matrix seed");

        stopWatch.start();
        int subByteL = Math.min(idx_byteL, SimpleCpIdxPirPtoDesc.getMaxSubByteL(idx_n));
        int[] sizes = SimpleCpIdxPirPtoDesc.getMatrixSize(idx_n, idx_byteL);
        int rows = sizes[0];
        idx_columns = sizes[1];
        List<byte[]> serverParams = new ArrayList<>();
        serverParams.add(intToBytes(ks_arity));
        serverParams.add(intToBytes(idx_columns));
        sendServerPayload(PtoStep.DATA_OWNER_SEND_SERVER_PARAMS.ordinal(), serverParams);

        int partition = sizes[2];
        // create database
        IntMatrix[] dbs = IntStream.range(0, partition)
            .mapToObj(p -> IntMatrix.createZeros(rows, idx_columns))
            .toArray(IntMatrix[]::new);
        int i = 0;
        int j = 0;
        for (int dataIndex = 0; dataIndex < database.rows(); dataIndex++) {
            byte[] element = database.getBytesData(dataIndex);
            assert element.length == idx_byteL;
            byte[] paddingElement = BytesUtils.paddingByteArray(element, subByteL * partition);
            // encode each row into partition databases
            for (int entryIndex = 0; entryIndex < subByteL; entryIndex++) {
                for (int p = 0; p < partition; p++) {
                    dbs[p].set(i, j, (paddingElement[p * subByteL + entryIndex] & 0xFF));
                }
                i++;
                // change column index
                if (i == rows) {
                    i = 0;
                    j++;
                }
            }
        }
        // create hint
        IntMatrix matrixA = IntMatrix.createRandom(idx_columns, idx_dimension, seed);
        idx_tdbs = new IntMatrix[partition];
        IntStream intStream = parallel ? IntStream.range(0, partition).parallel() : IntStream.range(0, partition);
        IntMatrix[] hint = intStream.mapToObj(p -> dbs[p].mul(matrixA)).toArray(IntMatrix[]::new);
        // transpose database
        IntStream.range(0, partition).forEach(p -> {
            IntStream hintIntStream = parallel ? IntStream.range(0, rows).parallel() : IntStream.range(0, rows);
            List<byte[]> hintPayload = hintIntStream
                    .mapToObj(rowIndex -> IntUtils.intArrayToByteArray(hint[p].getRow(rowIndex).getElements()))
                    .collect(Collectors.toList());
            sendDataUserPayload(PtoStep.DATA_OWNER_SEND_HINT.ordinal(), hintPayload,0);
            idx_tdbs[p] = dbs[p].transpose();
        });

        List<byte[]> databasePayload = intMatrixArrayToByteListWithHeader(idx_tdbs);
        sendServerPayload(PtoStep.DATA_OWNER_SEND_DATABASE.ordinal(), databasePayload);

        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

        logPhaseInfo(PtoState.INIT_END);
    }
    private void SimplePirSetInitInput(NaiveDatabase database, int maxBatchNum) {
        idx_n = database.rows();
        idx_l = database.getL();
        idx_byteL = database.getByteL();
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        idx_maxBatchNum = maxBatchNum;
    }

    /**
     * IntMatrix[] → List<byte[]>
     */
    public static List<byte[]> intMatrixArrayToByteListWithHeader(IntMatrix[] matrices) {
        return Arrays.stream(matrices)
            .flatMap(matrix -> {
                int rows = matrix.getRows();
                int columns = matrix.getColumns();
                // Header: 2 ints (rows, columns)
                byte[] header = ByteBuffer.allocate(2 * Integer.BYTES)
                        .putInt(rows)
                        .putInt(columns)
                        .array();

                // Matrix rows
                Stream<byte[]> rowBytes = Arrays.stream(matrix.getElements())
                        .map(row -> {
                            ByteBuffer buffer = ByteBuffer.allocate(row.length * Integer.BYTES);
                            for (int v : row) buffer.putInt(v);
                            return buffer.array();
                        });

                return Stream.concat(Stream.of(header), rowBytes);
            })
            .toList(); // Java 16+
    }

    /**
     * Converts an int to a byte array.
     * @param value
     * @return
     */
    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(Integer.BYTES).putInt(value).array();
    }



    @Override
    public void sse(int batchNum) throws MpcAbortException {

    }

}
