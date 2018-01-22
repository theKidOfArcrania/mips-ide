package com.theKidOfArcrania.mips.parsing.directive;

import com.theKidOfArcrania.mips.parsing.*;

/**
 * This statement represents a directive. Directives are prepended by a dot (.).
 *
 * @author Henry Wang
 * @see DirType
 * @see DirTypes
 */
public class DirStatement extends ArgumentedStatement {

    /**
     * Parses a directive statement. This take the current line of the code token reader and parses the arguments,
     * returning a parsed directive statement.
     *
     * @param reader the code token reader
     * @return the parsed statement
     */
    public static DirStatement parseStatement(CodeTokenReader reader) {
        String dirName = reader.getToken().substring(1);
        DirType dirType = DirTypes.fetchDirective(dirName);
        if (dirType == null) {
            reader.error("Invalid directive name.", reader.getTokenPos());
            return null;
        }

        Argument[] args = dirType.parseArgs(reader);
        if (args == null) {
            return null;
        }

        int end = reader.getTokenEndIndex();
        if (reader.nextToken(false)) {
            Position start = reader.getTokenPos().getStart();
            reader.error("Expected end of statement.", Range.tokenRange(start.getLineNumber(), end,
                    reader.getLine().length()));
            return null;
        }

        return new DirStatement(reader, dirType, args);
    }

    private final DirType dirType;

    /**
     * Constructs a new directive statement.
     *
     * @param reader  reader associated with directive.
     * @param dirType the directive type that this statement derives from
     * @param args    the list of arguments.
     */
    private DirStatement(CodeTokenReader reader, DirType dirType, Argument[] args) {
        super(reader, args);
        this.dirType = dirType;
    }


    @Override
    public boolean resolveSymbols(CodeSymbols symbols) {
        return dirType.resolveSymbols(reader.getDelegateLogger(), this, symbols);
    }

    @Override
    public boolean verifySymbols(CodeSymbols symbols) {
        return dirType.verifySymbols(reader.getDelegateLogger(), this, symbols);
    }

    @Override
    public byte[] write(CodeSymbols symbols) {
        return dirType.write(this);
    }
}
