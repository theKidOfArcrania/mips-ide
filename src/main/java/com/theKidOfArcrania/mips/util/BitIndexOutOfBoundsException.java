package com.theKidOfArcrania.mips.util;

/**
 * Represents any errors if we access a bit value out of bounds for BitPacker.
 * @see BitPacker
 * @author Henry Wang
 */
public class BitIndexOutOfBoundsException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = 1247633307936771267L;

    /**
     * Constructs a default exception
     */
    public BitIndexOutOfBoundsException() {
    }

    /**
     * Constructs an exception with a detailed message
     * @param s the detailed message
     */
    public BitIndexOutOfBoundsException(String s) {
        super(s);
    }
}
