package edu.alibaba.mpc4j.s2pc.pir.cppir.index.svpir;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.crypto.prp.DefaultFixedKeyPrp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.FixedKeyPrp;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLwePirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.CpIdxPirFactory.CpIdxPirType;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirConfig;

import java.util.Objects;

public class SVCpIdxPirConfig extends AbstractMultiPartyPtoConfig implements CpIdxPirConfig{
    /**
     * fixed key PRP
     */
    private final FixedKeyPrp fixedKeyPrp;
    /**
     * Gaussian LWE parameter
     */
//    private final GaussianLweParam gaussianLweParam;

    public SVCpIdxPirConfig(Builder builder) {
        super(SecurityModel.MALICIOUS);
//        gaussianLweParam = builder.gaussianLweParam;
        fixedKeyPrp = Objects.requireNonNullElseGet(builder.fixedKeyPrp, () -> new DefaultFixedKeyPrp(getEnvType()));
    }

    public FixedKeyPrp getFixedKeyPrp() {
        return fixedKeyPrp;
    }

    @Override
    public CpIdxPirType getPtoType() {
        return CpIdxPirType.SV_PIR;
    }

//    @Override
//    public GaussianLweParam getGaussianLweParam() {
//        return gaussianLweParam;
//    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<SVCpIdxPirConfig> {
//        /**
//         * Gaussian LWE parameter
//         */
//        private final GaussianLweParam gaussianLweParam;

        /**
         * fixed key PRP
         */
        private FixedKeyPrp fixedKeyPrp;

        public Builder() {
            fixedKeyPrp = null;
        }

//        public Builder(GaussianLweParam gaussianLweParam) {
//            this.gaussianLweParam = gaussianLweParam;
//        }

        public SVCpIdxPirConfig.Builder setFixedKeyPrp(FixedKeyPrp fixedKeyPrp) {
            this.fixedKeyPrp = fixedKeyPrp;
            return this;
        }

        @Override
        public SVCpIdxPirConfig build() {
            return new SVCpIdxPirConfig(this);
        }
    }
}
