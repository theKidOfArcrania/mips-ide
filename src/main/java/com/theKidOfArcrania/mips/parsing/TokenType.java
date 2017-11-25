package com.theKidOfArcrania.mips.parsing;

/**
 * Represents all the possible token types that can be parsed by the {@link CodeTokenReader}.
 *
 * @author Henry Wang
 */
public enum TokenType {
    HEXADECIMAL, DECIMAL, REGISTER, INDIRECT, IDENTIFIER, DIRECTIVE, LABEL, STRING
}
