package com.theKidOfArcrania.mips.parsing;


import static com.theKidOfArcrania.mips.Constants.MAX_SHIFT;
import static com.theKidOfArcrania.mips.parsing.Range.characterRange;
import static java.lang.Character.isJavaIdentifierPart;
import static java.lang.Character.isJavaIdentifierStart;

/**
 * Represents the basic parameter types
 *
 * @author Henry
 */
public enum BasicParamType implements ParamType {

    SHAMT("shift (5-bit) amount", TokenType.DECIMAL) {
        @Override
        public boolean checkToken(CodeTokenReader reader) {
            int val = (Integer) reader.getTokenValue();
            if (val < 0 || val >= MAX_SHIFT) {
                reader.error("Shift amount out of bounds (" + 0 + " to " + (MAX_SHIFT - 1) + ")",
                        reader.getTokenPos());
                return false;
            }
            return true;
        }
    },
    BYTE("byte (8-bit) integer", TokenType.DECIMAL) {
        @Override
        public boolean checkToken(CodeTokenReader reader) {
            int val = (Integer) reader.getTokenValue();
            if (val < Byte.MIN_VALUE || val > Byte.MAX_VALUE) {
                reader.error("Integer out of bounds (" + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE + ")",
                        reader.getTokenPos());
                return false;
            }
            return true;
        }
    },
    HWORD("half-word (16-bit) integer", TokenType.DECIMAL) {
        @Override
        public boolean checkToken(CodeTokenReader reader) {
            int val = (Integer) reader.getTokenValue();
            if (val < Short.MIN_VALUE || val > Short.MAX_VALUE) {
                reader.error("Integer out of bounds (" + Short.MIN_VALUE + " to " + Short.MAX_VALUE + ")",
                        reader.getTokenPos());
                return false;
            }
            return true;
        }
    },
    WORD("word (32-bit) integer", null) {
        @Override
        public boolean matches(CodeTokenReader reader) {
            TokenType t = reader.getTokenType();
            return t == TokenType.DECIMAL || t == TokenType.HEXADECIMAL;
        }
    },
    SIZE("unsigned (32-bit) integer", null) {
        @Override
        public boolean matches(CodeTokenReader reader) {
            int val = (Integer) reader.getTokenValue();
            if (val < 0) {
                reader.error("Integer must be a positive value.", reader.getTokenPos());
                return false;
            }
            TokenType t = reader.getTokenType();
            return t == TokenType.DECIMAL || t == TokenType.HEXADECIMAL;
        }
    },
    INDIRECT("register indirect addressing", TokenType.INDIRECT) {
        @Override
        public boolean checkToken(CodeTokenReader reader) {
            RegIndirect mem = (RegIndirect) reader.getTokenValue();
            if (mem.getOffset() < Short.MIN_VALUE || mem.getOffset() > Short.MAX_VALUE) {
                reader.error("Indirect offset out of bounds (" + Short.MIN_VALUE + " to " + Short.MAX_VALUE + ")",
                        reader.getTokenPos());
                return false;
            }
            return true;
        }
    },
    REGISTER("integer register identifier", TokenType.REGISTER),
    LOCATION("address location", TokenType.IDENTIFIER) {
        @Override
        public boolean checkToken(CodeTokenReader reader) {
            String token = (String) reader.getTokenValue();
            for (int i = 0; i < token.length(); i++) {
                if (i == 0 ? !isJavaIdentifierStart(token.charAt(i)) : !isJavaIdentifierPart(token.charAt(i))) {
                    reader.error("Illegal character.", characterRange(reader.getLineNumber(), reader
                            .getTokenStartIndex() + i));
                    return false;
                }
            }
            return true;
        }
    },
    STRING("string literal", TokenType.STRING);

    private final String name;
    private final TokenType reqTokenType;

    /**
     * Constructs a basic param type.
     *
     * @param name         the extended user-friendly name.
     * @param reqTokenType the required token type for parameter.
     */
    BasicParamType(String name, TokenType reqTokenType) {
        this.name = name;
        this.reqTokenType = reqTokenType;
    }


    @Override
    public boolean matches(CodeTokenReader reader) {
        return reader.getTokenType() == reqTokenType;
    }

    @Override
    public String getName() {
        return name;
    }
}
