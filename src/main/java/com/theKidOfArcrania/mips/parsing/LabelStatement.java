package com.theKidOfArcrania.mips.parsing;


/**
 * Represents a label statement.
 *
 * @author Henry Wang
 */
public class LabelStatement extends CodeStatement {
    /**
     * Reads in an label statement and parses it. The associated token-reader should be primed to the first token
     * (the label word) of that line.
     *
     * @param reader the token reader.
     * @return the parsed label.
     * @throws IllegalStateException if this parse statement is called when the reader is not primed correctly to a
     *                               label statement.
     */
    public static LabelStatement parseStatement(CodeTokenReader reader) {
        if (reader.hasTokenError() || reader.getTokenType() != TokenType.LABEL) {
            throw new IllegalStateException();
        }

        LabelStatement smt = new LabelStatement(reader, (String) reader.getTokenValue(), reader.getTokenPos());
        if (reader.nextToken(false)) {
            Position start = reader.getTokenPos().getStart();
            reader.error("Expected end of statement.", Range.tokenRange(start.getLineNumber(),
                    start.getColumnNumber(), reader.getLine().length()));
            return null;
        }

        return smt;
    }

    private final CodeTokenReader reader;
    private final String name;
    private final Range tokenPos;

    /**
     * Constructs a new label from the name and the associated token reader.
     *
     * @param reader   the token reader.
     * @param name     the name of the label.
     * @param tokenPos the token position of label.
     */
    private LabelStatement(CodeTokenReader reader, String name, Range tokenPos) {
        this.reader = reader;
        this.name = name;
        this.tokenPos = tokenPos;
    }

    @Override
    public boolean resolveSymbols(CodeSymbols symbols) {
        if (!symbols.addLabel(name)) {
            reader.error("Label '" + name + "' already used.", tokenPos);
            return false;
        }
        symbols.addLabel(name);
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
