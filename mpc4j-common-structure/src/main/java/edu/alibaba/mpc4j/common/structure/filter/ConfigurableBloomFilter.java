package edu.alibaba.mpc4j.common.structure.filter;

import edu.alibaba.mpc4j.common.structure.filter.BloomFilterFactory.BloomFilterType;
import edu.alibaba.mpc4j.common.structure.filter.FilterFactory.FilterType;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;


public class ConfigurableBloomFilter<T> extends AbstractBloomFilter<T> {
    /*
     * Table 3: False positive rate under various m/n and k combinations
     *
     * | m/n |    k=1   |    k=2   |    k=3   |    k=4   |    k=5   |    k=6   |    k=7   |    k=8   |
     * |-----|----------|----------|----------|----------|----------|----------|----------|----------|
     * | 2.0 | 0.393    | 0.400    |          |          |          |          |          |          |
     * | 3.0 | 0.283    | 0.237    | 0.253    |          |          |          |          |          |
     * | 4.0 | 0.221    | 0.155    | 0.147    | 0.160    |          |          |          |          |
     * | 5.0 | 0.181    | 0.109    | 0.092    | 0.092    | 0.101    |          |          |          |
     * | 6.0 | 0.154    | 0.0804   | 0.0609   | 0.0561   | 0.0578   | 0.0638   |          |          |
     * | 7.0 | 0.133    | 0.0618   | 0.0423   | 0.0359   | 0.0347   | 0.0364   |          |          |
     * | 8.0 | 0.118    | 0.0489   | 0.0306   | 0.024    | 0.0217   | 0.0216   | 0.0229   |          |
     * | 9.0 | 0.105    | 0.0397   | 0.0228   | 0.0166   | 0.0141   | 0.0133   | 0.0135   | 0.0145   |
     * |10.0 | 0.0952   | 0.0329   | 0.0174   | 0.0118   | 0.00943  | 0.00844  | 0.00819  | 0.00846  |
     * |11.0 | 0.0869   | 0.0276   | 0.0136   | 0.00864  | 0.0065   | 0.00552  | 0.00513  | 0.00509  |
     * |12.0 | 0.0800   | 0.0236   | 0.0108   | 0.00646  | 0.00459  | 0.00371  | 0.00329  | 0.00314  |
     * |13.0 | 0.0740   | 0.0203   | 0.00875  | 0.00492  | 0.00332  | 0.00255  | 0.00217  | 0.00199  |
     * |14.0 | 0.0689   | 0.0177   | 0.00718  | 0.00381  | 0.00244  | 0.00179  | 0.00146  | 0.00129  |
     * |15.0 | 0.0645   | 0.0156   | 0.00596  | 0.00300  | 0.00183  | 0.00128  | 0.00100  | 0.000852 |
     * |16.0 | 0.0606   | 0.0138   | 0.00500  | 0.00239  | 0.00139  | 0.000935 | 0.000702 | 0.000574 |
     * |17.0 | 0.0571   | 0.0123   | 0.00423  | 0.00193  | 0.00107  | 0.000692 | 0.000499 | 0.000394 |
     * |18.0 | 0.0540   | 0.0111   | 0.00362  | 0.00158  | 0.000839 | 0.000519 | 0.000360 | 0.000275 |
     * |19.0 | 0.0513   | 0.00998  | 0.00312  | 0.00130  | 0.000663 | 0.000394 | 0.000264 | 0.000194 |
     * |20.0 | 0.0488   | 0.00906  | 0.00270  | 0.00108  | 0.000533 | 0.000303 | 0.000196 | 0.000140 |
     * |21.0 | 0.0465   | 0.00825  | 0.00236  | 0.000905 | 0.000427 | 0.000236 | 0.000147 | 0.000101 |
     * |22.0 | 0.0444   | 0.00755  | 0.00207  | 0.000764 | 0.000347 | 0.000185 | 0.000112 | 7.46e-05 |
     * |23.0 | 0.0425   | 0.00694  | 0.00183  | 0.000649 | 0.000285 | 0.000147 | 8.56e-05 | 5.55e-05 |
     * |24.0 | 0.0408   | 0.00639  | 0.00162  | 0.000555 | 0.000235 | 0.000135 | 7.63e-05 | 4.17e-05 |
     * |25.0 | 0.0391   | 0.00591  | 0.00145  | 0.000478 | 0.000196 | 9.44e-05 | 5.18e-05 | 3.21e-05 |
     * |26.0 | 0.0377   | 0.00548  | 0.00129  | 0.000413 | 0.000164 | 7.66e-05 | 4.08e-05 | 2.42e-05 |
     * |27.0 | 0.0364   | 0.00510  | 0.00116  | 0.000359 | 0.000138 | 6.26e-05 | 3.24e-05 | 1.87e-05 |
     * |28.0 | 0.0351   | 0.00475  | 0.00105  | 0.000314 | 0.000117 | 5.15e-05 | 2.59e-05 | 1.46e-05 |
     * |29.0 | 0.0339   | 0.00444  | 0.000949 | 0.000276 | 9.96e-05 | 4.26e-05 | 2.09e-05 | 1.14e-05 |
     * |30.0 | 0.0328   | 0.00416  | 0.000862 | 0.000243 | 8.53e-05 | 3.55e-05 | 1.69e-05 | 9.01e-06 |
     * |31.0 | 0.0317   | 0.00390  | 0.000785 | 0.000215 | 7.33e-05 | 2.97e-05 | 1.38e-05 | 7.16e-06 |
     * |32.0 | 0.0308   | 0.00367  | 0.000717 | 0.000191 | 6.33e-05 | 2.50e-05 | 1.13e-05 | 5.73e-06 |
     */

    /*
     * Table 4: False positive rate under various m/n and k combinations
     *
     * | m/n |    k=9   |   k=10   |   k=11   |   k=12   |   k=13   |   k=14   |   k=15   |   k=16   |
     * |-----|----------|----------|----------|----------|----------|----------|----------|----------|
     * | 11  | 0.00531  |          |          |          |          |          |          |          |
     * | 12  | 0.00317  | 0.00334  |          |          |          |          |          |          |
     * | 13  | 0.00194  | 0.00198  | 0.00210  |          |          |          |          |          |
     * | 14  | 0.00121  | 0.00120  | 0.00124  |          |          |          |          |          |
     * | 15  | 0.000775 | 0.000744 | 0.000747 | 0.000778 |          |          |          |          |
     * | 16  | 0.000505 | 0.000470 | 0.000459 | 0.000466 | 0.000488 |          |          |          |
     * | 17  | 0.000335 | 0.000302 | 0.000287 | 0.000284 | 0.000291 |          |          |          |
     * | 18  | 0.000226 | 0.000198 | 0.000183 | 0.000176 | 0.000176 | 0.000182 |          |          |
     * | 19  | 0.000155 | 0.000132 | 0.000118 | 0.000111 | 0.000109 | 0.000110 | 0.000114 |          |
     * | 20  | 0.000108 | 8.89e-05 | 7.77e-05 | 7.12e-05 | 6.79e-05 | 6.71e-05 | 6.84e-05 |          |
     * | 21  | 7.59e-05 | 6.09e-05 | 5.18e-05 | 4.63e-05 | 4.31e-05 | 4.17e-05 | 4.16e-05 | 4.27e-05 |
     * | 22  | 5.42e-05 | 4.23e-05 | 3.50e-05 | 3.05e-05 | 2.78e-05 | 2.63e-05 | 2.57e-05 | 2.59e-05 |
     * | 23  | 3.92e-05 | 2.97e-05 | 2.40e-05 | 2.04e-05 | 1.81e-05 | 1.68e-05 | 1.61e-05 | 1.59e-05 |
     * | 24  | 2.86e-05 | 2.11e-05 | 1.66e-05 | 1.38e-05 | 1.20e-05 | 1.08e-05 | 1.02e-05 | 9.87e-06 |
     * | 25  | 2.11e-05 | 1.52e-05 | 1.16e-05 | 9.42e-06 | 8.01e-06 | 7.10e-06 | 6.54e-06 | 6.22e-06 |
     * | 26  | 1.57e-05 | 1.10e-05 | 8.23e-06 | 6.52e-06 | 5.42e-06 | 4.70e-06 | 4.42e-06 | 3.96e-06 |
     * | 27  | 1.18e-05 | 8.07e-06 | 5.89e-06 | 4.56e-06 | 3.70e-06 | 3.15e-06 | 2.79e-06 | 2.55e-06 |
     * | 28  | 8.96e-06 | 5.97e-06 | 4.25e-06 | 3.22e-06 | 2.56e-06 | 2.13e-06 | 1.85e-06 | 1.66e-06 |
     * | 29  | 6.85e-06 | 4.45e-06 | 3.10e-06 | 2.29e-06 | 1.79e-06 | 1.46e-06 | 1.24e-06 | 1.09e-06 |
     * | 30  | 5.28e-06 | 3.35e-06 | 2.28e-06 | 1.65e-06 | 1.26e-06 | 1.01e-06 | 8.39e-07 | 7.26e-07 |
     * | 31  | 4.10e-06 | 2.54e-06 | 1.69e-06 | 1.26e-06 | 8.93e-07 | 7.00e-07 | 5.73e-07 | 4.87e-07 |
     * | 32  | 3.20e-06 | 1.94e-06 | 1.26e-06 | 8.74e-07 | 6.40e-07 | 4.92e-07 | 3.95e-07 | 3.30e-07 |
     */

    /*
     * Table 5: False positive rate under various m/n and k combinations
     *
     * | m/n |   k=17   |   k=18   |   k=19   |   k=20   |   k=21   |   k=22   |   k=23   |   k=24   |
     * |-----|----------|----------|----------|----------|----------|----------|----------|----------|
     * | 22  | 2.67e-05 |          |          |          |          |          |          |          |
     * | 23  | 1.61e-05 |          |          |          |          |          |          |          |
     * | 24  | 9.84e-06 | 1e-05    |          |          |          |          |          |          |
     * | 25  | 6.08e-06 | 6.11e-06 | 6.27e-06 |          |          |          |          |          |
     * | 26  | 3.81e-06 | 3.76e-06 | 3.80e-06 | 3.92e-06 |          |          |          |          |
     * | 27  | 2.41e-06 | 2.34e-06 | 2.33e-06 | 2.37e-06 |          |          |          |          |
     * | 28  | 1.54e-06 | 1.47e-06 | 1.44e-06 | 1.44e-06 | 1.48e-06 |          |          |          |
     * | 29  | 9.96e-07 | 9.35e-07 | 9.10e-07 | 8.89e-07 | 8.96e-07 | 9.21e-07 |          |          |
     * | 30  | 6.50e-07 | 6e-07    | 5.69e-07 | 5.54e-07 | 5.5e-07  | 5.58e-07 |          |          |
     * | 31  | 4.29e-07 | 3.89e-07 | 3.63e-07 | 3.48e-07 | 3.41e-07 | 3.41e-07 | 3.48e-07 |          |
     * | 32  | 2.85e-07 | 2.55e-07 | 2.34e-07 | 2.21e-07 | 2.13e-07 | 2.10e-07 | 2.12e-07 | 2.17e-07 |
     */


    private static final int HASH_NUM = 13;
    /**
     * type
     */
    private static final FilterType FILTER_TYPE = FilterType.CONFIGURABLE_BLOOM_FILTER;

    /**
     * Gets m for the given n.
     *
     * @param maxSize number of elements.
     * @return m.
     */
    public static int bitSize(int maxSize) {
        MathPreconditions.checkPositive("n", maxSize);
        // m = n / ln(2) * σ, flooring so that m % Byte.SIZE = 0.
        int bitLength = (int) Math.ceil(maxSize * 16.0);
        return CommonUtils.getByteLength(bitLength) * Byte.SIZE;
    }

    /**
     * Creates an empty filter.
     *
     * @param envType environment.
     * @param maxSize max number of inserted elements.
     * @param key     hash key.
     * @return an empty filter.
     */
    public static <X> ConfigurableBloomFilter<X> create(EnvType envType, int maxSize, byte[] key) {
        int m = ConfigurableBloomFilter.bitSize(maxSize);
        byte[] storage = new byte[CommonUtils.getByteLength(m)];
        // all positions are initiated as 0
        Arrays.fill(storage, (byte) 0x00);
        return new ConfigurableBloomFilter<>(envType, maxSize, m, key, 0, storage, 0);
    }

    /**
     * Creates the filter based on {@code List<byte[]>}.
     *
     * @param envType       environment.
     * @param byteArrayList the filter represented by {@code List<byte[]>}.
     * @param <X>           the type.
     * @return the filter.
     */
    static <X> ConfigurableBloomFilter<X> load(EnvType envType, List<byte[]> byteArrayList) {
        MathPreconditions.checkEqual("actual list size", "expect list size", byteArrayList.size(), 3);

        // read type
        int typeOrdinal = IntUtils.byteArrayToInt(byteArrayList.remove(0));
        MathPreconditions.checkEqual("expect filter type", "actual filter type", typeOrdinal, FILTER_TYPE.ordinal());

        // read header
        ByteBuffer headerByteBuffer = ByteBuffer.wrap(byteArrayList.remove(0));
        // max size
        int maxSize = headerByteBuffer.getInt();
        int m = ConfigurableBloomFilter.bitSize(maxSize);
        // size
        int size = headerByteBuffer.getInt();
        // item byte length
        int itemByteLength = headerByteBuffer.getInt();
        // key
        byte[] key = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        headerByteBuffer.get(key);

        // read storage
        byte[] storage = byteArrayList.remove(0);

        return new ConfigurableBloomFilter<>(envType, maxSize, m, key, size, storage, itemByteLength);
    }

    ConfigurableBloomFilter(EnvType envType, int maxSize, int m, byte[] key, int size, byte[] storage, int itemByteLength) {
        super(FILTER_TYPE, envType, maxSize, m, HASH_NUM, key, size, storage, itemByteLength);
    }

    @Override
    public BloomFilterType getBloomFilterType() {
        return BloomFilterType.NAIVE_RANDOM_BLOOM_FILTER;
    }

    @Override
    public int[] hashIndexes(T data) {
        byte[] dataBytes = ObjectUtils.objectToByteArray(data);
        byte[] hashes = hash.getBytes(dataBytes);
        return Arrays.stream(IntUtils.byteArrayToIntArray(hashes))
            .map(hi -> Math.abs(hi % m))
            .distinct()
            .toArray();
    }
}
