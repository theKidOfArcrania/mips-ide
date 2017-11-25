package com.theKidOfArcrania.mips.util;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

@SuppressWarnings({"JavaDoc", "MagicNumber"})
public class BitPackerTest {

    BitPacker bits;

    @Before
    public void setUp() {
        bits = new BitPacker(32);
    }

    @Test
    public void set1() throws Exception {
        bits.set(1, 12, 0b101010101010);
        assertArrayEquals(new byte[] {0b01010101, 0b01010000, 0, 0}, bits.toBytes());
    }

    @Test
    public void set2() throws Exception {
        bits.set(1, 12, 0b101010101010);
        bits.set(4, 7, 0b1010101);
        assertArrayEquals(new byte[] {0b01011010, (byte)0b10110000, 0, 0}, bits.toBytes());
    }

    @Test
    public void set3() throws Exception {
        bits.set(1, 12, 0b101010101010);
        bits.set(4, 7, 0b1010101);
        bits.setWORD(1, 0b1011101010101011);
        assertArrayEquals(new byte[] {0b01011101, 0b01010101, (byte)0b10000000, 0}, bits.toBytes());
    }

    @Test
    public void get1() throws Exception {
        bits.setDWORD(0, 0b10101010101010101010101010101010);
        assertEquals(0b01010101, bits.get(3, 8));
    }

    @Test
    public void get2() throws Exception {
        bits.setDWORD(0, 0b10101010101010101010101010101010);
        assertEquals(0b0101010101010101, bits.get(3, 16));
    }
}