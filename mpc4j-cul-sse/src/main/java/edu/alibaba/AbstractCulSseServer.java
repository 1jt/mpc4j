package edu.alibaba;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.util.List;

/**
 * abstract client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractCulSseServer<T> extends AbstractMultiPartyPto implements CulSseServer<T> {
    /**
     * Constructs a cul sse protocol.
     *
     * @param ptoDesc    protocol description.
     * @param ownRpc     own RPC.
     * @param otherParties other parties.
     * @param config     config.
     */
    protected AbstractCulSseServer(PtoDesc ptoDesc, MultiPartyPtoConfig config, Rpc ownRpc, Party... otherParties) {
        super(ptoDesc, config, ownRpc, otherParties);
    }

    /**
     * Sends payload to the data owner.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendDataOwnerPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, getDataOwner(), payload);
    }

    /**
     * Sends payload to the data user.
     *
     * @param stepId  step ID.
     * @param payload payload.
     * @param userId data user ID.
     */
    protected void sendDataUserPayload(int stepId, List<byte[]> payload, int userId) {
        sendPayload(stepId, getDataUsers(userId), payload);
    }

    /**
     * Receives payload from the data owner.
     *
     * @param stepId step ID.
     * @return payload.
     */
    protected List<byte[]> receiveDataOwnerPayload(int stepId) {
        return receivePayload(stepId, getDataOwner());
    }

    /**
     * Receives payload from the data user.
     *
     * @param stepId  step ID.
     * @param userId data user ID.
     * @return payload.
     */
    protected List<byte[]> receiveDataUserPayload(int stepId, int userId) {
        return receivePayload(stepId, getDataUsers(userId));
    }

    /**
     * keyword num
     */
    protected int keywordNum;
    /**
     * mat batch num
     */
    private int maxBatchNum;
    /**
     * batch num
     */
    protected int batchNum;


    protected void setInitInput(int keywordNum, int maxBatchNum) {
        MathPreconditions.checkPositive("keywordNum", keywordNum);
        this.keywordNum = keywordNum;
        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }

    protected void setPtoInput(int batchNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", batchNum, maxBatchNum);
        this.batchNum = batchNum;
    }
}
