package edu.alibaba.mpc4j.common.circuit.zl;

import edu.alibaba.mpc4j.common.structure.vector.Vector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.structure.vector.ZlVector;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * plain Zl vector.
 *
 * @author Weiran Liu
 * @date 2023/5/8
 */
public class PlainZlVector implements MpcZlVector {
    /**
     * Create a plain vector with the assigned value.
     *
     * @param zl     Zl instance.
     * @param values the assigned values.
     * @return a plain vector.
     */
    public static PlainZlVector create(Zl zl, BigInteger[] values) {
        PlainZlVector plainZlVector = new PlainZlVector();
        plainZlVector.zlVector = ZlVector.create(zl, values);
        return plainZlVector;
    }

    /**
     * Creates a plain vector with the assigned vector.
     *
     * @param zlVector the assigned vector.
     * @return a plain vector.
     */
    public static PlainZlVector create(ZlVector zlVector) {
        PlainZlVector plainZlVector = new PlainZlVector();
        plainZlVector.zlVector = zlVector;
        return plainZlVector;
    }

    /**
     * Create a random plain vector.
     *
     * @param zl           Zl instance.
     * @param num          num.
     * @param secureRandom random states.
     * @return a plain vector.
     */
    public static PlainZlVector createRandom(Zl zl, int num, SecureRandom secureRandom) {
        PlainZlVector plainZlVector = new PlainZlVector();
        plainZlVector.zlVector = ZlVector.createRandom(zl, num, secureRandom);
        return plainZlVector;
    }

    /**
     * Create a plain all-one vector.
     *
     * @param zl  Zl instance.
     * @param num num.
     * @return a plain vector.
     */
    public static PlainZlVector createOnes(Zl zl, int num) {
        PlainZlVector plainZlVector = new PlainZlVector();
        plainZlVector.zlVector = ZlVector.createOnes(zl, num);
        return plainZlVector;
    }

    /**
     * Create a plain all-zero vector.
     *
     * @param zl  Zl instance.
     * @param num num.
     * @return a plain vector.
     */
    public static PlainZlVector createZeros(Zl zl, int num) {
        PlainZlVector plainZlVector = new PlainZlVector();
        plainZlVector.zlVector = ZlVector.createZeros(zl, num);
        return plainZlVector;
    }

    /**
     * Create an empty plain vector.
     *
     * @param zl Zl instance.
     * @return a plain vector.
     */
    public static PlainZlVector createEmpty(Zl zl) {
        PlainZlVector plainZlVector = new PlainZlVector();
        plainZlVector.zlVector = ZlVector.createEmpty(zl);
        return plainZlVector;
    }

    /**
     * Zl vector
     */
    private ZlVector zlVector;

    /**
     * private constructor.
     */
    private PlainZlVector() {
        // empty
    }

    @Override
    public ZlVector getZlVector() {
        return zlVector;
    }

    @Override
    public boolean isPlain() {
        return true;
    }

    @Override
    public PlainZlVector copy() {
        PlainZlVector clone = new PlainZlVector();
        clone.zlVector = zlVector.copy();

        return clone;
    }

    @Override
    public int getNum() {
        return zlVector.getNum();
    }

    @Override
    public PlainZlVector split(int splitNum) {
        ZlVector splitVector = zlVector.split(splitNum);
        return PlainZlVector.create(splitVector);
    }

    @Override
    public void reduce(int reduceNum) {
        zlVector.reduce(reduceNum);
    }

    @Override
    public void merge(Vector other) {
        PlainZlVector that = (PlainZlVector) other;
        zlVector.merge(that.getZlVector());
    }
}
