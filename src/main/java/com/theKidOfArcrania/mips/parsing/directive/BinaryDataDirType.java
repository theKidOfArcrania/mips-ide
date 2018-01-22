package com.theKidOfArcrania.mips.parsing.directive;

import com.theKidOfArcrania.mips.parsing.BasicParamType;
import com.theKidOfArcrania.mips.parsing.CodeSymbols;
import com.theKidOfArcrania.mips.parsing.ErrorLogger;

import static com.theKidOfArcrania.mips.Constants.SEG_DATA;
import static com.theKidOfArcrania.mips.parsing.BasicParamType.*;
import static com.theKidOfArcrania.mips.parsing.directive.DirTypes.addDirective;

/**
 * This directive type represents the set of directives involving binary integer data. This includes the following
 * directives and the following syntaxes:
 * <pre>
 *   .byte b1,...,bn
 *   .half h1,...,hn
 *   .word w1,...,wn
 * </pre>
 * They represent 8-bit, 16-bit, and 32-bit signed integers, respectively.
 *
 * @author Henry Wang
 */
//TODO: test for more space
public class BinaryDataDirType extends DirType {
    private final int size;
    private final int bitSize;

    /**
     * Initializes all the directives
     */
    static void init() {
        addDirective("byte", new BinaryDataDirType(BYTE, Byte.BYTES));
        addDirective("half", new BinaryDataDirType(HWORD, Short.BYTES));
        addDirective("word", new BinaryDataDirType(WORD, Integer.BYTES));
    }

    /**
     * Constructs a binary-data directive-type
     *
     * @param intParam the integer parameter type
     * @param size     the byte size of each number
     */
    private BinaryDataDirType(BasicParamType intParam, int size) {
        super(SEG_DATA, true, intParam);
        this.size = size;

        int count = 0;
        while (size > 0) {
            size >>= 1;
            count++;
        }
        bitSize = count;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This allocates the space necessary for the binary integer data onto the end of the data segment,
     * applying any alignment if specified, and/or specified by our size
     *
     * @return true iff the current segment is the expected .data segment
     */
    @Override
    public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
        if (!super.resolveSymbols(logger, dir, symbols)) {
            return false;
        }

        symbols.pushToSegment(dir, size * dir.getArgSize(), bitSize - 1);
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This writes the numeric binary data to our .data segment in little endian format, returning the binary format
     * of this data. It will only return the numeric data itself, unpadded from alignment.
     */
    @Override
    public byte[] write(DirStatement dir) {
        byte[] data = new byte[size * dir.getArgSize()];
        for (int i = 0; i < data.length; i++) {
            int val = dir.getIntArgValue(i);
            for (int j = 0; j < size; j++) {
                data[i] = (byte) val;
                val >>= Byte.SIZE;
            }
        }
        return data;
    }
}
