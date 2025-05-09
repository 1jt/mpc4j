package edu.alibaba.culsse;

import edu.alibaba.AbstractCulSseServer;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirConfig;
import edu.alibaba.mpc4j.s2pc.pir.cppir.ks.simple.SimpleNaiveCpKsPirServer;
import org.apache.commons.lang3.time.StopWatch;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static edu.alibaba.culsse.SingleCulSseDesc.*;

public class SingleCulSseServer<T> extends AbstractCulSseServer<T> {
    /**
     * max size
     */
    private final int maxSize = 100;


    // keyword pir params
    /**
     * arity
     */
    private int ks_arity;


    // index pir params
    /**
     * database size
     */
    protected int idx_n;
    /**
     * value bit length
     */
    protected int idx_l;
    /**
     * value byte length
     */
    protected int idx_byteL;
    /**
     * columns
     */
    private int idx_columns;
    /**
     * transpose database database
     */
    private IntMatrix[] idx_tdbs;

    public SingleCulSseServer(Rpc serverRpc, SingleCulSseConfig config,Party doParty,Party duParty) {
        super(getInstance(), config, serverRpc, doParty, duParty);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
//        SimpleNaiveCpKsPirConfig simpleNaiveCpKsPirConfig = new SimpleNaiveCpKsPirConfig.Builder(gaussianLweParam).build();
//        simpleNaiveCpKsPirServer = new SimpleNaiveCpKsPirServer<T>(serverRpc, clientParty, simpleNaiveCpKsPirConfig);
//        addSubPto(simpleNaiveCpKsPirServer);
    }


    @Override
    public void init(int keywordNum, int maxBatchNum) throws MpcAbortException {
        setInitInput(keywordNum, maxBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        List<byte[]> serverParams = receiveDataOwnerPayload(PtoStep.DATA_OWNER_SEND_SERVER_PARAMS.ordinal());
        MpcAbortPreconditions.checkArgument(serverParams.size() == 2);
        ks_arity = bytesToInt(serverParams.get(0));
        idx_columns = bytesToInt(serverParams.get(1));

        List<byte[]> databasePayload = receiveDataOwnerPayload(PtoStep.DATA_OWNER_SEND_DATABASE.ordinal());
        idx_tdbs = byteListToIntMatrixArrayWithHeader(databasePayload);
        stopWatch.stop();
        long receiveDatabaseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, receiveDatabaseTime, "Server receives database");

        logPhaseInfo(PtoState.INIT_END);
    }

    public static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * List<byte[]> → IntMatrix[]
     */
    public static IntMatrix[] byteListToIntMatrixArrayWithHeader(List<byte[]> byteList) {
        List<IntMatrix> matrices = new ArrayList<>();
        int index = 0;
        while (index < byteList.size()) {
            // Parse header
            byte[] headerBytes = byteList.get(index++);
            ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes);
            int rows = headerBuffer.getInt();
            int columns = headerBuffer.getInt();

            // Parse matrix rows
            IntVector[] rowVectors = new IntVector[rows];
            for (int i = 0; i < rows; i++) {
                byte[] rowBytes = byteList.get(index++);
                ByteBuffer buffer = ByteBuffer.wrap(rowBytes);
                int[] elements = new int[columns];
                for (int j = 0; j < columns; j++) {
                    elements[j] = buffer.getInt();
                }
                rowVectors[i] = IntVector.create(elements);
            }

            matrices.add(IntMatrix.create(rowVectors));
        }

        return matrices.toArray(new IntMatrix[0]);
    }



    @Override
    public void sse(int batchNum) throws MpcAbortException {
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SimpleNaivePir(batchNum);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Cul_Server responses query");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void SimpleNaivePir(int batchNum) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            SimpleNaiveAnswer();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Ks_Server responses query");

        logPhaseInfo(PtoState.PTO_END);
    }

    private void SimpleNaiveAnswer() throws MpcAbortException {
        for (int i = 0; i < ks_arity; i++) {
            SimplePirAnswer();
        }
    }

    private void SimplePirAnswer() throws MpcAbortException {
        List<byte[]> dataUserQueryPayload = receiveDataUserPayload(PtoStep.DATA_USER_SEND_QUERY.ordinal(), 0);
        MpcAbortPreconditions.checkArgument(dataUserQueryPayload.size() == 1);
        // parse qu
        IntVector qu = IntVector.create(IntUtils.byteArrayToIntArray(dataUserQueryPayload.get(0)));
        MpcAbortPreconditions.checkArgument(qu.getNum() == idx_columns);
        // generate response
        IntStream pIntStream = parallel ? IntStream.range(0, idx_tdbs.length).parallel() : IntStream.range(0, idx_tdbs.length);
        List<byte[]> responsePayload = pIntStream
                .mapToObj(p -> idx_tdbs[p].leftMul(qu))
                .map(ans -> IntUtils.intArrayToByteArray(ans.getElements()))
                .toList();
        sendDataUserPayload(PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload, 0);
    }
}
