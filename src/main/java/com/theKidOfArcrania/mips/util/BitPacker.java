package com.theKidOfArcrania.mips.util;

/**
 * Manages a set of bits, allowing for easier bit manipulation with bit offsets that might not be aligned to byte
 * boundaries. The 0th bit within a byte is defined as the MSB of the number.
 * @author Henry Wang
 */
public class BitPacker {
    private static final int[] MASKS = {0x0, 0x1, 0x3, 0x7, 0xf, 0x1f, 0x3f, 0x7f, 0xff};

    private final int DWORD_BITS = 32;
    private final int WORD_BITS = 16;
    private final int BYTE_BITS = 8;

    private final byte[] bits;
    private final int size;

    /**
     * Constructs a bit packer
     * @param bitSize the size in the number of bits to create with
     */
    public BitPacker(int bitSize) {
        this.size = bitSize;

        int bytes = bitSize / BYTE_BITS;
        if (bytes * BYTE_BITS < bitSize)
            bytes++;
        bits = new byte[bytes];
    }

    /**
     * Sets a range of bits (up to 32 bits)
     * @param offset the bit offset to start at
     * @param length the number of bits to set
     * @param value the value to set to
     */
    public void set(int offset, int length, int value) {
        if (offset < 0 || offset > size || length < 0 || length > size || offset + length > size) {
            throw new BitIndexOutOfBoundsException();
        }

        while (length > 0) {
            int bitStart = offset % BYTE_BITS;
            int count = Math.min(length, BYTE_BITS - bitStart);
            int bitEnd = bitStart + count;

            int val = value >>> (length - count) & MASKS[count];
            int tmp = bits[offset / BYTE_BITS];
            tmp = (tmp & ~MASKS[BYTE_BITS - bitStart]) | val << (BYTE_BITS - bitEnd) |
                    (tmp & MASKS[BYTE_BITS - bitEnd]);
            bits[offset / BYTE_BITS] = (byte)tmp;

            length -= count;
            offset += count;
        }
    }

    /**
     * Fetches a range of bits (up to 32 bits)
     * @param offset the bit offset to start at
     * @param length the number of bits to fetch
     * @return the fetched bits in MSB format.
     */
    public int get(int offset, int length) {
        if (length > Integer.SIZE) {
            throw new IllegalArgumentException("Maximum length is 32 bits");
        }
        checkBounds(offset, length);

        int ret = 0;
        while (length > 0) {
            int bitStart = offset % BYTE_BITS;
            int count = Math.min(length, BYTE_BITS - bitStart);
            int bitEnd = bitStart + count;

            int val = (bits[offset / BYTE_BITS] >>> (BYTE_BITS - bitEnd)) & MASKS[count];
            ret |= val << (length - count);

            length -= count;
            offset += count;
        }
        return ret;
    }


    /**
     * Sets a DWORD (32 bit) value
     * @param offset the bit offset to start at
     * @param value the value to write.
     */
    public void setDWORD(int offset, int value) {
        set(offset, DWORD_BITS, value);
    }

    /**
     * Sets a WORD (16 bit) value
     * @param offset the bit offset to start at
     * @param value the value to write.
     */
    public void setWORD(int offset, int value) {
        set(offset, WORD_BITS, value);
    }

    /**
     * Sets a BYTE (8 bit) value
     * @param offset the bit offset to start at
     * @param value the value to write.
     */
    public void setBYTE(int offset, int value) {
        set(offset, BYTE_BITS, value);
    }

    /**
     * Gets a DWORD (32 bit) value
     * @param offset the bit offset to start at
     * @return a DWORD value
     */
    public int getDWORD(int offset) {
        return get(offset, DWORD_BITS);
    }

    /**
     * Gets a WORD (16 bit) value
     * @param offset the bit offset to start at
     * @return a WORD value
     */
    public short getWORD(int offset) {
        return (short)get(offset, WORD_BITS);
    }

    /**
     * Gets a BYTE (8 bit) value
     * @param offset the bit offset to start at
     * @return a BYTE value
     */
    public byte getBYTE(int offset) {
        return (byte)get(offset, BYTE_BITS);
    }

    /**
     * Obtains the packed byte form of our bit data
     * @return an byte array
     */
    public byte[] toBytes() {
        return bits.clone();
    }

    /**
     * Checks for out-of-bounds.
     * @param offset the bit offset to start at
     * @param length the number of bits in this range
     */
    private void checkBounds(int offset, int length) {
        if (offset < 0 || length < 0 || offset > size || offset + length > size) {
            throw new BitIndexOutOfBoundsException();
        }
    }
}




