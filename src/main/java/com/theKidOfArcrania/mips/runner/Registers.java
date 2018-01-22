package com.theKidOfArcrania.mips.runner;

/**
 * Holds all the special register indexes
 *
 * @author Henry Wang
 */
public interface Registers {
    int REG_ZERO = 0;
    int REG_AT = 1;
    int REG_V0 = 2;
    int REG_A0 = 4;
    int REG_A1 = 5;
    int REG_A2 = 6;
    int REG_K0 = 26;
    int REG_K1 = 27;
    int REG_GP = 28;
    int REG_SP = 29;
    int REG_FP = 30;
    int REG_RA = 31;
}
