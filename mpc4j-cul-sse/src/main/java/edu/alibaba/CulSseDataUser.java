package edu.alibaba;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPto;

import java.util.ArrayList;
import java.util.Map;

public interface CulSseDataUser<T> extends MultiPartyPto {
    /**
     * Get Data Owner
     */
    default Party getDataOwner(){
        return otherParties()[0];
    }

    /**
     * Get Server
     */
    default Party getServer(){
        return otherParties()[1];
    }

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
     * @param keys keyword array
     * @return retrieval results.
     * @throws MpcAbortException the protocol failure aborts.
     */
    byte[][] sse(ArrayList<T> keys) throws MpcAbortException;

    /**
     * Server executes the protocol.
     * @param key keyword.
     * @return retrieval results.
     * @throws MpcAbortException the protocol failure aborts.
     */
    default byte[] sse(T key) throws MpcAbortException {
        ArrayList<T> xs = new ArrayList<>(1);
        xs.add(key);
        byte[][] temp = sse(xs);
        return temp[0];
    }
}