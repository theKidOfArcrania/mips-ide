package com.theKidOfArcrania.mips.runner;

import com.theKidOfArcrania.mips.Constants;
import com.theKidOfArcrania.mips.runner.ProgramException.ErrorType;
import com.theKidOfArcrania.mips.util.RangeSet;

import java.util.Set;


/**
 * Represents the memory state (RAM memory and registers) for a MIPS program, including the TEXT/DATA segments.
 * @author Henry Wang
 */
public class MemState implements Constants, Registers {

    /**
     * Represents a single memory segment
     */
    private static class Segment {
        private final int start;
        private final byte[] data;

        /**
         * Constructs a memory segment
         * @param start the starting/base address
         * @param size the size of this memory segment
         */
        public Segment(int start, int size) {
            this.start = start;
            this.data = new byte[size];
        }

        /**
         * Gets a byte at the address within this segment
         * @param addr the absolute address to fetch
         * @return the byte value
         */
        public byte get(int addr) {
            return data[addr - start];
        }

        /**
         * Bulk get method.
         * @param addr the address to start getting from
         * @param buff the byte buffer to write to
         * @param offset the index offset of buffer to start from
         * @param length the number of bytes to get.
         */
        public void get(int addr, byte[] buff, int offset, int length) {
            System.arraycopy(data, addr - start, buff, offset, length);
        }

        /**
         * Sets a byte at the address within this segment
         * @param addr the absolute address to set
         * @param b the value to set to
         */
        public void set(int addr, byte b) {
            data[addr - start] = b;
        }

        /**
         * Bulk set method.
         * @param addr the address to start setting to
         * @param buff the byte buffer to read from
         * @param offset the index offset of buffer to start from
         * @param length the number of bytes to set.
         */
        public void set(int addr, byte[] buff, int offset, int length) {
            System.arraycopy(buff, offset, data, addr - start, length);
        }
    }

    private static final long BYTE_MASK = 0xFFL;

    private int pc;
    private int nPC;

    private int high;
    private int low;
    private final int[] regs = new int[REGISTER_COUNT];
    private final RangeSet<Segment> memory = new RangeSet<>();

    //Program counter operations
    /**
     * @return the current program counter
     */
    public int pc() {
        return pc;
    }

    /**
     * Sets the next instruction to execute.
     * @param pc the next program counter value
     */
    public void pc(int pc) {
        this.pc = pc;
        this.nPC = pc + INST_SIZE;
    }

    /**
     * Advances the program counter by an offset number of bytes.
     * @param offset the offset in bytes to advance the program counter.
     */
    public void advancePC(int offset) {
        pc = nPC;
        nPC += offset;
    }

    /**
     * Jumps the program counter to a specified address
     * @param address the address to jump to
     */
    public void jump(int address) {
        pc = nPC;
        nPC = address;
    }

    //Register operations
    /**
     * Gets a register value
     * @param regInd the register index to get
     * @return the value of the register
     */
    public int getRegister(int regInd) {
        return regs[regInd];
    }

    /**
     * Sets a register value
     * @param regInd the register index to set
     * @param val the value to set it to
     */
    public void setRegister(int regInd, int val) {
        regs[regInd] = val;
    }

    public int getHigh() {
        return high;
    }

    public void setHigh(int high) {
        this.high = high;
    }

    public int getLow() {
        return low;
    }

    public void setLow(int low) {
        this.low = low;
    }

    //Memory operations
    /**
     * Allocates a memory segment at a starting base address.
     * @param start the starting/base address
     * @param size the size of this memory segment
     */
    public void allocateSegment(int start, int size) {
        if (start < 0 || size < 0) {
            throw new IndexOutOfBoundsException();
        }

        if (start + size < 0) {
            throw new IllegalArgumentException("Integer overflow alert!");
        }

        if (!memory.isRangeEmpty(start, start + size)) {
            throw new IllegalArgumentException("Cannot allocate overlapping memory segments");
        }

        if (size == 0) {
            return;
        }
        memory.add(start, start + size, new Segment(start, size));
    }

    /**
     * Gets a single byte
     * @param addr address to get byte
     * @return the value
     * @throws ProgramException if the address does not map to a valid segment (ADDRL).
     */
    public byte get(int addr) throws ProgramException{
        return getSegment(addr, true).get(addr);
    }

    /**
     * Bulk get method.
     * @param addr the address to start getting from
     * @param buff the byte buffer to write to
     * @param offset the index offset of buffer to start from
     * @param length the number of bytes to get.
     * @throws ProgramException if the address does not map to a valid segment (ADDRL).
     */
    public void get(int addr, byte[] buff, int offset, int length) throws ProgramException {
        if (offset < 0 || offset > buff.length || length > buff.length || offset + length > buff.length)
            throw new ProgramException(ErrorType.ADDRL);

        while (length > 0) {
            Segment seg = getSegment(addr, true);
            int reading = Math.min(length, seg.start + seg.data.length - addr);
            if (reading == 0) {
                throw new InternalError();
            }

            seg.get(addr, buff, offset, reading);

            offset += reading;
            addr += reading;
            length -= reading;
        }
    }

    /**
     * Bulk convenience get method.
     * @param addr the address to start getting from
     * @param buff the byte buffer to write to.
     * @throws ProgramException if the address does not map to a valid segment (ADDRL).
     */
    public void get(int addr, byte[] buff) throws ProgramException {
        get(addr, buff, 0, buff.length);
    }

    /**
     * Bulk convenience get method.
     * @param addr the address to start getting from
     * @return a 16-bit integer in little endian.
     * @throws ProgramException if the address does not map to a valid segment, or if address is misaligned (ADDRL).
     */
    public short getShort(int addr) throws ProgramException {
        return (short)getLittleEndian(addr, Short.BYTES);
    }

    /**
     * Bulk convenience get method.
     * @param addr the address to start getting from
     * @return a 32-bit integer in little endian.
     * @throws ProgramException if the address does not map to a valid segment, or if address is misaligned (ADDRL).
     */
    public int getInt(int addr) throws ProgramException {
        return (int)getLittleEndian(addr, Integer.BYTES);
    }

    /**
     * Bulk convenience get method.
     * @param addr the address to start getting from
     * @return a 64-bit integer in little endian.
     * @throws ProgramException if the address does not map to a valid segment, or if address is misaligned (ADDRL).
     */
    public long getLong(int addr) throws ProgramException {
        return getLittleEndian(addr, Long.BYTES);
    }

    /**
     * Sets a single byte
     * @param addr address to set byte
     * @param val the value to set
     * @throws ProgramException if the address does not map to a valid segment (ADDRS).
     */
    public void set(int addr, byte val) throws ProgramException{
        getSegment(addr, false).set(addr, val);
    }

    /**
     * Bulk set method.
     * @param addr the address to start setting to
     * @param buff the byte buffer to read from
     * @param offset the index offset of buffer to start from
     * @param length the number of bytes to set.
     * @throws ProgramException if the address does not map to a valid segment (ADDRS).
     */
    public void set(int addr, byte[] buff, int offset, int length) throws ProgramException{
        if (offset < 0 || offset > buff.length || length > buff.length || offset + length > buff.length)
            throw new ProgramException(ErrorType.ADDRS);

        while (length > 0) {
            Segment seg = getSegment(addr, false);
            int reading = Math.min(length, seg.start + seg.data.length - addr);
            if (reading == 0) {
                throw new InternalError();
            }

            seg.set(addr, buff, offset, reading);

            offset += reading;
            addr += reading;
            length -= reading;
        }
    }

    /**
     * Bulk convenience set method.
     * @param addr the address to start setting to
     * @param buff the byte buffer to read from.
     * @throws ProgramException if the address does not map to a valid segment (ADDRS).
     */
    public void set(int addr, byte[] buff) throws ProgramException {
        set(addr, buff, 0, buff.length);
    }

    /**
     * Bulk convenience set method.
     * @param addr the address to start setting to
     * @param val 16-bit integer to write
     * @throws ProgramException if the address does not map to a valid segment, or if address is misaligned (ADDRS).
     */
    public void setShort(int addr, short val) throws ProgramException {
        setLittleEndian(addr, Short.BYTES, val);
    }

    /**
     * Bulk convenience set method.
     * @param addr the address to start setting to
     * @param val 32-bit integer to write
     * @throws ProgramException if the address does not map to a valid segment, or if address is misaligned (ADDRS).
     */
    public void setInt(int addr, int val) throws ProgramException {
        setLittleEndian(addr, Integer.BYTES, val);
    }

    /**
     * Bulk convenience set method.
     * @param addr the address to start setting to
     * @param val 64-bit integer to write
     * @throws ProgramException if the address does not map to a valid segment, or if address is misaligned (ADDRS).
     */
    public void setLong(int addr, long val) throws ProgramException {
        setLittleEndian(addr, Long.BYTES, val);
    }

    /**
     * Fetches a little-endian integer of a particular size from memory
     * @param addr the address to read from
     * @param bytes the size of the integer
     * @return the read integer
     * @throws ProgramException if the address does not map to a valid segment or address is not aligned (ADDRL).
     */
    private long getLittleEndian(int addr, int bytes) throws ProgramException {
        if (addr % bytes != 0) {
            throw new ProgramException(ErrorType.ADDRL);
        }

        byte[] buff = new byte[bytes];
        get(addr, buff);

        long ret = 0;
        for (int i = 0; i < bytes; i++) {
            ret |= (buff[i] & BYTE_MASK) << (i * Byte.SIZE);
        }

        return ret;
    }

    /**
     * Writes a little-endian integer of a particular size to memory
     * @param addr the address to write to
     * @param bytes the size of the integer
     * @param val the integer to write
     * @throws ProgramException if the address does not map to a valid segment, or if address is misaligned (ADDRS).
     */
    private void setLittleEndian(int addr, int bytes, long val) throws ProgramException {
        if ((addr & bytes - 1) != 0) {
            throw new ProgramException(ErrorType.ADDRL);
        }

        byte[] buff = new byte[bytes];
        for (int i = 0; i < bytes; i++) {
            buff[i] = (byte)(val >> (i * Byte.SIZE));
        }

        set(addr, buff);
    }

    /**
     * Fetches a memory segment associated that contains the address
     * @param addr the address to find
     * @param get whether if this is a get or set operation
     * @return a memory segment if found
     * @throws ProgramException if the address does not map to a valid segment (ADDRL/ADDRS).
     */
    private Segment getSegment(int addr, boolean get) throws ProgramException{
        Set<Segment> found = memory.get(addr);
        if (found.isEmpty())
            throw new ProgramException(get ? ErrorType.ADDRL : ErrorType.ADDRS);
        return found.iterator().next();
    }
}
