package edu.alibaba;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;

public class LJT2PtoDesc implements PtoDesc {
    private static final int PTO_ID = Math.abs((int) 1276797068183810774L);
    /**
     * protocol name
     */
    private static final String PTO_NAME = "LSIMPLE_CP_IDX_PIR";

    /**
     * the singleton mode
     */
    private static final LJT2PtoDesc INSTANCE = new LJT2PtoDesc();

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

    public static void main(String[] args) {
        System.out.println(LJTPtoDesc.PTO_ID);
        PtoDescManager.printAllPtoDesc();
    }
}
