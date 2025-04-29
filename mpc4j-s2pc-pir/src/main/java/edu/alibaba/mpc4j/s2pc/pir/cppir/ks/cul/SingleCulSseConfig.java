package edu.alibaba.mpc4j.s2pc.pir.cppir.ks.cul;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLwePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.CpKsPirFactory;

/**
 * Simple naive client-specific preprocessing KSPIR protocol description.
 *
 * @author Liqiang Peng
 * @date 2024/8/2
 */
public class SingleCulSseConfig extends AbstractMultiPartyPtoConfig implements CpKsPirConfig, GaussianLwePirConfig {
    /**
     * Gaussian LWE parameter
     */
    private final GaussianLweParam gaussianLweParam;

    public SingleCulSseConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST);
        gaussianLweParam = builder.gaussianLweParam;
    }

    @Override
    public CpKsPirFactory.CpKsPirType getPtoType() {
        return CpKsPirFactory.CpKsPirType.CUL_NAIVE;
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
