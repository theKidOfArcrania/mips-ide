package com.theKidOfArcrania.mips.parsing;


/**
 * Represents a parsed line of statement of the token reader. This is a base class for different types of statements,
 * including instruction lines, label lines, directives, and no-op lines.
 *
 * @author Henry Wang
 */
public abstract class CodeStatement {

    /**
     * Updates the current line position of this code statement. This is called whenever a line above this was
     * inserted or deleted, shifting the line position of the next few lines. This may also be called when no line
     * shift has occurred. In that case, this should ignore the update call. Subclasses should implement this method
     * by, for example, updating all {@link Range} and {@link Position} classes to point to the correct line number.
     *
     * @param line the new line number this code statement should be on.
     */
    public abstract void updateLinePos(int line);

    /**
     * Resolves the symbols this statement refers to are resolved. Any errors that are not deemed fatal, i.e. would
     * affect how this symbol is resolved should NOT be thrown now, but rather deferred until the verification
     * stage, since at this stage we would not have fully resolved code symbols yet.
     *
     * @param symbols the reference to the code symbols
     * @return true if resolution is successful, false if errors occurred
     * @see #verifySymbols(CodeSymbols)
     */
    public abstract boolean resolveSymbols(CodeSymbols symbols);

    /**
     * Verifies that all symbols referred on this statement is fully resolved. At this step, any remaining resolution
     * errors should be thrown since all the code symbols have been resolved now.
     *
     * @param symbols the fully resolved code symbols.
     * @return true if verification is successful, false if errors occurred
     */
    public abstract boolean verifySymbols(CodeSymbols symbols);

    /**
     * Writes this code statement into a byte instruction
     *
     * @param symbols the fully resolved symbols to write.
     * @return a byte array representing this instruction
     */
    public abstract byte[] write(CodeSymbols symbols);

}
