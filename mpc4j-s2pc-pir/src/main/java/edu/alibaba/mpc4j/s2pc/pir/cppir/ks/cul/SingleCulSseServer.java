package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.cul;

import com.google.common.primitives.Bytes;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilter;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory;
import edu.alibaba.mpc4j.common.structure.filter.NaiveRandomBloomFilter;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFuseFilter;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.AbstractCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.HintCpKsPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirServer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.s2pc.pir.cppir.ks.cul.SingleCulSseDesc.*;


/**
 * Simple naive client-specific preprocessing KSPIR server.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SingleCulSseServer<T> extends AbstractCpKsPirServer<T> implements HintCpKsPirServer<T> {
    /**
     * simple naive keyword PIR server
     */
    private final SimpleNaiveCpKsPirServer<T> simpleNaiveCpKsPirServer;
    /**
     * arity
     */
    private int arity;
    /**
     * bloom filter key
     */
    private byte[] seed;
    /**
     * bloom filter type
     */
    private final BloomFilterFactory.BloomFilterType bloomFilterType = BloomFilterFactory.BloomFilterType.NAIVE_RANDOM_BLOOM_FILTER;
    /**
     * max size
     */
    private final int maxSize = 100;

    public SingleCulSseServer(Rpc serverRpc, Party clientParty, SingleCulSseConfig config) {
        super(getInstance(), serverRpc, clientParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        SimpleNaiveCpKsPirConfig simpleNaiveCpKsPirConfig = new SimpleNaiveCpKsPirConfig.Builder(gaussianLweParam).build();
        simpleNaiveCpKsPirServer = new SimpleNaiveCpKsPirServer<T>(serverRpc, clientParty, simpleNaiveCpKsPirConfig);
        addSubPto(simpleNaiveCpKsPirServer);
    }

    @Override
    public void init(Map<T, byte[]> keyValueMap, int l, int matchBatchNum) throws MpcAbortException {
        setInitInput(keyValueMap, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // Creates an empty bloom filter
        seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        sendOtherPartyPayload(PtoStep.SERVER_SEND_BLOOM_FILTER_SEED.ordinal(), Collections.singletonList(seed));
        Map<T, byte[]> keyBloomMap = new HashMap<>();
        int length = NaiveRandomBloomFilter.bitSize(maxSize);

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
        logStepInfo(PtoState.INIT_STEP, 1, 1, bloomFilterTime, "Server generates bloom filter");

        stopWatch.start();
        simpleNaiveCpKsPirServer.init(keyBloomMap, length, matchBatchNum);
        stopWatch.stop();
        long initSimplePirTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 1, initSimplePirTime, "Server initializes Simple PIR");

        logPhaseInfo(PtoState.INIT_END);
    }

    public void addAll(BloomFilter<byte[]> bloomFilter, byte[] values) {
        for (int i = 0; i < values.length; i += 2) {
            byte[] pair = new byte[2];
            pair[0] = values[i];
            pair[1] = values[i + 1];
            bloomFilter.put(pair);
        }
    };

    @Override
    public void pir(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        simpleNaiveCpKsPirServer.pir(batchNum);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server responses query");

        logPhaseInfo(PtoState.PTO_END);
    }

//    private void answer() throws MpcAbortException {
//        for (int i = 0; i < arity; i++) {
//            simpleNaiveCpKsPirServer.answer();
//        }
//    }
}
