package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.cul;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilter;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.NaiveRandomBloomFilter;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFusePosition;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.HintCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.cul.SingleCulSseDesc.*;

/**
 * Simple naive client-specific preprocessing KSPIR client.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SingleCulSseClient<T> extends AbstractCpKsPirClient<T> implements CpKsPirClient<T> {
    /**
     * simple index PIR client
     */
    private final SimpleNaiveCpKsPirClient<T> simpleNaiveCpKsPirClient;
    /**
     * fuse position
     */
    private Arity3ByteFusePosition<T> arity3ByteFusePosition;
    /**
     * hash
     */
    private Hash hash;
    /**
     * bloom filter key
     */
    private byte[] seed;
    /**
     * bloom filter type
     */
    /**
     * max size
     */
    private final int maxSize = 100;
//    TODO: make into a config
    BloomFilterFactory.BloomFilterType bloomFilterType = BloomFilterFactory.BloomFilterType.NAIVE_RANDOM_BLOOM_FILTER;

    public SingleCulSseClient(Rpc clientRpc, Party serverParty, SingleCulSseConfig config) {
        super(getInstance(), clientRpc, serverParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        SimpleNaiveCpKsPirConfig simpleNaiveCpKsPirConfig = new SimpleNaiveCpKsPirConfig.Builder(gaussianLweParam).build();
        simpleNaiveCpKsPirClient = new SimpleNaiveCpKsPirClient<T>(clientRpc, serverParty, simpleNaiveCpKsPirConfig);
        addSubPto(simpleNaiveCpKsPirClient);
    }

    @Override
    public void init(int n, int l, int maxBatchNum) throws MpcAbortException {
        setInitInput(n, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> seedPayload = receiveOtherPartyPayload(PtoStep.SERVER_SEND_BLOOM_FILTER_SEED.ordinal());
        seed = seedPayload.get(0);
        int length = NaiveRandomBloomFilter.bitSize(maxSize);
        simpleNaiveCpKsPirClient.init(n , length, maxBatchNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 2, initTime, "Client setups params");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public byte[][] pir(ArrayList<T> keys) throws MpcAbortException {
        setPtoInput(keys);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        byte[][] entries = simpleNaiveCpKsPirClient.pir(keys);
        stopWatch.stop();
        long recoverTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, recoverTime, "Client recovers answer");

        logPhaseInfo(PtoState.PTO_END);

        BloomFilter<byte[]> bloomFilter = BloomFilterFactory.createBloomFilter(
                envType, bloomFilterType, maxSize, seed
        );
        bloomFilter.setStorage(entries[0]);
        byte[] arr = {0,34};
        byte[] arr1 = {0,-108};
        System.out.println(bloomFilter.mightContain(arr));
        System.out.println(bloomFilter.mightContain(arr1));
        return entries;
    }
//
//    private void query(T key) {
//        simpleNaiveCpKsPirClient.query(key);
//    }

//    private byte[] decode(T key) throws MpcAbortException {
//        byte[] entry = simpleNaiveCpKsPirClient.decode( key);
////        int[] xs = arity3ByteFusePosition.positions(key);
////        byte[][] entries = new byte[arity3ByteFusePosition.arity()][];
////        entries[0] = simpleCpIdxPirClient.recover(xs[0], 0);
////        for (int i = 1; i < arity3ByteFusePosition.arity(); i++) {
////            entries[i] = simpleCpIdxPirClient.recover(xs[i], i);
////            addi(entries[0], entries[i], byteL + DIGEST_BYTE_L);
////        }
////        byte[] actualDigest = BytesUtils.clone(entries[0], 0, DIGEST_BYTE_L);
////        byte[] expectDigest = hash.digestToBytes(ObjectUtils.objectToByteArray(key));
////        if (BytesUtils.equals(actualDigest, expectDigest)) {
////            return BytesUtils.clone(entries[0], DIGEST_BYTE_L, byteL);
////        } else {
////            return null;
////        }
//        return new byte[1024];
//    }

//    private void addi(byte[] p, byte[] q, int byteLength) {
//        assert p.length == byteLength && q.length == byteLength;
//        for (int i = 0; i < byteLength; i++) {
//            p[i] += q[i];
//        }
//    }

//    @Override
//    public void updateKeys() {
////        simpleCpIdxPirClient.updateKeys();
//    }
}
