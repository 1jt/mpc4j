package edu.alibaba.mpc4j.s2pc.pir.cppir.index.svpir;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class SVCpIdxPirPtoDesc implements PtoDesc {
    /**
     * the protocol ID.
     */
    private static final int PTO_ID = Math.abs((int) 2000071312345678901L);
    /**
     * the protocol name.
     */
    private static final String PTO_NAME = "SV_CP_IDX_PIR";

    /**
     * the protocol step.
     */
    enum PtoStep {
        /**
         * server send seed.
         */
        SERVER_SEND_SEED,
        /**
         * server sends the stream database request
         */
        SERVER_SEND_STREAM_DATABASE_REQUEST,
        /**
         * client sends the stream database response
         */
        CLIENT_SEND_STREAM_DATABASE_RESPONSE,
        /**
         * client send query.
         */
        CLIENT_SEND_QUERY_V,
        /**
         * client send query.
         */
        CLIENT_SEND_QUERY_S,
        /**
         * server send response.
         */
        SERVER_SEND_RESPONSE,
    }

    /**
     * the singleton mode.
     */
    private static final SVCpIdxPirPtoDesc INSTANCE = new SVCpIdxPirPtoDesc();

    private SVCpIdxPirPtoDesc() {
        // empty
    }

    public static SVCpIdxPirPtoDesc getInstance() {
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

    /**
     * LWE dimension, Frodo Section 5.2 of the paper requires n = 1774
     */
    static final int N = 1024;
}
