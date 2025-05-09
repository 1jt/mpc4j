package edu.alibaba.culsse;

import com.google.common.base.Preconditions;
import edu.alibaba.AbstractCulSseDataUser;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilter;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.ConfigurableBloomFilter;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFusePosition;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;

import static edu.alibaba.culsse.SingleCulSseDesc.getInstance;
import static edu.alibaba.culsse.SingleCulSseDesc.*;

import edu.alibaba.culsse.SingleCulSseDesc.*;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SingleCulSseDataUser <T> extends AbstractCulSseDataUser<T> {
    /**
     * bloom filter key
     */
    private byte[] seed;
    /**
     * max size
     */
    private final int maxSize = 100;
    /**
     * value byte length
     */
    protected int byteL = CommonUtils.getByteLength(ConfigurableBloomFilter.bitSize(maxSize));
    /**
     * bloom filter type
     */
    private static BloomFilterFactory.BloomFilterType bloomFilterType = BloomFilterFactory.BloomFilterType.CONFIGURABLE_BLOOM_FILTER;



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
    protected int ks_maxBatchNum;
    /**
     * batch num
     */
    protected int ks_batchNum;
    /**
     * fuse position
     */
    private Arity3ByteFusePosition<T> ks_arity3ByteFusePosition;
    /**
     * hash
     */
    private Hash ks_hash;


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
    protected int idx_maxBatchNum;
    /**
     * batch num
     */
    protected int idx_batchNum;
    /**
     * LWE dimension
     */
    private final int idx_dimension;
    /**
     * σ
     */
    private final double idx_sigma;
    /**
     * partition
     */
    private int idx_partition;
    /**
     * byteL for each partition
     */
    private int idx_subByteL;
    /**
     * rows
     */
    private int idx_rows;
    /**
     * columns
     */
    private int idx_columns;
    /**
     * number of elements in each row
     */
    private int idx_rowElementNum;
    /**
     * transpose matrix A
     */
    private IntMatrix idx_transposeMatrixA;
    /**
     * transpose hint
     */
    private IntMatrix[] idx_transposeHint;
    /**
     * secret key s ← Z_q^n, As
     */
    private IntVector[] idx_ass;
    /**
     * hint · s
     */
    private IntVector[][] idx_hss;



    public SingleCulSseDataUser(Rpc duRpc, SingleCulSseConfig config, Party doParty, Party serverParty) {
        super(getInstance(), config, duRpc, doParty, serverParty);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        idx_dimension = gaussianLweParam.getDimension();
        idx_sigma = gaussianLweParam.getSigma();
    }


    @Override
    public void init(int keywordNum, int maxBatchNum) throws MpcAbortException {
        setInitInput(keywordNum, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> seedPayload = receiveDataOwnerPayload(PtoStep.DATA_OWNER_SEND_BLOOM_FILTER_SEED.ordinal());
        seed = seedPayload.get(0);

        simpleNaiveInit(keywordNum,  byteL * Byte.SIZE, maxBatchNum);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime, "Client setups params");

        logPhaseInfo(PtoState.INIT_END);
    }

    private void simpleNaiveInit(int n, int l, int maxBatchNum) throws MpcAbortException{
        SimpleNaiveSetInitInput(n, l , maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ks_hash = HashFactory.createInstance(envType, DIGEST_BYTE_L);
        List<byte[]> fuseFilterSeedPayload = receiveDataOwnerPayload(PtoStep.DATA_OWNER_SEND_FUSE_FILTER_SEED.ordinal());
        MpcAbortPreconditions.checkArgument(fuseFilterSeedPayload.size() == 1);
        byte[] fuseFilterSeed = fuseFilterSeedPayload.get(0);
        ks_arity3ByteFusePosition = new Arity3ByteFusePosition<>(envType, n, ks_byteL + DIGEST_BYTE_L, fuseFilterSeed);
        int filterLength = ks_arity3ByteFusePosition.filterLength();
        stopWatch.stop();
        long fusePosTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, fusePosTime, "DataUser generates fuse position");

        stopWatch.start();

        SimplePirInit(filterLength, (ks_byteL + DIGEST_BYTE_L) * Byte.SIZE, ks_arity3ByteFusePosition.arity() * maxBatchNum);

        stopWatch.stop();
        long initSimplePirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, initSimplePirTime, "DataUser initializes Simple PIR");

        logPhaseInfo(PtoState.INIT_END);
    }
    protected void SimpleNaiveSetInitInput(int n, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("n", n);
        ks_n = n;
        MathPreconditions.checkPositive("l", l);
        ks_l = l;
        ks_byteL = CommonUtils.getByteLength(l);
        byte[] bot = new byte[ks_byteL];
        Arrays.fill(bot, (byte) 0xFF);
        BytesUtils.reduceByteArray(bot, l);
        ks_botByteBuffer = ByteBuffer.wrap(bot);
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        ks_maxBatchNum = maxBatchNum;
    }

    private void SimplePirInit(int n, int l, int maxBatchNum) throws MpcAbortException {
        SimplePirSetInitInput(n, l, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // we treat plaintext modulus as p = 2^8, so that the database can be seen as N rows and byteL columns.
        idx_subByteL = Math.min(idx_byteL, SimpleCpIdxPirPtoDesc.getMaxSubByteL(n));
        int[] sizes = SimpleCpIdxPirPtoDesc.getMatrixSize(n, idx_byteL);
        idx_rows = sizes[0];
        idx_rowElementNum = idx_rows / idx_subByteL;
        idx_columns = sizes[1];
        idx_partition = sizes[2];
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, paramTime, "DataUser setups params");

        List<byte[]> seedPayload = receiveDataOwnerPayload(PtoStep.DATA_OWNER_SEND_MATRIX_SEED.ordinal());

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(seedPayload.size() == 1);
        byte[] seed = seedPayload.get(0);
        MpcAbortPreconditions.checkArgument(seed.length == CommonConstants.BLOCK_BYTE_LENGTH);
        IntMatrix matrixA = IntMatrix.createRandom(idx_columns, idx_dimension, seed);
        idx_transposeMatrixA = matrixA.transpose();
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, seedTime, "DataUser generates matrix A");

        stopWatch.start();
        idx_transposeHint = new IntMatrix[idx_partition];
        for (int p = 0; p < idx_partition; p++) {
            List<byte[]> hintPayload = receiveDataOwnerPayload(PtoStep.DATA_OWNER_SEND_HINT.ordinal());
            MpcAbortPreconditions.checkArgument(hintPayload.size() == idx_rows);
            byte[][] hintBytes = hintPayload.toArray(new byte[0][]);
            IntVector[] hintVectors = IntStream.range(0, idx_rows)
                    .mapToObj(i -> IntUtils.byteArrayToIntArray(hintBytes[i]))
                    .map(IntVector::create)
                    .toArray(IntVector[]::new);
            IntMatrix hint = IntMatrix.create(hintVectors);
            idx_transposeHint[p] = hint.transpose();
        }
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, hintTime, "Client stores hints");

        updateKeys();

        logPhaseInfo(PtoState.INIT_END);
    }
    protected void SimplePirSetInitInput(int n, int l, int maxBatchNum) {
        MathPreconditions.checkPositive("n", n);
        idx_n = n;
        MathPreconditions.checkPositive("l", l);
        idx_l = l;
        idx_byteL = CommonUtils.getByteLength(l);
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        idx_maxBatchNum = maxBatchNum;
    }
    public void updateKeys() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        idx_ass = new IntVector[idx_maxBatchNum];
        idx_hss = new IntVector[idx_maxBatchNum][idx_partition];
        IntStream batchIntStream = parallel ? IntStream.range(0, idx_maxBatchNum).parallel() : IntStream.range(0, idx_maxBatchNum);
        batchIntStream.forEach(batchIndex -> {
            IntVector s = IntVector.createRandom(idx_dimension, secureRandom);
            idx_ass[batchIndex] = idx_transposeMatrixA.leftMul(s);
            // generate s and s · hint
            IntStream intStream = parallel ? IntStream.range(0, idx_partition).parallel() : IntStream.range(0, idx_partition);
            idx_hss[batchIndex] = intStream.mapToObj(p -> idx_transposeHint[p].leftMul(s)).toArray(IntVector[]::new);
        });
        stopWatch.stop();
        long keyTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, keyTime, "Client updates keys");
    }

    @Override
    public byte[][] sse(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] entries = SimpleNaivePir(keys);
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Cul_DataUser recovers answer");

        logPhaseInfo(PtoState.PTO_END);

        BloomFilter<byte[]> bloomFilter = BloomFilterFactory.createBloomFilter(
                envType, bloomFilterType, maxSize, seed
        );
        bloomFilter.setStorage(entries[0]);
        byte[] arr = {0,34};
        byte[] arr1 = {1,108};
        System.out.println(bloomFilter.mightContain(arr));
        System.out.println(bloomFilter.mightContain(arr1));

        return entries;
    }

    public byte[][] SimpleNaivePir(ArrayList<T> keys) throws MpcAbortException {
        SimpleNaiveSetPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            SimpleNaiveQuery(keys.get(i));
        }
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client generates query");

        stopWatch.start();
        byte[][] entries = new byte[ks_batchNum][];
        for (int i = 0; i < ks_batchNum; i++) {
            entries[i] = SimpleNaiveDecode(keys.get(i));
        }
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        logPhaseInfo(PtoState.PTO_END);
        return entries;
    }
    private void SimpleNaiveSetPtoInput(ArrayList<T> keys) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", keys.size(), ks_maxBatchNum);
        ks_batchNum = keys.size();
        for (T keyword : keys) {
            Preconditions.checkArgument(keyword != null, "x must not equal ⊥");
        }
    }

    private void SimpleNaiveQuery(T key) {
        int[] xs = ks_arity3ByteFusePosition.positions(key);
        for (int i = 0; i < ks_arity3ByteFusePosition.arity(); i++) {
            SimplePirQuery(xs[i], i);
        }
    }

    private void SimplePirQuery(int x, int i) {
        // client generates qu
        int colIndex = x / idx_rowElementNum;
        // qu = A * s + e + q/p * u_i_col
        IntVector e = IntVector.createGaussian(idx_columns, idx_sigma, secureRandom);
        IntVector qu = idx_ass[i].add(e);
        qu.addi(colIndex, 1 << (Integer.SIZE - Byte.SIZE));
        List<byte[]> queryPayload = Collections.singletonList(IntUtils.intArrayToByteArray(qu.getElements()));
        sendServerPayload(PtoStep.DATA_USER_SEND_QUERY.ordinal(), queryPayload);
    }

    private byte[] SimpleNaiveDecode(T key) throws MpcAbortException {
        int[] xs = ks_arity3ByteFusePosition.positions(key);
        byte[][] entries = new byte[ks_arity3ByteFusePosition.arity()][];
        entries[0] = SimplePirRecover(xs[0], 0);
        for (int i = 1; i < ks_arity3ByteFusePosition.arity(); i++) {
            entries[i] = SimplePirRecover(xs[i], i);
            addi(entries[0], entries[i], ks_byteL + DIGEST_BYTE_L);
        }
        byte[] actualDigest = BytesUtils.clone(entries[0], 0, DIGEST_BYTE_L);
        byte[] expectDigest = ks_hash.digestToBytes(ObjectUtils.objectToByteArray(key));
        if (BytesUtils.equals(actualDigest, expectDigest)) {
            return BytesUtils.clone(entries[0], DIGEST_BYTE_L, ks_byteL);
        } else {
            return null;
        }
    }

    private void addi(byte[] p, byte[] q, int byteLength) {
        assert p.length == byteLength && q.length == byteLength;
        for (int i = 0; i < byteLength; i++) {
            p[i] += q[i];
        }
    }

    public byte[] SimplePirRecover(int x, int i) throws MpcAbortException {
        List<byte[]> responsePayload = receiveServerPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal());
        MpcAbortPreconditions.checkArgument(responsePayload.size() == idx_partition);
        IntVector[] ansArray = responsePayload.stream()
                .map(ans -> IntVector.create(IntUtils.byteArrayToIntArray(ans)))
                .toArray(IntVector[]::new);
        for (IntVector ans : ansArray) {
            MpcAbortPreconditions.checkArgument(ans.getNum() == idx_rows);
        }
        int rowIndex = x % idx_rowElementNum;
        ByteBuffer paddingEntryByteBuffer = ByteBuffer.allocate(idx_subByteL * idx_partition);
        for (int p = 0; p < idx_partition; p++) {
            IntVector d = ansArray[p].sub(idx_hss[i][p]);
            byte[] partitionEntry = new byte[idx_subByteL];
            for (int elementIndex = 0; elementIndex < idx_subByteL; elementIndex++) {
                int element = d.getElement(rowIndex * idx_subByteL + elementIndex);
                if ((element & 0x00800000) > 0) {
                    partitionEntry[elementIndex] = (byte) ((element >>> (Integer.SIZE - Byte.SIZE)) + 1);
                } else {
                    partitionEntry[elementIndex] = (byte) (element >>> (Integer.SIZE - Byte.SIZE));
                }
            }
            paddingEntryByteBuffer.put(partitionEntry);
        }
        byte[] paddingEntry = paddingEntryByteBuffer.array();
        byte[] entry = new byte[idx_byteL];
        System.arraycopy(paddingEntry, idx_subByteL * idx_partition - idx_byteL, entry, 0, idx_byteL);
        return entry;
    }

}
