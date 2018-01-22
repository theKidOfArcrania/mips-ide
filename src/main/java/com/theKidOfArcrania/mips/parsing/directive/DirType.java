package com.theKidOfArcrania.mips.parsing.directive;

import com.theKidOfArcrania.mips.parsing.*;

import java.util.ArrayList;
import java.util.Arrays;

import static com.theKidOfArcrania.mips.Constants.SEG_DATA;

/**
 * This is the abstract base class encompassing all different directive types. To effectively implement the diversity
 * and plethora of directive types, we will subclass this base abstract class and load them all into
 * the {@link DirTypes} class.
 *
 * @author Henry Wang
 * @see DirTypes
 */
public abstract class DirType {
    private static final String[] SEGMENT_TYPES = {".text", ".data", ".globl", ".ktext", ".kdata"};

    private final int expectedSegment;
    private final boolean paramArray;
    private final ParamType[] params;

    /**
     * Creates a new directive type with the associated opcode.
     *
     * @param expectedSegment the expected section that this directive type should be in
     * @param paramArray      true if multiple of the last parameter value can be passed.
     * @param params          the parameter types with this directive
     */
    DirType(int expectedSegment, boolean paramArray, ParamType... params) {
        this.expectedSegment = expectedSegment;
        this.paramArray = paramArray;
        this.params = params;
    }

    public boolean isParamArray() {
        return paramArray;
    }

    /**
     * Parses all the arguments of this directive type.
     *
     * @param reader the token reader to parse from.
     * @return a list of parsed arguments
     */
    public Argument[] parseArgs(CodeTokenReader reader) {
        Argument[] req = reader.parseArguments(params);
        if (!paramArray) {
            return req;
        }

        boolean error = req == null;
        ParamType last = params[params.length - 1];
        ArrayList<Argument> extra = new ArrayList<>();
        while (reader.nextArgument()) {
            if (!last.matches(reader)) {
                reader.errorExpected(last.getName());
                error = true;
            } else if (last.checkToken(reader)) {
                extra.add(new Argument(reader, last));
            } else {
                error = true;
            }
        }

        if (error) {
            return null;
        } else {
            Argument[] args = Arrays.copyOf(req, req.length + extra.size());
            System.arraycopy(extra.toArray(new Argument[0]), 0, args, req.length, extra.size());
            return args;
        }
    }

    /**
     * Resolves the symbols by executing this directive.
     *
     * @param logger  the logger used to log any errors emitted.
     * @param dir     the directive that has been parsed.
     * @param symbols the list of symbols not fully yet resolved.
     * @return true if no errors occurred, false if some errors occurred.
     */
    public boolean resolveSymbols(ErrorLogger logger, DirStatement dir, CodeSymbols symbols) {
        if (!checkSegment(symbols.getCurrentSegment())) {
            logger.logError("This directive should be located in " +
                    SEGMENT_TYPES[expectedSegment] + " segment.", dir.getLineRange());
            return false;
        }
        return true;
    }

    /**
     * Verifies that the symbols needed are resolved. This is called after the entire code body is parsed, and when
     * external changes are made. By default, this does nothing.
     *
     * @param logger   the logger used to log any errors emitted.
     * @param dir      the directive that has been parsed.
     * @param resolved the list of resolved symbols.
     * @return true if no errors occurred, false if some errors occurred.
     */
    public boolean verifySymbols(ErrorLogger logger, DirStatement dir, CodeSymbols resolved) {
        return true;
    }

    /**
     * Turns a directive into it's data bytes. This will NEVER return a null array
     *
     * @param dir the directive statement to write.
     * @return the binary form of this directive. This will return an empty array by default
     */
    public byte[] write(DirStatement dir) {
        return new byte[0];
    }

    /**
     * Checks whether the current segment matches our expected segment type
     *
     * @param curSegment the current segment
     * @return true if check is successful, false if check failed
     */
    private boolean checkSegment(int curSegment) {
        return expectedSegment == -1 || expectedSegment == curSegment ||
                curSegment <= SEG_DATA && expectedSegment == curSegment - 3;
    }
}
