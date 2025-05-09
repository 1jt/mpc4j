package edu.alibaba.main;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.rpc.main.MainPtoConfigUtils;
import org.apache.commons.lang3.time.StopWatch;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public abstract class AbstractMainMultiPartyPto implements MainMultiPartyPto{
    /**
     * stop watch
     */
    protected final StopWatch stopWatch;
    /**
     * own RPC
     */
    protected final Rpc ownRpc;
    /**
     * append string
     */
    protected final String appendString;
    /**
     * save file path
     */
    protected final String filePathString;

    public AbstractMainMultiPartyPto(Properties properties, String ownName) {
        stopWatch = new StopWatch();
        // read append string
        appendString = MainPtoConfigUtils.readAppendString(properties);
        // read save file path
        filePathString = MainPtoConfigUtils.readFileFolderName(properties);
        File inputFolder = new File(filePathString);
        if (!inputFolder.exists()) {
            boolean success = inputFolder.mkdir();
            assert success;
        }
        // read RPC
        ownRpc = RpcPropertiesUtils.readNettyRpcWithOwnName(properties, ownName, "data_owner", "server", "data_user");
    }

    @Override
    public void runNetty() throws IOException, MpcAbortException {
        if (ownRpc.ownParty().getPartyId() == 0) {
            runParty1(ownRpc, ownRpc.getParty(1), ownRpc.getParty(2));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runParty2(ownRpc, ownRpc.getParty(0), ownRpc.getParty(2));
        } else if (ownRpc.ownParty().getPartyId() == 2) {
            runParty3(ownRpc, ownRpc.getParty(0), ownRpc.getParty(1));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }
}
