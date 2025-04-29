package edu.alibaba;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;

public class LJTPtoDesc implements PtoDesc {
    public static final int PTO_ID = Math.abs((int) 1276797068183810774L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "SIMPLE_CP_IDX_PIR";

    /**
     * the singleton mode
     */
    private static final LJTPtoDesc INSTANCE = new LJTPtoDesc();

    /**
     * private constructor.
     */
    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }
}
