package com.theKidOfArcrania.mips.runner;

import com.theKidOfArcrania.mips.Constants;
import com.theKidOfArcrania.mips.runner.ProgramException.ErrorType;
import com.theKidOfArcrania.mips.util.BitPacker;

import java.io.*;
import java.util.Scanner;

import static com.theKidOfArcrania.mips.runner.ProgramException.ErrorType.ARIH;
import static java.lang.Integer.toUnsignedLong;
import static java.lang.Short.toUnsignedInt;

//TODO: global pointer is at 0x10008000
/**
 * This interprets instruction opcodes for a MIPS program, allowing step-by-step running.
 * @author Henry Wang
 */
public class Interpreter implements Constants, Registers {
    private BitPacker current;
    private MemState state;

    private int addrDataTail;
    private int addrTextTail;

    private InputStream in;
    private PrintStream out;

    /**
     * Creates a new interpreter with the default memory segments loaded.
     */
    public Interpreter() {
        in = new BufferedInputStream(System.in);
        out = System.out;

        current = new BitPacker(INST_SIZE);
        state = new MemState();

        addrTextTail = ADDR_TEXT;
        addrDataTail = ADDR_DATA;

        state.allocateSegment(addrDataTail, ADDR_BLOCK_SIZE);
        state.allocateSegment(ADDR_GLOBL, ADDR_BLOCK_SIZE);
        state.allocateSegment(ADDR_STACK - ADDR_BLOCK_SIZE + 1, ADDR_BLOCK_SIZE);

        state.setRegister(REG_GP, ADDR_GLOBL);
        state.setRegister(REG_SP, ADDR_STACK);
    }

    /**
     * Appends a .text chunk to the end of the text segment
     * @param chunk the chunk of data to add
     * @throws ProgramException an error occurred while trying to append the data
     */
    public void appendText(byte[] chunk) throws ProgramException {
        state.allocateSegment(addrTextTail, chunk.length);
        state.set(addrTextTail, chunk);
        addrTextTail += chunk.length;
    }

    /**
     * Appends a .data chunk to the end of the data segment
     * @param chunk the chunk of data to add
     * @throws ProgramException an error occurred while trying to append the data
     */
    public void appendData(byte[] chunk) throws ProgramException {
        state.set(addrDataTail, chunk);
        addrDataTail += chunk.length;
    }

    /**
     * Executes a single instruction, specifically the instruction that PC is pointing to
     * @throws ProgramException if executing this instruction results in an illegal action
     */
    @SuppressWarnings("MagicNumber")
    public void execute() throws ProgramException{
        int pc = state.pc();

        current.setDWORD(0, state.getInt(pc));

        int regS = current.get(BITS_REG_OFF, BITS_REG);
        int regT = current.get(BITS_REG_OFF + BITS_REG, BITS_REG); //Also register immediate
        int regD = current.get(BITS_REG_OFF + BITS_REG * 2, BITS_REG);
        int shamt = current.get(BITS_SHAMT_OFF, BITS_SHAMT);
        int imm = current.get(BITS_IMM_OFF, BITS_IMM);
        int addr = current.get(BITS_ADDR_OFF, BITS_ADDR);

        int advance = 4;
        switch (current.get(0, BITS_OPCODE)) {
            case 0x00: //R-type opcodes
                switch(current.get(BITS_FUNCT_OFF, BITS_FUNCT)) {
                    case 0x00: //sll
                        reg(regD, reg(regT) << shamt);
                        break;
                    case 0x02: //srl
                        reg(regD, reg(regT) >>> shamt);
                        break;
                    case 0x03: //sra
                        reg(regD, reg(regT) >> shamt);
                        break;
                    case 0x04: //sllv
                        reg(regD, reg(regT) << reg(regS));
                        break;
                    case 0x06: //srlv
                        reg(regD, reg(regT) >>> reg(regS));
                        break;
                    case 0x07: //sarv
                        reg(regD, reg(regT) >> reg(regS));
                        break;
                    case 0x08: //jr
                        state.jump(reg(regS));
                        advance = -1;
                        break;
                    case 0x09: //jalr
                        state.jump(reg(regS));
                        advance = -1;
                        reg(regD, pc + INST_SIZE * 2);
                        break;
                    case 0x0c: //syscall
                        syscall();
                        break;
                    case 0x0d: //break
                        throw new ProgramException(ErrorType.BKPT);
                    case 0x10: //mfhi
                        reg(regD, state.getHigh());
                        break;
                    case 0x11: //mthi
                        state.setHigh(reg(regS));
                        break;
                    case 0x12: //mflo
                        reg(regD, state.getLow());
                        break;
                    case 0x13: //mtlo
                        state.setLow(reg(regS));
                        break;
                    case 0x18: //mult
                        long res = (long)reg(regS) * reg(regT);
                        state.setLow((int)res);
                        state.setHigh((int)(res >>> Integer.SIZE));
                        break;
                    case 0x19: //multu
                        res = regU(regS) * regU(regT);
                        state.setLow((int)res);
                        state.setHigh((int)(res >>> Integer.SIZE));
                        break;
                    case 0x1A: //div
                        if (reg(regT) == 0) {
                            throw new ProgramException(ARIH);
                        }
                        state.setLow(reg(regS) / reg(regT));
                        state.setHigh(reg(regS) % reg(regT));
                        break;
                    case 0x1B: //divu
                        if (reg(regT) == 0) {
                            throw new ProgramException(ARIH);
                        }
                        state.setLow((int)(regU(regS) / regU(regT)));
                        state.setHigh((int)(regU(regS) % regU(regT)));
                        break;
                    case 0x20: //add
                        res = (long)reg(regS) + reg(regT);
                        testOverflow(res);
                        reg(regD, (int)res);
                        break;
                    case 0x21: //addu
                        reg(regD, reg(regS) + reg(regT));
                        break;
                    case 0x22: // sub
                        res = (long)reg(regS) - reg(regT);
                        testOverflow(res);
                        reg(regD, (int) res);
                        break;
                    case 0x23: // subu
                        reg(regD, reg(regS) - reg(regT));
                        break;
                    case 0x24: // and
                        reg(regD, reg(regS) & reg(regT));
                        break;
                    case 0x25: // or
                        reg(regD, reg(regS) | reg(regT));
                        break;
                    case 0x26: // xor
                        reg(regD, reg(regS) ^ reg(regT));
                        break;
                    case 0x27: // nor
                        reg(regD, ~(reg(regS) | reg(regT)));
                        break;
                    case 0x2a: // slt
                        reg(regD, reg(regS) < reg(regT) ? 1 : 0);
                        break;
                    case 0x2b: // sltu
                        reg(regD, regU(reg(regS)) < regU(regT) ? 1 : 0);
                        break;
                    default:
                        throw new ProgramException(ErrorType.RI);
                }
                break;
            case 0x01: //RI-type opcodes
                switch(regT) {
                    case 0x00: //bltz
                        if (reg(regS) < 0) {
                            advance = imm << 2;
                        }
                        break;
                    case 0x01: //bgez
                        if (reg(regS) >= 0) {
                            advance = imm << 2;
                        }
                        break;
                    case 0x10: //bltzal
                        if (reg(regS) < 0) {
                            advance = imm << 2;
                            reg(REG_RA, pc + INST_SIZE * 2);
                        }
                        break;
                    case 0x11: //bgezal
                        if (reg(regS) >= 0) {
                            advance = imm << 2;
                            reg(REG_RA, pc + INST_SIZE * 2);
                        }
                        break;
                    default:
                        throw new ProgramException(ErrorType.RI);
                }
                break;
            case 0x02: //j
                state.jump((pc & JMP_FAR_MASK) | (addr << 2));
                break;
            case 0x03: //jal
                reg(REG_RA, pc + INST_SIZE * 2);
                state.jump((pc & JMP_FAR_MASK) | (addr << 2));
                break;
            case 0x04: //beq
                if (reg(regS) == reg(regT)) {
                    advance = imm << 2;
                }
                break;
            case 0x05: //bne
                if (reg(regS) != reg(regT)) {
                    advance = imm << 2;
                }
                break;
            case 0x06: //blez
                if (reg(regS) <= 0) {
                    advance = imm << 2;
                }
                break;
            case 0x07: //bgtz
                if (reg(regS) > 0) {
                    advance = imm << 2;
                }
                break;
            case 0x08: //addi
                long res = (long)reg(regS) + imm;
                testOverflow(res);
                reg(regD, (int) res);
                break;
            case 0x09: //addiu
                reg(regD, reg(regS) + imm);
                break;
            case 0x0a: //slti
                reg(regT, reg(regS) < imm ? 1 : 0);
                break;
            case 0x0b: //sltiu
                reg(regT, regU(regS) < toUnsignedLong(imm) ? 1 : 0);
                break;
            case 0x0c: //andi
                reg(regT, reg(regS) & imm);
                break;
            case 0x0d: //ori
                reg(regT, reg(regS) | imm);
                break;
            case 0x0e: //xori
                reg(regT, reg(regS) ^ imm);
                break;
            case 0x0f: //lui
                reg(regT, imm << Short.SIZE);
                break;
            case 0x20: //lb
                reg(regT, state.get(reg(regS) + imm));
                break;
            case 0x21: //lh
                reg(regT, state.getShort(reg(regS) + imm));
                break;
            case 0x22: //lwl
                //TODO: not implemented
                break;
            case 0x23: //lw
                reg(regT, state.getInt(reg(regS) + imm));
                break;
            case 0x24: //lbu
                reg(regT, toUnsignedInt(state.get(reg(regS) + imm)));
                break;
            case 0x25: //lhu
                reg(regT, toUnsignedInt(state.getShort(reg(regS) + imm)));
                break;
            case 0x26: //lwr
                //TODO: not implemented
                break;
            case 0x28: //sb
                state.set(reg(regS), (byte)reg(regT));
                break;
            case 0x29: //sh
                state.setShort(reg(regS), (short)reg(regT));
                break;
            case 0x2a: //swl
                //Stores upper bytes
                //TODO: not implemented
                break;
            case 0x2b: //sw
                state.setInt(reg(regS), reg(regT));
                break;
            case 0x2e: //swr
                //TODO: not implemented
                break;
            default:
                throw new ProgramException(ErrorType.RI);
        }

        if (advance != -1) {
            state.advancePC(pc);
        }
    }

    /**
     * Tests whether if a 32-bit integer has overflowed
     * @param res the number to test
     * @throws ProgramException when an overflow has occurred (ARIH)
     */
    private void testOverflow(long res) throws ProgramException {
        if (res != ((int)res)) { //Overflow bit is set
            throw new ProgramException(ARIH);
        }
    }

    /**
     * Executes a syscall.
     * @throws ProgramException if a program exception occurs while executing system call
     */
    @SuppressWarnings("MagicNumber")
    private void syscall() throws ProgramException {
        try {
            Scanner scan = new Scanner(System.in);
            switch (reg(REG_V0)) {
                case 1: //print_int
                    out.println(reg(REG_A0));
                    break;
                case 4: //print_string
                    int addr = reg(REG_A0);
                    int c;
                    while ((c = state.get(addr++)) != 0) {
                        out.print((char)c);
                    }
                    break;
                case 5: //read_int
                    reg(REG_V0, scan.nextInt());
                    scan.nextLine();
                    break;
                case 8: //read_string
                    addr = reg(REG_A0);
                    int length = reg(REG_A1);
                    while (length --> 1) {
                        c = in.read();
                        if (c == -1) {
                            break;
                        }
                        state.set(addr++, (byte)c);
                        if (c == '\n') {
                            break;
                        }
                    }
                    break;
                case 9: //sbrk
                    System.out.println(reg(REG_A0));
                    break;
                case 10: //exit
                    throw new ProgramException(ErrorType.EXIT);
                case 11: //print_character
                    out.print((char) reg(REG_A0));
                    break;
                case 12: //read_character
                    reg(REG_V0, in.read());
                    break;
                case 13: //open
                    //TODO: unimplemented.
                    break;
                case 14: //read
                    //TODO: unimplemented.
                    break;
                case 15: //write
                    //TODO: unimplemented.
                    break;
                case 16: //close
                    //TODO: unimplemented.
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new ProgramException(ErrorType.INT);
        }
    }

    int reg(int regind) {
        return state.getRegister(regind);
    }

    long regU(int regind) {
        return toUnsignedLong(state.getRegister(regind));
    }

    void reg(int regind, int val) {
        state.setRegister(regind, val);
    }
}
