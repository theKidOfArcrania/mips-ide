package com.theKidOfArcrania.mips.parsing.directive;

import com.theKidOfArcrania.mips.parsing.CodeSymbols;
import com.theKidOfArcrania.mips.parsing.ErrorLogger;

import static com.theKidOfArcrania.mips.Constants.SEG_DATA;
import static com.theKidOfArcrania.mips.parsing.BasicParamType.STRING;
import static com.theKidOfArcrania.mips.parsing.directive.DirTypes.addDirective;
import static java.util.Arrays.copyOf;

/**
 * This represents all the string data directive types. They encompass the two directives, with the following syntax:
 * <pre>
 *   .ascii str
 *   .asciiz str
 * </pre>
 *
 * Where the <code>asciiz</code> form appends a null-terminating character to the string value.
 * @author Henry Wang
 */
public class StringDataDirType extends DirType {

    /**
     * Initializes all the directives
     */
    static void init() {
        addDirective("ascii", new StringDataDirType(false));
        addDirective("asciiz", new StringDataDirType(true));
    }

    private final boolean nullTerminate;

    /**
     * Constructs a string-data directive-type
     * @param nullTerminate true to null-terminate the string, false to ignore null-terminator
     */
    private StringDataDirType(boolean nullTerminate) {
        super(SEG_DATA, false, STRING);
        this.nullTerminate = nullTerminate;
    }

    @Override
    public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
        if (!super.resolveSymbols(logger, dir, symbols)) {
            return false;
        }

        byte[] str = dir.getArgValue(0, String.class).getBytes();
        symbols.pushToSegment(dir, str.length + (nullTerminate ? 1 : 0), 0);
        return true;
    }

    @Override
    public byte[] write(DirStatement dir) {
        byte[] str = dir.getArgValue(0, String.class).getBytes();
        return nullTerminate ? copyOf(str, str.length) : str;
    }
}
