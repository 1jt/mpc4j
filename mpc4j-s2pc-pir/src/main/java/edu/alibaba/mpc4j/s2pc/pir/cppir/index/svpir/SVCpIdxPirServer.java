package edu.alibaba.mpc4j.s2pc.pir.cppir.index.svpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.HintCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.simple.SimpleCpIdxPirPtoDesc;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SVCpIdxPirServer extends AbstractCpIdxPirServer implements HintCpIdxPirServer {
    /**
     * matrix A
     */
    private IntMatrix matrixA;
    /**
     * LWE dimension
     */
    private final int dimension;
    /**
     * columns
     */
    private int columns;
    /**
     * database
     */
    private IntMatrix db;

    public SVCpIdxPirServer(Rpc serverRpc, Party clientParty, SVCpIdxPirConfig config) {
        super(SVCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
        dimension = gaussianLweParam.getDimension();
    }

    @Override
    public void init(NaiveDatabase database, int matchBatchNum) throws MpcAbortException {
        setInitInput(database, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // server generates and sends the seed for the random matrix A.
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        matrixA = IntMatrix.createRandom(SVCpIdxPirPtoDesc.N, n, seed);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        sendOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.SERVER_SEND_SEED.ordinal(), seedPayload);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1 , 2, seedTime, "Server generates seed");

        stopWatch.start();
        // server runs D ← parse(DB, ρ), where parse encodes the DB into a matrix D ∈ Z_q^{m×ω}, where ω = w/ρ.
        // here we set ρ = 8, so that ω = byteL
        db = IntMatrix.createZeros(n, byteL);
        for (int i = 0; i < database.rows(); i++) {
            byte[] entry = database.getBytesData(i);
            assert entry.length == byteL;
            for (int j = 0; j < byteL; j++) {
                db.set(i, j, entry[j] & 0xFF);
            }
        }
        // server runs M ← A · D, recall that A ∈ Z_q^{n×m}
        IntMatrix matrixA = IntMatrix.createRandom(SVCpIdxPirPtoDesc.N, n, seed);
        IntMatrix matrixM = matrixA.mul(db);
        stopWatch.stop();
        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException{
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            answer();
        }
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, responseTime, "Server generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    public void answer() throws MpcAbortException{
        List<byte[]> clientQueryPayload = receiveOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 1);
        // parse qu
        IntVector qu = IntVector.create(IntUtils.byteArrayToIntArray(clientQueryPayload.get(0)));
        MpcAbortPreconditions.checkArgument(qu.getNum() == n);
        // generate response
        IntMatrix matrixM = matrixA.mul(db);
        List<byte[]> responsePayload = new java.util.ArrayList<>(IntStream.range(0, SVCpIdxPirPtoDesc.N)
                .mapToObj(i -> IntUtils.intArrayToByteArray(matrixM.getRow(i).getElements()))
                .toList());
        IntVector ans = db.leftMul(qu);
        responsePayload.add(IntUtils.intArrayToByteArray(ans.getElements()));
        sendOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }
}
