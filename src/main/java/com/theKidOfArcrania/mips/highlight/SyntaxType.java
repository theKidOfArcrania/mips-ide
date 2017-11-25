package com.theKidOfArcrania.mips.highlight;

/**
 * Represents all the valid syntax highlight types that can be used.
 *
 * @author Henry Wang
 */
public enum SyntaxType {
    COMMENT, DIRECTIVE, INSTRUCTION, REGISTER, LABEL, PARENTHESIS, COMMA, NUMBER, IDENTIFIER, STRING, PBROKEN, PPAIR;

    @Override
    public String toString() {
        return "syntax-" + super.toString().toLowerCase();
    }
}
