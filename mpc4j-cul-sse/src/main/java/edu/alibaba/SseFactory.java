package edu.alibaba;

import edu.alibaba.culsse.SingleCulSseConfig;
import edu.alibaba.culsse.SingleCulSseDataOwner;
import edu.alibaba.culsse.SingleCulSseDataUser;
import edu.alibaba.culsse.SingleCulSseServer;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

public class SseFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private SseFactory() {
        // empty
    }

    /**
     * protocol type
     */
    public enum SseType {
        /**
         * cul naive SSE
         */
        CUL_NAIVE,
    }

    /**
     * create a dataOwner.
     *
     * @param doRpc   dataOwner RPC.
     * @param serverParty server party.
     * @param duParty dataUser party.
     * @param config      config.
     * @return a dataOwner.
     */
    // TODO: make Party duParty --> Party... duParties
    public static <T> CulSseDataOwner<T> createDataOwner(Rpc doRpc, SseConfig config,Party serverParty,Party duParty) {
        SseType type = config.getPtoType();
        switch (type){
            case CUL_NAIVE -> {
                return new SingleCulSseDataOwner<>(doRpc, (SingleCulSseConfig) config, serverParty, duParty);
            }
            default -> throw new IllegalArgumentException("Invalid " + SseType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create a server.
     *
     * @param serverRpc   server RPC.
     * @param doParty dataOwner party.
     * @param duParty dataUser party.
     * @param config      config.
     * @return a server.
     */
    public static <T> CulSseServer<T> createServer(Rpc serverRpc, SseConfig config,Party doParty, Party duParty) {
        SseType type = config.getPtoType();
        switch (type){
            case CUL_NAIVE -> {
                return new SingleCulSseServer<>(serverRpc, (SingleCulSseConfig) config, doParty, duParty);
            }
            default -> throw new IllegalArgumentException("Invalid " + SseType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * create a dataUser.
     *
     * @param duRpc   dataUser RPC.
     * @param doParty dataOwner party.
     * @param serverParty server party.
     * @param config      config.
     * @return a dataUser.
     */
    public static <T> CulSseDataUser<T> createDataUser(Rpc duRpc, SseConfig config,Party doParty,Party serverParty) {
        SseType type = config.getPtoType();
        switch (type){
            case CUL_NAIVE -> {
                return new SingleCulSseDataUser<>(duRpc, (SingleCulSseConfig) config, doParty, serverParty);
            }
            default -> throw new IllegalArgumentException("Invalid " + SseType.class.getSimpleName() + ": " + type.name());
        }
    }
}
