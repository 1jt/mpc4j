package edu.alibaba.mpc4j.common.tool.bitmatrix.trans;

import edu.alibaba.mpc4j.common.tool.bitmatrix.trans.TransBitMatrixFactory.TransBitMatrixType;

/**
 * JDK列切分转置布尔矩阵。
 *
 * @author Weiran Liu
 * @date 2021/12/09
 */
class JdkSplitColTransBitMatrix extends AbstractSplitColTransBitMatrix {

    JdkSplitColTransBitMatrix(int rows, int columns) {
        // for android
//        super(TransBitMatrixType.JDK, rows, columns);
        super(TransBitMatrixType.JDK_SIMD, rows, columns);
    }

    @Override
    public TransBitMatrixType getTransBitMatrixType() {
        return TransBitMatrixType.JDK_SPLIT_COL;
    }
}
