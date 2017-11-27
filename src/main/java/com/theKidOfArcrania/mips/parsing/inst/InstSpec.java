package com.theKidOfArcrania.mips.parsing.inst;

import com.theKidOfArcrania.mips.parsing.*;

import java.util.Arrays;

import static com.theKidOfArcrania.mips.parsing.BasicParamType.LOCATION;
import static com.theKidOfArcrania.mips.parsing.BasicParamType.REGISTER;
import static com.theKidOfArcrania.mips.parsing.Range.tokenRange;
import static com.theKidOfArcrania.mips.parsing.inst.InstFormat.*;
import static com.theKidOfArcrania.mips.runner.Registers.REG_AT;
import static com.theKidOfArcrania.mips.runner.Registers.REG_K0;
import static com.theKidOfArcrania.mips.runner.Registers.REG_K1;

/**
 * An instruction specification representing a list of arguments and argument types for a particular instruction
 * This can apply to multiple instruction opcodes if all the instructions have a similar instruction specification.
 *
 * @author Henry Wang
 */
public enum InstSpec {
    SPEC_NO_ARG(FORMAT_R),
    SPEC_R(FORMAT_R, BasicParamType.REGISTER),
    SPEC_RR(FORMAT_R, BasicParamType.REGISTER, BasicParamType.REGISTER),
    SPEC_RRR(FORMAT_R, BasicParamType.REGISTER, BasicParamType.REGISTER, BasicParamType.REGISTER),
    SPEC_RI_16(FORMAT_I, BasicParamType.REGISTER, BasicParamType.HWORD),
    SPEC_RI_32(FORMAT_I, BasicParamType.REGISTER, BasicParamType.WORD),
    SPEC_RRS(FORMAT_R, BasicParamType.REGISTER, BasicParamType.REGISTER, BasicParamType.SHAMT),
    SPEC_RRI_16(FORMAT_I, BasicParamType.REGISTER, BasicParamType.REGISTER, BasicParamType.HWORD),
    SPEC_L(FORMAT_J, LOCATION),
    SPEC_RL(FORMAT_I, BasicParamType.REGISTER, LOCATION),
    SPEC_RRL(FORMAT_I, BasicParamType.REGISTER, BasicParamType.REGISTER, LOCATION),
    SPEC_RM(FORMAT_I, BasicParamType.REGISTER, new MultipleParamType(LOCATION, BasicParamType.INDIRECT)),
    //Special specs for div (instruction overload)
    SPEC_DIV(FORMAT_R, BasicParamType.REGISTER, BasicParamType.REGISTER /*[, REGISTER]*/) {
        @Override
        public Argument[] parseInstArgs(CodeTokenReader reader) {
            Argument[] args = super.parseInstArgs(reader);
            return optRegister(reader, args);
        }
    },
    //Special specs for jalr (instruction overload)
    SPEC_JALR(FORMAT_R, BasicParamType.REGISTER /*[, REGISTER]*/) {
        @Override
        public Argument[] parseInstArgs(CodeTokenReader reader) {
            Argument[] args = super.parseInstArgs(reader);
            return optRegister(reader, args);
        }
    };

    /**
     * Queries for an optional register argument and returns the resulting argument array.
     * @param reader the code token reader
     * @param args the current list of arguments.
     * @return null if an error occurred, otherwise, the resulting argument after optional parameter
     */
    private static Argument[] optRegister(CodeTokenReader reader, Argument[] args) {
        if (reader.nextArgument()) {
            if (!BasicParamType.REGISTER.matches(reader)) {
                reader.errorExpected(BasicParamType.REGISTER.getName());
                return null;
            } else if (!BasicParamType.REGISTER.checkToken(reader)) {
                return null;
            }

            args = Arrays.copyOf(args, args.length + 1);
            args[args.length - 1] = new Argument(reader, BasicParamType.REGISTER);
        }
        return args;
    }

    private final InstFormat format;
    private final ParamType[] params;

    /**
     * Constructs a new instruction specification with a specific parameter signature.
     *
     * @param format the default instruction format for a particular spec.
     * @param params the parameter signature for this spec.
     */
    InstSpec(InstFormat format, ParamType... params) {
        this.format = format;
        this.params = params;
    }

    public InstFormat getFormat() {
        return format;
    }

    /**
     * Parses the instruction arguments from the current position
     *
     * @param reader the token reader
     * @return an array of the parsed arguments.
     */
    public Argument[] parseInstArgs(CodeTokenReader reader) {
        return reader.parseArguments(params);
    }

    /**
     * Verifies that the symbols needed are resolved. This is called after the entire code body is parsed, and when
     * external changes are made. This make sure that all string labels are resolved.
     *
     * @param logger   the logger used to log any errors emitted.
     * @param inst     the instruction that has been parsed.
     * @param resolved the list of resolved symbols.
     * @return true if no errors occurred, false if some errors occurred.
     */
    public boolean verifySymbol(ErrorLogger logger, InstStatement inst, CodeSymbols resolved) {
        for (int i = 0; i < inst.getArgSize(); i++) {
            ParamType type = inst.getArgExactType(i);
            Object val = inst.getArgValue(i);
            if (type == LOCATION) {
                int addr = resolved.resolveLabel((String)val);
                if (addr == -1) {
                    logger.logError("Unresolved symbol.", inst.getArg(i).getTokenPos());
                    return false;
                }

                int instAddr = resolved.getStatementAddress(inst);
                if (inst.getOpcode().getFormat() == InstFormat.FORMAT_J) {
                    if ((addr & JMP_FAR_MASK) != (instAddr & JMP_FAR_MASK)) {
                        logger.logError("Imprecise jump. (Jump too far).", inst.getArg(i).getTokenPos());
                        return false;
                    }
                } else if (inst.getOpcode() != InstOpcodes.LA){
                    int offset = (addr - instAddr) >> 2;
                    if (offset != (short) offset) {
                        logger.logError("Branch offset too big.", inst.getArg(i).getTokenPos());
                        return false;
                    }
                }
            } else if (type == REGISTER) {
                int reg = (Integer)val;
                if (reg == REG_AT || reg == REG_K0 || reg == REG_K1) {
                    logger.logWarning("Reserved register", inst.getArg(i).getTokenPos());
                }
            }
        }
        return true;
    }
}
