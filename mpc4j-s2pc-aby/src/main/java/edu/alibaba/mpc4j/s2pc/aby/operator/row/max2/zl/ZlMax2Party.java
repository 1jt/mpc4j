package edu.alibaba.mpc4j.s2pc.aby.operator.row.max2.zl;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

/**
 * Zl Greater Party.
 *
 * @author Li Peng
 * @date 2023/5/22
 */
public interface ZlMax2Party extends TwoPartyPto {
    /**
     * inits the protocol.
     *
     * @param maxL maxL.
     * @param maxNum max num.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxL, int maxNum) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param xi the arithmetic share xi.
     * @param yi the arithmetic share yi.
     * @return the party's output.
     * @throws MpcAbortException the protocol failure aborts.
     */
    SquareZlVector max2(SquareZlVector xi, SquareZlVector yi) throws MpcAbortException;
}
