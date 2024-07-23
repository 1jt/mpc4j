package edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.sp.ssp;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.galoisfield.sgf2k.Sgf2k;
import edu.alibaba.mpc4j.s2pc.pcg.vole.gf2k.Gf2kVoleReceiverOutput;

/**
 * Single single-point GF2K-VOLE receiver thread.
 *
 * @author Weiran Liu
 * @date 2023/3/16
 */
class Gf2kSspVoleReceiverThread extends Thread {
    /**
     * the receiver
     */
    private final Gf2kSspVoleReceiver receiver;
    /**
     * field
     */
    private final Sgf2k field;
    /**
     * Δ
     */
    private final byte[] delta;
    /**
     * num
     */
    private final int num;
    /**
     * pre-computed receiver output
     */
    private final Gf2kVoleReceiverOutput preReceiverOutput;
    /**
     * receiver output
     */
    private Gf2kSspVoleReceiverOutput receiverOutput;

    Gf2kSspVoleReceiverThread(Gf2kSspVoleReceiver receiver, Sgf2k field, byte[] delta, int num) {
        this(receiver, field, delta, num, null);
    }

    Gf2kSspVoleReceiverThread(Gf2kSspVoleReceiver receiver, Sgf2k field, byte[] delta, int num,
                              Gf2kVoleReceiverOutput preReceiverOutput) {
        this.receiver = receiver;
        this.field = field;
        this.delta = delta;
        this.num = num;
        this.preReceiverOutput = preReceiverOutput;
    }

    Gf2kSspVoleReceiverOutput getReceiverOutput() {
        return receiverOutput;
    }

    @Override
    public void run() {
        try {
            receiver.init(field.getSubfieldL(), delta);
            receiverOutput = receiver.receive(num, preReceiverOutput);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
