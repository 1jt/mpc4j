package edu.alibaba;

import edu.alibaba.culsse.SingleCulSseConfig;
import edu.alibaba.main.CulSseMain;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory;
import edu.alibaba.mpc4j.s2pc.pir.main.cppir.index.CpIdxPirMain;

import java.util.Properties;

public class SseConfigUtils {
    /**
     * private constructor.
     */
    private SseConfigUtils() {
        // empty
    }

    /**
     * create config.
     *
     * @param properties properties.
     * @return config.
     */

    public static SseConfig createConfig(Properties properties) {
        SseFactory.SseType sseType = MainPtoConfigUtils.readEnum(SseFactory.SseType.class, properties, CulSseMain.PTO_NAME_KEY);
        switch (sseType){
            case CUL_NAIVE -> {
                return new SingleCulSseConfig.Builder().build();
            }
            default -> throw new IllegalArgumentException("Invalid " + SseFactory.SseType.class.getSimpleName() + ": " + sseType.name());
        }
    }
}
