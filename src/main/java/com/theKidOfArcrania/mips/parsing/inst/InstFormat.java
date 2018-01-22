package com.theKidOfArcrania.mips.parsing.inst;

import com.theKidOfArcrania.mips.Constants;
import com.theKidOfArcrania.mips.parsing.BasicParamType;
import com.theKidOfArcrania.mips.parsing.CodeSymbols;
import com.theKidOfArcrania.mips.parsing.RegIndirect;
import com.theKidOfArcrania.mips.util.BitPacker;

/**
 * Represents all the possible instruction formats possible
 *
 * @author Henry Wang
 */
public enum InstFormat implements Constants {
    FORMAT_R(BITS_FUNCT_OFF, BITS_OPCODE),
    FORMAT_RI(BITS_REG_OFF + BITS_REG, BITS_REG),
    FORMAT_I(0, BITS_OPCODE), FORMAT_J(0, BITS_OPCODE);

    /**
     * Converts an instruction statement into its component bytes.
     *
     * @param inst     the instruction statement
     * @param resolved the resolved code symbols
     * @return a byte array encapsulating the instruction
     */
    public static byte[] writeInst(InstStatement inst, CodeSymbols resolved) {
        InstOpcodes opcode = inst.getOpcode();
        InstFormat format = opcode.getFormat();

        BitPacker bits = new BitPacker(INST_SIZE);
        if (format == FORMAT_RI) {
            bits.set(0, BITS_OPCODE, 1);
        }

        bits.set(format.opcodeOffset, format.opcodeLength, opcode.getOpcode());

        int[] regOrder = opcode.getRegOrder();
        int regs = 0;
        for (int i = 0; i < inst.getArgSize(); i++) {
            switch ((BasicParamType) inst.getArgExactType(i)) {
                case SHAMT:
                    bits.set(BITS_SHAMT_OFF, BITS_SHAMT, inst.getIntArgValue(i));
                    break;
                case HWORD:
                    bits.set(BITS_IMM_OFF, BITS_IMM, inst.getIntArgValue(i));
                    break;
                case WORD:
                    throw new IllegalArgumentException("Cannot write a WORD number");
                case INDIRECT:
                    RegIndirect mem = inst.getArgValue(i, RegIndirect.class);
                    bits.set(BITS_REG_OFF + BITS_REG * regOrder[regs++], BITS_REG, mem.getRegInd());
                    bits.set(BITS_IMM_OFF, BITS_IMM, mem.getOffset());
                    break;
                case REGISTER:
                    bits.set(BITS_REG_OFF + BITS_REG * regOrder[regs++], BITS_REG, inst.getIntArgValue(i));
                    break;
                case LOCATION:
                    int addr = resolved.resolveLabel(inst.getArgValue(i, String.class));
                    if (format == FORMAT_J) {
                        addr = (addr >> 2) & BITS_ADDR_MASK;
                        bits.set(BITS_ADDR_OFF, BITS_ADDR, addr);
                    } else {
                        addr = (addr >> 2) - resolved.getStatementAddress(inst);
                        bits.set(BITS_IMM_OFF, BITS_IMM, addr);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported argument type: " + inst.getArgExactType(i));
            }
        }
        return bits.toBytes();
    }

    private final int opcodeOffset;
    private final int opcodeLength;

    /**
     * Constructor for an instruction format
     *
     * @param opcodeOffset opcode offset for format
     * @param opcodeLength opcode length for format
     */
    InstFormat(int opcodeOffset, int opcodeLength) {
        this.opcodeOffset = opcodeOffset;
        this.opcodeLength = opcodeLength;
    }

}
