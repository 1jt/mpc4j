package edu.alibaba;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;

import java.util.Map;

public interface CulSseDataOwner<T> extends MultiPartyPto {
    /**
     * Get Server
     */
    default Party getServer(){
        return otherParties()[0];
    }

    /**
     * Get Data Owner
     */
    Party getDataOwner();

    /**
     * Get a Data Users
     */
    default Party getDataUsers(int userId){
        return otherParties()[userId + 1];
    }

    /**
     * Get all Data Users
     */
    default Party[] getDataUsers(){
        return otherParties();
    };

    /**
     * DataOwner initializes the protocol.
     *
     * @param keyValueMap key-value map.
     * @param maxBatchNum max batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(Map<T, byte[]> keyValueMap, int maxBatchNum) throws MpcAbortException;

    /**
     * DataOwner initializes the protocol.
     *
     * @param keyValueMap key-value map.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void init(Map<T, byte[]> keyValueMap) throws MpcAbortException {
        init(keyValueMap, 1);
    }

    /**
     * Server executes the protocol.
     *
     * @param batchNum batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void sse(int batchNum) throws MpcAbortException;

    /**
     * Server executes the protocol.
     *
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void sse() throws MpcAbortException {
        sse(1);
    }
}