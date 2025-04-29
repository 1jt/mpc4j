package edu.alibaba;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;

import java.util.Map;

public interface CulSseDataUser<T> extends MultiPartyPto {
    /**
     * Get Server
     */
    Party getServer();

    /**
     * Get Data Owner
     */
    Party getDataOwner();

    /**
     * Get Other Data Users
     */
    Party[] getOtherDataUsers();

    /**
     * DataUser initializes the protocol.
     *
     * @param keywordNum keyword num.
     * @param maxBatchNum max batch num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int keywordNum, int maxBatchNum) throws MpcAbortException;

    /**
     * DataUser initializes the protocol.
     *
     * @param keywordNum keyword num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default void init(int keywordNum) throws MpcAbortException {
        init(keywordNum, 1);
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