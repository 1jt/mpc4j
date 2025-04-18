package edu.alibaba.mpc4j.s2pc.aby.edit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

/**
 * Edit distance receiver.
 *
 * @author Li Peng
 * @date 2024/4/8
 */
public interface DistCmpReceiver extends TwoPartyPto {
    /**
     * Init protocol.
     *
     * @param maxNum max number.
     * @throws MpcAbortException if the protocol is abort.
     */
    void init(int maxNum) throws MpcAbortException;

    /**
     * Compute edit distances.
     *
     * @param input input strings.
     * @throws MpcAbortException if the protocol is abort.
     */
    void editDist(String[] input) throws MpcAbortException;
}
