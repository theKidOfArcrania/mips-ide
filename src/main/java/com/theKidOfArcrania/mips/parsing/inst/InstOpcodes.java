package com.theKidOfArcrania.mips.parsing.inst;

import java.util.HashMap;

/**
 * Enumeration containing all the valid instruction opcodes.
 *
 * @author Henry Wang
 */
public enum InstOpcodes {


    //**********************
    //* ALU operations
    //**********************

    //Add/subtract
    ADD(InstSpec.SPEC_RRR, 0x20, 2, 0, 1),
    ADDU(InstSpec.SPEC_RRR, 0x21, 2, 0, 1),
    SUB(InstSpec.SPEC_RRR, 0x22, 2, 0, 1),
    SUBU(InstSpec.SPEC_RRR, 0x23, 2, 0, 1),

    ADDI(InstSpec.SPEC_RRI_16, 0x8, 1, 0),
    ADDIU(InstSpec.SPEC_RRI_16, 0x9, 1, 0),

    //Multiplication/Division
    MFHI(InstSpec.SPEC_R, 0x10, 2),
    MTHI(InstSpec.SPEC_R, 0x11, 0),
    MFLO(InstSpec.SPEC_R, 0x12, 2),
    MTLO(InstSpec.SPEC_R, 0x13, 0),
    MULT(InstSpec.SPEC_RR, 0x18, 0, 1),
    MULTU(InstSpec.SPEC_RR, 0x19, 0, 1),
    DIV(InstSpec.SPEC_DIV, 0x1a, 0, 1),
    DIVU(InstSpec.SPEC_RR, 0x1b, 0, 1),

    //Shifts
    SLL(InstSpec.SPEC_RRS, 0x0, 2, 1),
    SRL(InstSpec.SPEC_RRS, 0x2, 2, 1),
    SRA(InstSpec.SPEC_RRS, 0x3, 2, 1),
    SLLV(InstSpec.SPEC_RRR, 0x4, 2, 1, 0),
    SRLV(InstSpec.SPEC_RRR, 0x6, 2, 1, 0),
    SRAV(InstSpec.SPEC_RRR, 0x7, 2, 1, 0),

    //Comparision
    SLT(InstSpec.SPEC_RRR, 0x2a, 2, 1, 0),
    SLTU(InstSpec.SPEC_RRR, 0x2b, 2, 1, 0),

    SLTI(InstSpec.SPEC_RRI_16, 0xa, 0, 1),
    SLTIU(InstSpec.SPEC_RRI_16, 0xb, 0, 1),

    //Logical operations
    AND(InstSpec.SPEC_RRR, 0x24, 2, 1, 0),
    OR(InstSpec.SPEC_RRR, 0x25, 2, 1, 0),
    NOR(InstSpec.SPEC_RRR, 0x27, 2, 1, 0),
    XOR(InstSpec.SPEC_RRR, 0x26, 2, 1, 0),

    ANDI(InstSpec.SPEC_RRI_16, 0xc, 0, 1),
    ORI(InstSpec.SPEC_RRI_16, 0xd, 0, 1),
    XORI(InstSpec.SPEC_RRI_16, 0xe, 0, 1),
    LUI(InstSpec.SPEC_RI_16, 0xf, 1),

    //**********************
    //* Control Flow
    //**********************

    //Conditional Branches
    BEQ(InstSpec.SPEC_RRL, 0x4, 0, 1),
    BNE(InstSpec.SPEC_RRL, 0x5, 0, 1),
    BLEZ(InstSpec.SPEC_RL, 0x6, 0),
    BGTZ(InstSpec.SPEC_RL, 0x7, 0),

    BLTZ(InstSpec.SPEC_RL, 0x0, InstFormat.FORMAT_RI, 0),
    BGEZ(InstSpec.SPEC_RL, 0x1, InstFormat.FORMAT_RI, 0),
    BLTZAL(InstSpec.SPEC_RL, 0x10, InstFormat.FORMAT_RI, 0),
    BGEZAL(InstSpec.SPEC_RL, 0x11, InstFormat.FORMAT_RI, 0),

    //Unconditional jumps
    J(InstSpec.SPEC_L, 0x2),
    JAL(InstSpec.SPEC_L, 0x3),

    //Indirect jumps
    JR(InstSpec.SPEC_R, 0x8, 0),
    JALR(InstSpec.SPEC_JALR, 0x9, 0, 2),

    //**********************
    //* Memory
    //**********************

    //Loads
    LB(InstSpec.SPEC_RM, 0x20, 1, 0),
    LH(InstSpec.SPEC_RM, 0x21, 1, 0),
    LWL(InstSpec.SPEC_RM, 0x22, 1, 0),
    LW(InstSpec.SPEC_RM, 0x23, 1, 0),
    LBU(InstSpec.SPEC_RM, 0x24, 1, 0),
    LHU(InstSpec.SPEC_RM, 0x25, 1, 0),
    LWR(InstSpec.SPEC_RM, 0x26, 1, 0),

    //Stores
    SB(InstSpec.SPEC_RM, 0x28, 1, 0),
    SH(InstSpec.SPEC_RM, 0x29, 1, 0),
    SWL(InstSpec.SPEC_RM, 0x2a, 1, 0),
    SW(InstSpec.SPEC_RM, 0x2b, 1, 0),
    SWR(InstSpec.SPEC_RM, 0x2e, 1, 0),

    //**********************
    //* Operating System
    //**********************
    SYSCALL(InstSpec.SPEC_NO_ARG, 0xc),
    BREAK(InstSpec.SPEC_NO_ARG, 0xd),

    //**********************
    //* Psuedo-instructions
    //**********************
    B(InstSpec.SPEC_L, -1),
    BAL(InstSpec.SPEC_L, -1),
    BEQZ(InstSpec.SPEC_RL, -1),
    BGE(InstSpec.SPEC_RRL, -1),
    BGT(InstSpec.SPEC_RRL, -1),
    BGTU(InstSpec.SPEC_RRL, -1),
    BLE(InstSpec.SPEC_RRL, -1),
    BLT(InstSpec.SPEC_RRL, -1),
    CLEAR(InstSpec.SPEC_R, -1),
    LA(InstSpec.SPEC_RL, -1),
    LI(InstSpec.SPEC_RI_32, -1),
    MOVE(InstSpec.SPEC_RR, -1),
    MUL(InstSpec.SPEC_RRR, -1),
    NOT(InstSpec.SPEC_RR, -1),
    NOP(InstSpec.SPEC_NO_ARG, -1),
    REM(InstSpec.SPEC_RRR, -1);

    private static final HashMap<String, InstOpcodes> nameMappings;

    static {
        nameMappings = new HashMap<>();
        for (InstOpcodes opcode : InstOpcodes.values()) {
            nameMappings.put(opcode.getInstName(), opcode);
        }
    }


    /**
     * Fetches the corresponding opcode from the instruction name.
     *
     * @param instName the instruction name to lookup
     * @return the corresponding opcode, or null if instruction name does not exist.
     */
    public static InstOpcodes fetchOpcode(String instName) {
        return nameMappings.get(instName.toUpperCase());
    }

    private final InstSpec spec;
    private final int opcode;
    private final InstFormat format;
    private final int[] regOrder;


    /**
     * Creates a InstOpcode enum (the inst format will be defaulted to the InstSpec's inst format)
     *
     * @param spec     the instruction spec associated with opcode.
     * @param opcode   the opcode/funct/regimm code of this instruction
     * @param regOrder the array representing the slots that the registers will go into
     */
    InstOpcodes(InstSpec spec, int opcode, int... regOrder) {
        this(spec, opcode, spec.getFormat(), regOrder);
    }

    /**
     * Creates a InstOpcode enum
     *
     * @param spec     the instruction spec associated with opcode.
     * @param opcode   the opcode/funct/regimm code of this instruction
     * @param format   the explicit instruction format for this instruction
     * @param regOrder the array representing the slots that the registers will go into
     */
    InstOpcodes(InstSpec spec, int opcode, InstFormat format, int... regOrder) {
        this.spec = spec;
        this.opcode = opcode;
        this.format = format;
        this.regOrder = regOrder;
    }

    /**
     * Obtains the instruction name (in all lower case)
     *
     * @return the instruction name.
     */
    public String getInstName() {
        return name();
    }

    public InstSpec getInstSpec() {
        return spec;
    }

    public InstFormat getFormat() {
        return format;
    }

    public int getOpcode() {
        return opcode;
    }

    public int[] getRegOrder() {
        return regOrder.clone();
    }
}
