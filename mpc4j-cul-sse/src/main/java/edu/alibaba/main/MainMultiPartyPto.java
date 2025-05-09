package edu.alibaba.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

public interface MainMultiPartyPto {
    /**
     * Runs Netty.
     *
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runNetty() throws IOException, MpcAbortException;

    /**
     * Runs the first party.
     *
     * @param party1Rpc RPC for Party 1.
     * @param otherParties   otherParties
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runParty1(Rpc party1Rpc, Party... otherParties) throws IOException, MpcAbortException;

    /**
     * Runs the second party.
     *
     * @param party1Rpc RPC for Party 2.
     * @param otherParties   otherParties
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runParty2(Rpc party1Rpc, Party... otherParties) throws IOException, MpcAbortException;

    /**
     * Runs the third party.
     *
     * @param party1Rpc RPC for Party 3.
     * @param otherParties   otherParties
     * @throws IOException       for IOException.
     * @throws MpcAbortException for MPC Abort Exception.
     */
    void runParty3(Rpc party1Rpc, Party... otherParties) throws IOException, MpcAbortException;
}
