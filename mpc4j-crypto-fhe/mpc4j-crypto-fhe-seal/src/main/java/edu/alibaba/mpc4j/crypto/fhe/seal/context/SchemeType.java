package edu.alibaba.mpc4j.crypto.fhe.seal.context;

/**
 * Describes the type of encryption scheme to be used.
 * <p>
 * The implementation is from
 * <a href="https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/encryptionparams.h#L25">
 * scheme_type in encryptionparams.h
 * </a>.
 *
 * @author Anony_Trent, Weiran Liu
 * @date 2023/8/30
 */
public enum SchemeType {
    /**
     * No scheme set; cannot be used for encryption
     */
    NONE(0),
    /**
     * Brakerski/Fan-Vercauteren scheme
     */
    BFV(1),
    /**
     * Cheon-Kim-Kim-Song scheme
     */
    CKKS(2),
    /**
     * Brakerski-Gentry-Vaikuntanathan scheme
     */
    BGV(3);

    /**
     * the index of the SchemeType
     */
    private final int value;

    /**
     * Creates a SchemeType.
     *
     * @param value the index of the SchemeType.
     */
    SchemeType(int value) {
        this.value = value;
    }

    /**
     * Gets the index of the SchemeType.
     *
     * @return the index of the SchemeType.
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets SchemeType by the index.
     *
     * @param value the index of the SchemeType.
     * @return the corresponding SchemeType.
     */
    public static SchemeType getByValue(int value) {
        return switch (value) {
            case 0 -> NONE;
            case 1 -> BFV;
            case 2 -> CKKS;
            case 3 -> BGV;
            default -> throw new IllegalArgumentException("no match scheme for given value");
        };
    }
}