package com.theKidOfArcrania.mips.parsing;


/**
 * Represents an empty no-op blank line.
 *
 * @author Henry Wang
 */
public class EmptyStatement extends CodeStatement {
    @Override
    public void updateLinePos(int line) {
        //Does nothing
    }

    @Override
    public boolean resolveSymbols(CodeSymbols symbols) {
        return true;
    }

    @Override
    public boolean verifySymbols(CodeSymbols symbols) {
        return true;
    }

    @Override
    public byte[] write(CodeSymbols symbols) {
        return new byte[0];
    }
}
