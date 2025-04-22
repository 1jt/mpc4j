package edu.alibaba.mpc4j.s2pc.pir.cppir.index.svpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLwePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;

public class SVCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig, GaussianLwePirConfig {
    /**
     * Gaussian LWE parameter
     */
    private final GaussianLweParam gaussianLweParam;

    public SVCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
        gaussianLweParam = builder.gaussianLweParam;
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.SV_PIR;
    }

    @Override
    public GaussianLweParam getGaussianLweParam() {
        return gaussianLweParam;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SVCpIdxPirConfig> {
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
        public SVCpIdxPirConfig build() {
            return new SVCpIdxPirConfig(this);
        }
    }
}
