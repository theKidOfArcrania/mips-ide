package com.theKidOfArcrania.mips.parsing;

import com.theKidOfArcrania.mips.Constants;

/**
 * Represents an indirect register addressing mode. This refers to the address of the register's value plus an offset
 * from that.
 *
 * @author Henry Wang
 */
public class RegIndirect {
    private final int offset;
    private final int regInd;

    /**
     * Constructs an register indirect address
     *
     * @param offset the offset from the register address
     * @param regInd the register index to use with indirect
     */
    public RegIndirect(int offset, int regInd) {
        if (regInd < 0 || regInd >= Constants.REGISTER_COUNT) {
            throw new IllegalArgumentException("Register index out of bounds");
        }

        this.offset = offset;
        this.regInd = regInd;
    }

    /**
     * @return the offset element of the indirect addressing
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return the register index of the indirect addressing
     */
    public int getRegInd() {
        return regInd;
    }
}
