package edu.alibaba.culsse;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

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
         * data_owner sends the bloom filter seed
         */
        DATA_OWNER_SEND_BLOOM_FILTER_SEED,
        /**
         * data_owner sends the fuse filter seed
         */
        DATA_OWNER_SEND_FUSE_FILTER_SEED,
        /**
         * data_owner sends matrix seed
         */
        DATA_OWNER_SEND_MATRIX_SEED,
        /**
         * data_owner sends server params
         */
        DATA_OWNER_SEND_SERVER_PARAMS,
        /**
         * data_owner sends hint
         */
        DATA_OWNER_SEND_HINT,
        /**
         * data_owner sends the database
         */
        DATA_OWNER_SEND_DATABASE,

        /**
         * data_user sends the query
         */
        DATA_USER_SEND_QUERY,
        /**
         * server sends the response
         */
        SERVER_SEND_RESPONSE,
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