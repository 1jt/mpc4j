package edu.alibaba;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * abstract client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractCulSseDataUser<T> extends AbstractMultiPartyPto implements CulSseDataUser<T> {
    /**
     * Constructs a cul sse protocol.
     *
     * @param ptoDesc    protocol description.
     * @param ownRpc     own RPC.
     * @param otherParties other parties.
     * @param config     config.
     */
    protected AbstractCulSseDataUser(PtoDesc ptoDesc, MultiPartyPtoConfig config, Rpc ownRpc, Party... otherParties) {
        super(ptoDesc, config, ownRpc, otherParties);
    }

    /**
     * Sends payload to the data owner.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendDataUserPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, getDataOwner(), payload);
    }

    /**
     * Sends payload to the server.
     *
     * @param stepId  step ID.
     * @param payload payload.
     */
    protected void sendServerPayload(int stepId, List<byte[]> payload) {
        sendPayload(stepId, getServer(), payload);
    }

    /**
     * Receives payload from the data owner.
     *
     * @param stepId  step ID.
     * @return payload.
     */
    protected List<byte[]> receiveDataOwnerPayload(int stepId) {
        return receivePayload(stepId, getDataOwner());
    }

    /**
     * Receives payload from the server.
     *
     * @param stepId step ID.
     * @return payload.
     */
    protected List<byte[]> receiveServerPayload(int stepId) {
        return receivePayload(stepId, getServer());
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

    protected void setPtoInput(ArrayList<T> keys) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("batch_num", keys.size(), maxBatchNum);
        this.batchNum = keys.size();
        for (T keyword : keys) {
            Preconditions.checkArgument(keyword != null, "x must not equal ⊥");
        }
    }
}
