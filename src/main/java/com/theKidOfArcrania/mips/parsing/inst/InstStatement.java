package com.theKidOfArcrania.mips.parsing.inst;

import com.theKidOfArcrania.mips.parsing.*;
import com.theKidOfArcrania.mips.runner.Registers;

import java.util.ArrayList;

import static com.theKidOfArcrania.mips.Constants.*;


//TODO: support sw, lw addr psuedo-inst
//TODO: support gp (extern) addressing

/**
 * This statement represents a single instruction.
 *
 * @author Henry Wang
 */
public class InstStatement extends ArgumentedStatement {
    private static final Argument ARG_R_ZERO = new Argument(Registers.REG_ZERO, null, BasicParamType.REGISTER);
    private static final Argument ARG_R_AT = new Argument(Registers.REG_AT, null, BasicParamType.REGISTER);
    private static final Argument ARG_R_RA = new Argument(Registers.REG_RA, null, BasicParamType.REGISTER);

    private static final Argument ARG_S_ZERO = new Argument(0, null, BasicParamType.SHAMT);
    private static final Argument ARG_C_ZERO = new Argument(0, null, BasicParamType.HWORD);

    /**
     * Reads in an instruction statement and parses it. The associated token-reader should be primed to the first token
     * (the instruction word) of that line. If an error occurred while parsing this instruction, this will return null.
     *
     * @param reader the token reader.
     * @return the instruction parsed, or null.
     */
    public static InstStatement parseStatement(CodeTokenReader reader) {
        String instName = reader.getToken();
        InstOpcodes opcode = InstOpcodes.fetchOpcode(instName);
        if (opcode == null) {
            reader.error("Invalid instruction name.", reader.getTokenPos());
            return null;
        }

        InstSpec instSpec = opcode.getInstSpec();
        Argument[] args = instSpec.parseInstArgs(reader);
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

        return new InstStatement(reader, opcode, toRealInstruction(reader, opcode, args), args);
    }

    /**
     * Converts possibly psuedo-instructions into its corresponding real instructions
     *
     * @param reader the code token reader
     * @param opcode the instruction opcode
     * @param args   the array of arguments.
     * @return an array of real instruction statements, or null if this is a real instruction
     */
    private static InstStatement[] toRealInstruction(CodeTokenReader reader, InstOpcodes opcode, Argument[] args) {
        ArrayList<InstStatement> real = new ArrayList<>();

        switch (opcode) {
            case MOVE:
                real.add(new InstStatement(reader, InstOpcodes.ADDIU, args[0], args[1], ARG_C_ZERO));
                break;
            case CLEAR:
                real.add(new InstStatement(reader, InstOpcodes.ADDU, args[0], ARG_R_ZERO, ARG_R_ZERO));
                break;
            case LI:
                int addr = (Integer) args[1].getValue();
                real.add(new InstStatement(reader, InstOpcodes.LUI, args[0], new Argument(addr >> Short.SIZE,
                        args[1].getTokenPos(), BasicParamType.HWORD)));
                real.add(new InstStatement(reader, InstOpcodes.ORI, args[0], new Argument(addr & WORD_MASK,
                        args[1].getTokenPos(), BasicParamType.HWORD)));
                break;
            case LA:
                //Add dummy instructions for now, but will be replaced with actual load instructions later on.
                real.add(new InstStatement(reader, InstOpcodes.SLL, ARG_R_ZERO, ARG_R_ZERO, ARG_S_ZERO));
                real.add(new InstStatement(reader, InstOpcodes.SLL, ARG_R_ZERO, ARG_R_ZERO, ARG_S_ZERO));
                break;
            case B:
                real.add(new InstStatement(reader, InstOpcodes.BEQ, ARG_R_ZERO, ARG_R_ZERO, args[0]));
                break;
            case BAL:
                real.add(new InstStatement(reader, InstOpcodes.BGEZAL, ARG_R_ZERO, args[0]));
                break;
            case BGT:
                real.add(new InstStatement(reader, InstOpcodes.SLT, ARG_R_AT, args[1], args[0]));
                real.add(new InstStatement(reader, InstOpcodes.BNE, ARG_R_AT, ARG_R_ZERO, args[2]));
                break;
            case BLT:
                real.add(new InstStatement(reader, InstOpcodes.SLT, ARG_R_AT, args[0], args[1]));
                real.add(new InstStatement(reader, InstOpcodes.BNE, ARG_R_AT, ARG_R_ZERO, args[2]));
                break;
            case BGE:
                real.add(new InstStatement(reader, InstOpcodes.SLT, ARG_R_AT, args[0], args[1]));
                real.add(new InstStatement(reader, InstOpcodes.BEQ, ARG_R_AT, ARG_R_ZERO, args[2]));
                break;
            case BLE:
                real.add(new InstStatement(reader, InstOpcodes.SLT, ARG_R_AT, args[1], args[0]));
                real.add(new InstStatement(reader, InstOpcodes.BEQ, ARG_R_AT, ARG_R_ZERO, args[2]));
                break;
            case BGTU:
                real.add(new InstStatement(reader, InstOpcodes.SLT, ARG_R_AT, args[1], args[0]));
                real.add(new InstStatement(reader, InstOpcodes.BNE, ARG_R_AT, ARG_R_ZERO, args[2]));
                break;
            case BEQZ:
                real.add(new InstStatement(reader, InstOpcodes.BEQ, args[0], ARG_R_ZERO, args[1]));
                break;
            case MUL:
                real.add(new InstStatement(reader, InstOpcodes.MULT, args[1], args[2]));
                real.add(new InstStatement(reader, InstOpcodes.MFLO, args[0]));
                break;
            case DIV:
                if (args.length == 3) {
                    real.add(new InstStatement(reader, InstOpcodes.DIV, args[1], args[2]));
                    real.add(new InstStatement(reader, InstOpcodes.MFLO, args[0]));
                }
                break;
            case REM:
                real.add(new InstStatement(reader, InstOpcodes.DIV, args[1], args[2]));
                real.add(new InstStatement(reader, InstOpcodes.MFHI, args[0]));
                break;
            case JALR:
                if (args.length == 1) {
                    real.add(new InstStatement(reader, InstOpcodes.JALR, args[0], ARG_R_RA));
                }
                break;
            case NOT:
                real.add(new InstStatement(reader, InstOpcodes.NOR, args[0], args[1], ARG_R_ZERO));
                break;
            case NOP:
                real.add(new InstStatement(reader, InstOpcodes.SLL, ARG_R_ZERO, ARG_R_ZERO, ARG_S_ZERO));
                break;
            default:
                return null;
        }

        return real.isEmpty() ? null : real.toArray(new InstStatement[0]);
    }

    private final InstSpec spec;
    private final InstOpcodes opcode;
    private final InstStatement[] real;

    /**
     * Constructs a new real instruction
     *
     * @param reader reader associated with instruction.
     * @param opcode the opcode of this instruction
     * @param args   the list of arguments.
     */
    private InstStatement(CodeTokenReader reader, InstOpcodes opcode, Argument... args) {
        this(reader, opcode, null, args);
    }

    /**
     * Constructs a new (possibly psuedo) instruction
     *
     * @param reader reader associated with instruction.
     * @param opcode the opcode of this instruction
     * @param args   the list of arguments.
     * @param real   the array of instruction(s) corresponding to this psuedo-instruction.
     */
    private InstStatement(CodeTokenReader reader, InstOpcodes opcode, InstStatement[] real, Argument... args) {
        super(reader, args);
        this.spec = opcode.getInstSpec();
        this.opcode = opcode;
        this.real = real;
    }

    public InstStatement[] getRealInstructions() {
        return real;
    }

    public InstSpec getSpec() {
        return spec;
    }

    public InstOpcodes getOpcode() {
        return opcode;
    }

    @Override
    public boolean resolveSymbols(CodeSymbols symbols) {
        if (symbols.getCurrentSegment() != SEG_TEXT) {
            reader.error("Not in .text segment.", getLineRange());
            return false;
        }
        symbols.pushToSegment(this, INST_SIZE, 0);
        return true;
    }

    @Override
    public boolean verifySymbols(CodeSymbols symbols) {
        return getSpec().verifySymbol(reader.getDelegateLogger(), this, symbols);
    }

    @Override
    public byte[] write(CodeSymbols symbols) {
        if (opcode == InstOpcodes.LA) {
            Argument args[] = getArgs();
            int addr = symbols.resolveLabel((String) args[1].getValue());
            real[0] = new InstStatement(reader, InstOpcodes.LUI, args[0], new Argument(addr >> Short.SIZE,
                    args[1].getTokenPos(), BasicParamType.HWORD));
            real[1] = new InstStatement(reader, InstOpcodes.ORI, args[0], new Argument(addr & WORD_MASK,
                    args[1].getTokenPos(), BasicParamType.HWORD));
        }

        if (real != null) {
            byte[] data = new byte[real.length * INST_SIZE];
            int offset = 0;
            for (InstStatement inst : real) {
                System.arraycopy(inst.write(symbols), 0, data, offset, INST_SIZE);
                offset += INST_SIZE;
            }
            return data;
        } else {
            return InstFormat.writeInst(this, symbols);
        }
    }
}
