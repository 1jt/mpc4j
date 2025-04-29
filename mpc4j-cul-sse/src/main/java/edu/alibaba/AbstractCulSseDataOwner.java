package edu.alibaba;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * abstract client-specific preprocessing index PIR server.
 *
 * @author Weiran Liu
 * @date 2023/8/25
 */
public abstract class AbstractCulSseDataOwner<T> extends AbstractMultiPartyPto implements CulSseDataOwner<T> {
    /**
     * Constructs a cul sse protocol.
     *
     * @param ptoDesc    protocol description.
     * @param ownRpc     own RPC.
     * @param otherParties other parties.
     * @param config     config.
     */
    protected AbstractCulSseDataOwner(PtoDesc ptoDesc, MultiPartyPtoConfig config, Rpc ownRpc, Party... otherParties) {
        super(ptoDesc, config, ownRpc, otherParties);
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
     * Sends payload to the data user.
     *
     * @param stepId  step ID.
     * @param payload payload.
     * @param dataUser data user.
     */
    protected void sendDataUserPayload(int stepId, List<byte[]> payload, Party dataUser) {
        sendPayload(stepId, dataUser, payload);
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
     * Receives payload from the data user.
     *
     * @param stepId  step ID.
     * @param dataUser data user.
     * @return payload.
     */
    protected List<byte[]> receiveDataUserPayload(int stepId, Party dataUser) {
        return receivePayload(stepId, dataUser);
    }

    /**
     * keyword num
     */
    protected int keywordNum;
    /**
     * mat batch num
     */
    private int maxBatchNum;

    protected void setInitInput(Map<T, byte[]> keyValueMap, int maxBatchNum) {
        MathPreconditions.checkPositive("keywordNum", keyValueMap.size());
        this.keywordNum = keyValueMap.size();
        // TODO: bot?
        keyValueMap.forEach((keyword, value)->{
            MathPreconditions.checkPositive("values", value.length);
        });

        MathPreconditions.checkPositive("max_batch_num", maxBatchNum);
        this.maxBatchNum = maxBatchNum;
        initState();
    }
//
//    protected void setPtoInput(int batchNum) {
//        checkInitialized();
//        MathPreconditions.checkPositiveInRangeClosed("batch_num", batchNum, maxBatchNum);
//        this.maxBatchNum = batchNum;
//    }
}
