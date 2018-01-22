package com.theKidOfArcrania.mips.runner;

import com.theKidOfArcrania.mips.Constants;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"JavaDoc", "MagicNumber"})
public class MemStateTest implements Constants {

    private MemState mem;

    @Before
    public void setUp() throws Exception {
        mem = new MemState();
        mem.allocateSegment(0, 10);
        mem.allocateSegment(10, 13);
        //Gap at index 23
        mem.allocateSegment(24, 1);
        mem.allocateSegment(25, 2);
        mem.allocateSegment(27, 50);
    }

    @Test(expected = IllegalArgumentException.class)
    public void memoverlap1() throws Exception {
        mem.allocateSegment(23, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void memoverlap2() throws Exception {
        mem.allocateSegment(24, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void memoverlap3() throws Exception {
        mem.allocateSegment(1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void memoverlap4() throws Exception {
        mem.allocateSegment(29, 1);
    }

    @Test
    public void memalloc1() throws Exception {
        mem.allocateSegment(23, 1);
        byte buff[] = new byte[50];
        mem.get(0, buff);
    }

    @Test
    public void memalloc2() throws Exception {
        mem.allocateSegment(23, 0);
    }

    @Test(expected = ProgramException.class)
    public void memalign1() throws Exception {
        mem.setInt(1, 2);
    }

    @Test(expected = ProgramException.class)
    public void memalign2() throws Exception {
        mem.getInt(1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void memoverflow() throws Exception {
        mem.allocateSegment(Integer.MAX_VALUE - 1, 20);
    }

    @Test
    public void registers() throws Exception {
        for (int i = 0; i < REGISTER_COUNT; i++) {
            assertEquals(0, mem.getRegister(i));
        }

        mem.setRegister(2, 12345);
        assertEquals(12345, mem.getRegister(2));
    }

    @Test
    public void blockShorts() throws Exception {
        mem.setShort(4, (short) 0xCAFE);
        assertEquals(0xCAFE, mem.getShort(4) & 0xFFFF);
        assertEquals(0, mem.get(6));
    }

    @Test
    public void blockInts() throws Exception {
        mem.setInt(4, 0xCAFEBABE);
        assertEquals(0xCAFEBABEL, mem.getInt(4) & 0xFFFFFFFFL);
        assertEquals(0, mem.get(8));
    }

    @Test
    public void blockLongs() throws Exception {
        mem.setLong(0, 0xCAFEBABECAFEBABEL);
        assertEquals(0xCAFEBABECAFEBABEL, mem.getLong(0));
        assertEquals(0, mem.get(8));
    }

    @Test
    public void endianness() throws Exception {
        mem.setInt(0, 0x12345678);

        assertEquals(0x12345678, mem.getInt(0));
        assertEquals(0x78, mem.get(0));
        assertEquals(0x56, mem.get(1));
        assertEquals(0x34, mem.get(2));
        assertEquals(0x12, mem.get(3));
    }

    @Test
    public void endianness2() throws Exception {
        mem.setInt(0, 0xCAFEBABE);

        assertEquals(0xCAFEBABE, mem.getInt(0));
        assertEquals(0xBE, mem.get(0) & 0xFF);
        assertEquals(0xBA, mem.get(1) & 0xFF);
        assertEquals(0xFE, mem.get(2) & 0xFF);
        assertEquals(0xCA, mem.get(3) & 0xFF);
    }

    @Test
    public void crossPage() throws Exception {
        mem.setLong(24, 0xCAFEBABECAFEBABEL);
        assertEquals(0xCAFEBABECAFEBABEL, mem.getLong(24));
    }

    @Test
    public void simpleBytes() throws Exception {
        mem.set(24, (byte) 0xFF);
        assertEquals(0xFF, mem.get(24) & 0xFF);
    }

    @Test(expected = ProgramException.class)
    public void segfault1() throws Exception {
        mem.get(23);
    }

    @Test(expected = ProgramException.class)
    public void segfault2() throws Exception {
        byte[] buff = new byte[24];
        mem.get(0, buff);
    }
}