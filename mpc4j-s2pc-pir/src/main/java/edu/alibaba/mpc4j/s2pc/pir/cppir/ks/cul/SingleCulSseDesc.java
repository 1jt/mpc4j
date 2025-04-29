package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.cul;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.common.structure.fusefilter.Arity3ByteFuseInstance;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;

/**
 * Simple naive client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
class SingleCulSseDesc implements PtoDesc {
    /**
     * protocol ID
     */
    private static final int PTO_ID = Math.abs((int) 7750086851816818949L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "CUL_NAIVE_SSE";
    /**
     * digest byte length
     */
    static final int DIGEST_BYTE_L = 8;

    /**
     * protocol step
     */
    enum PtoStep {
        /**
         * server sends the bloom filter seed
         */
        SERVER_SEND_BLOOM_FILTER_SEED,
    }

    /**
     * the singleton mode
     */
    private static final SingleCulSseDesc INSTANCE = new SingleCulSseDesc();

    /**
     * private constructor.
     */
    private SingleCulSseDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }

}
