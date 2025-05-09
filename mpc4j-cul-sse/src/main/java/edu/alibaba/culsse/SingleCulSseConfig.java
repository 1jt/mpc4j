package edu.alibaba.culsse;

import edu.alibaba.SseConfig;
import edu.alibaba.SseFactory;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLwePirConfig;

public class SingleCulSseConfig extends AbstractMultiPartyPtoConfig implements SseConfig, GaussianLwePirConfig {
    /**
     * Gaussian LWE parameter
     */
    private final GaussianLweParam gaussianLweParam;

    public SingleCulSseConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        gaussianLweParam = builder.gaussianLweParam;
    }

    @Override
    public SseFactory.SseType getPtoType() {
        return SseFactory.SseType.CUL_NAIVE;
    }

    @Override
    public GaussianLweParam getGaussianLweParam() {
        return gaussianLweParam;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SingleCulSseConfig> {
        /**
         * Gaussian LWE parameter
         */
        private final GaussianLweParam gaussianLweParam;

        public Builder() {
            this(GaussianLweParam.N_1024_SIGMA_6_4);
        }

        public Builder(GaussianLweParam gaussianLweParam) {
            this.gaussianLweParam = gaussianLweParam;
        }

        @Override
        public SingleCulSseConfig build() {
            return new SingleCulSseConfig(this);
        }
    }
}
