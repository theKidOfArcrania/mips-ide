package com.theKidOfArcrania.mips;

/**
 * @author Henry Wang
 */
public interface Constants {
    int WORD_MASK = 0xFFFF;
    int HEX_RADIX = 16;
    int MAX_SHIFT = 32;
    int REGISTER_COUNT = 32;
    int INST_SIZE = 4;
    int INST_ADDR_BITS = 26;

    int SEG_TEXT = 0;
    int SEG_DATA = 1;
    int SEG_GLOBL = 2;
    int SEG_KTEXT = 3;
    int SEG_KDATA = 4;

    int ADDR_TEXT  = 0x00400000;
    int ADDR_DATA  = 0x10010000;
    int ADDR_GLOBL = 0x10000000;
    int ADDR_KTEXT = 0x40000000;
    int ADDR_KDATA = 0x40010000;
    int ADDR_STACK = 0x7FFFFFFF;

    int ADDR_BLOCK_SIZE = 0x10000;

    int BITS_OPCODE = 6;
    int BITS_REG = 5;
    int BITS_REG_OFF = BITS_OPCODE;
    int BITS_SHAMT = 5;
    int BITS_SHAMT_OFF = BITS_REG_OFF + BITS_REG * 3;
    int BITS_IMM = 16;
    int BITS_IMM_MASK = 0xFFFF;
    int BITS_IMM_OFF = BITS_REG_OFF + BITS_REG * 2;
    int BITS_ADDR = 26;
    int BITS_ADDR_MASK = (1 << BITS_ADDR) - 1;
    int BITS_ADDR_OFF = BITS_OPCODE;
    int BITS_FUNCT = 6;
    int BITS_FUNCT_OFF = BITS_SHAMT_OFF + BITS_SHAMT;
    int JMP_FAR_MASK = 0xf0000000;
}
