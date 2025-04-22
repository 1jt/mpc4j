package edu.alibaba.mpc4j.s2pc.pir.cppir.index.svpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.structure.database.NaiveDatabase;
import edu.alibaba.mpc4j.common.structure.database.ZlDatabase;
import edu.alibaba.mpc4j.common.structure.matrix.IntMatrix;
import edu.alibaba.mpc4j.common.structure.vector.IntVector;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.GaussianLweParam;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.HintCpIdxPirServer;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.PianoCpIdxPirUtils;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.piano.hint.PianoHint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SVCpIdxPirServer extends AbstractCpIdxPirServer implements HintCpIdxPirServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SVCpIdxPirServer.class);
    /**
     * matrix A
     */
    private IntMatrix matrixA;
    /**
     * database
     */
    private IntMatrix db;
    /**
     * chunk size
     */
    private int chunkSize;
    /**
     * chunk num
     */
    private int chunkNum;
    /**
     * padding database
     */
    private ZlDatabase paddingDatabase;
    /**
     * query num for each preprocessing round
     */
    private int roundQueryNum;
    /**
     * current query num
     */
    private int currentQueryNum;

    public SVCpIdxPirServer(Rpc serverRpc, Party clientParty, SVCpIdxPirConfig config) {
        super(SVCpIdxPirPtoDesc.getInstance(), serverRpc, clientParty, config);
//        GaussianLweParam gaussianLweParam = config.getGaussianLweParam();
//        dimension = gaussianLweParam.getDimension();
    }

    @Override
    public void init(NaiveDatabase database, int matchBatchNum) throws MpcAbortException {
        setInitInput(database, matchBatchNum);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        // server generates and sends the seed for the random matrix A.
        byte[] seed = BytesUtils.randomByteArray(CommonConstants.BLOCK_BYTE_LENGTH, secureRandom);
        matrixA = IntMatrix.createRandom(SVCpIdxPirPtoDesc.N, PianoCpIdxPirUtils.getChunkNum(n), seed);
        List<byte[]> seedPayload = Collections.singletonList(seed);
        sendOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.SERVER_SEND_SEED.ordinal(), seedPayload);
        stopWatch.stop();
        long seedTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1 , 2, seedTime, "Server generates seed");

        stopWatch.start();
        chunkSize = PianoCpIdxPirUtils.getChunkSize(n);
        chunkNum = PianoCpIdxPirUtils.getChunkNum(n);
        assert chunkSize * chunkNum >= n
                : "chunkSize * chunkNum must be greater than or equal to n (" + n + "): " + chunkSize * chunkNum;
        // pad the database
        byte[][] paddingData = new byte[chunkSize * chunkNum][byteL];
        for (int x = 0; x < n; x++) {
            paddingData[x] = database.getBytesData(x);
        }
        for (int x = n; x < chunkSize * chunkNum; x++) {
            paddingData[x] = BytesUtils.randomByteArray(byteL, l, secureRandom);
        }
        paddingDatabase = ZlDatabase.create(l, paddingData);
        roundQueryNum = PianoCpIdxPirUtils.getRoundQueryNum(n);
        stopWatch.stop();
        long paddingTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
                PtoState.INIT_STEP, 1, 2, paddingTime,
                String.format(
                        "Server sets params: n = %d, ChunkSize = %d, ChunkNum = %d, n (pad) = %d, Q = %d",
                        n, chunkSize, chunkNum, chunkSize * chunkNum, roundQueryNum
                )
        );

        // preprocessing
        preprocessing();

//        stopWatch.start();
//        // server runs D ← parse(DB, ρ), where parse encodes the DB into a matrix D ∈ Z_q^{m×ω}, where ω = w/ρ.
//        // here we set ρ = 8, so that ω = byteL
//        db = IntMatrix.createZeros(n, byteL);
//        for (int i = 0; i < database.rows(); i++) {
//            byte[] entry = database.getBytesData(i);
//            assert entry.length == byteL;
//            for (int j = 0; j < byteL; j++) {
//                db.set(i, j, entry[j] & 0xFF);
//            }
//        }
//        // server runs M ← A · D, recall that A ∈ Z_q^{n×m}
//        IntMatrix matrixA = IntMatrix.createRandom(SVCpIdxPirPtoDesc.N, n, seed);
//        IntMatrix matrixM = matrixA.mul(db);
//        stopWatch.stop();
//        long hintTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(PtoState.INIT_STEP, 2, 2, hintTime, "Server generates hints");

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // stream sending the database
        for (int blockChunkId = 0; blockChunkId < chunkNum; blockChunkId += PianoHint.PRP_BLOCK_OFFSET_NUM) {
            LOGGER.info("preprocessing {} / {}", blockChunkId + 1, chunkNum);
            // send batched chunks
            for (int chunkId = blockChunkId; chunkId < blockChunkId + PianoHint.PRP_BLOCK_OFFSET_NUM && chunkId < chunkNum; chunkId++) {
                // concatenate database into the whole byte buffer
                ByteBuffer byteBuffer = ByteBuffer.allocate(byteL * chunkSize);
                for (int offset = 0; offset < chunkSize; offset++) {
                    byteBuffer.put(paddingDatabase.getBytesData(chunkId * chunkSize + offset));
                }
                List<byte[]> streamRequestPayload = Collections.singletonList(byteBuffer.array());
                sendOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.SERVER_SEND_STREAM_DATABASE_REQUEST.ordinal(), streamRequestPayload);
            }
            // receive response
            List<byte[]> streamResponsePayload = receiveOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.CLIENT_SEND_STREAM_DATABASE_RESPONSE.ordinal());
            MpcAbortPreconditions.checkArgument(streamResponsePayload.isEmpty());
        }
        // reset current query num
        currentQueryNum = 0;
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, streamTime, "Server handles " + chunkNum + " chunk");
    }

    @Override
    public void pir(int batchNum) throws MpcAbortException{
        setPtoInput(batchNum);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        for (int i = 0; i < batchNum; i++) {
            LOGGER.info("query {} / {}", i + 1, batchNum);
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
        List<byte[]> queryRequestPayload = receiveOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.CLIENT_SEND_QUERY_S.ordinal());
        int queryRequestSize = queryRequestPayload.size();
        MpcAbortPreconditions.checkArgument(queryRequestSize == 0 || queryRequestSize == 1);
        if (queryRequestSize == 0) {
            sendOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), new LinkedList<>());
        } else {
            // generate db
            generateDb(queryRequestPayload);
        }

        List<byte[]> clientQueryPayload = receiveOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.CLIENT_SEND_QUERY_V.ordinal());
        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == 1);
        // parse qu
        IntVector qu = IntVector.create(IntUtils.byteArrayToIntArray(clientQueryPayload.get(0)));
        MpcAbortPreconditions.checkArgument(qu.getNum() == chunkNum);

        // generate response
        IntMatrix matrixM = matrixA.mul(db);
        List<byte[]> responsePayload = new java.util.ArrayList<>(IntStream.range(0, SVCpIdxPirPtoDesc.N)
                .mapToObj(i -> IntUtils.intArrayToByteArray(matrixM.getRow(i).getElements()))
                .toList());
        IntVector ans = db.leftMul(qu);
        responsePayload.add(IntUtils.intArrayToByteArray(ans.getElements()));
        sendOtherPartyPayload(SVCpIdxPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), responsePayload);
    }

    private void generateDb(List<byte[]> queryRequestPayload) throws MpcAbortException {
        byte[] queryByteArray = queryRequestPayload.get(0);
        MpcAbortPreconditions.checkArgument(queryByteArray.length == Short.BYTES * (chunkNum - 1));
        ByteBuffer queryByteBuffer = ByteBuffer.wrap(queryByteArray);
        int[] puncturedOffsets = new int[chunkNum - 1];
        for (int i = 0; i < chunkNum - 1; i++) {
            puncturedOffsets[i] = queryByteBuffer.getShort();
        }
        // Start to run PossibleParities
        // build the first guess assuming the punctured position is 0
        byte[] parity = new byte[byteL];
        for (int i = 0; i < chunkNum - 1; i++) {
            int chunkId = i + 1;
            int x = chunkId * chunkSize + puncturedOffsets[i];
            byte[] entry = paddingDatabase.getBytesData(x);
            BytesUtils.xori(parity, entry);
        }
        // init all guesses
        byte[][] guesses = new byte[chunkNum][byteL];
        // set the first guess
        guesses[0] = BytesUtils.clone(parity);
        // now build the rest of the guesses
        for (int misChunkId = 1; misChunkId < chunkNum; misChunkId++) {
            // The hole originally is in the (i-1)-th chunk. Now the hole should be in the i-th chunk.
            int offset = puncturedOffsets[misChunkId - 1];
            int oldX = misChunkId * chunkSize + offset;
            int newX = (misChunkId - 1) * chunkSize + offset;
            byte[] entryOld = paddingDatabase.getBytesData(oldX);
            byte[] entryNew = paddingDatabase.getBytesData(newX);
            BytesUtils.xori(parity, entryOld);
            BytesUtils.xori(parity, entryNew);
            guesses[misChunkId] = BytesUtils.clone(parity);
        }
        NaiveDatabase database = NaiveDatabase.create(byteL * 8, guesses);
        db = IntMatrix.createZeros(chunkNum, byteL);
        for (int i = 0; i < database.rows(); i++) {
            byte[] entry = database.getBytesData(i);
            assert entry.length == byteL;
            for (int j = 0; j < byteL; j++) {
                db.set(i, j, entry[j] & 0xFF);
            }
        }


//        // respond the query
//        ByteBuffer byteBuffer = ByteBuffer.allocate(chunkNum * byteL);
//        for (int i = 0; i < chunkNum; i++) {
//            byteBuffer.put(guesses[i]);
//        }
//        List<byte[]> queryResponsePayload = Collections.singletonList(byteBuffer.array());
//        sendOtherPartyPayload(PianoCpIdxPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), queryResponsePayload);
//        stopWatch.stop();
//        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
//        stopWatch.reset();
//        logStepInfo(
//                PtoState.PTO_STEP, 1, 1, responseTime,
//                "Server responses " + (currentQueryNum + 1) + "-th actual query"
//        );
//
//        currentQueryNum++;
//        // when query num exceeds the maximum, rerun preprocessing.
//        if (currentQueryNum > roundQueryNum) {
//            preprocessing();
//        }
    }
}
