package com.github.oscura.iot.parse.hex;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class HexParseTest {

    private HexParse hexParse;

    @Before
    public void init() {
        this.hexParse = new HexParse(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x81, (byte) 0x00,
                (byte) 0x00, (byte) 0x64, (byte) 0x59, (byte) 0xC1, (byte) 0x79, (byte) 0xEB, (byte) 0x85,
                (byte) 0xC0, (byte) 0xEB, (byte) 0x98, (byte) 0x95, (byte) 0x55, (byte) 0x1D, (byte) 0x68, (byte) 0xC7,
                (byte) 0xE5, (byte) 0xA4, (byte) 0xA9, (byte) 0xE6, (byte) 0xB0, (byte) 0x94, (byte) 0xE5, (byte) 0xA5, (byte) 0xBD,
                (byte) 0x32, (byte) 0x33, (byte) 0x41});
    }

    @Test
    public void toBoolean() {
        List<Boolean> list = this.hexParse.toBoolean(3, 0, 1);
        assertArrayEquals(new Boolean[]{true}, list.toArray(new Boolean[0]));
        list = this.hexParse.toBoolean(3, 0, 3);
        assertArrayEquals(new Boolean[]{true, false, false}, list.toArray(new Boolean[0]));
        list = this.hexParse.toBoolean(3, 6, 4);
        assertArrayEquals(new Boolean[]{false, true, false, false}, list.toArray(new Boolean[0]));
    }

    @Test
    public void toInt8() {
        List<Byte> list = this.hexParse.toInt8(1, 2);
        assertArrayEquals(new Byte[]{(byte) 0xFF, (byte) 0xFF}, list.toArray(new Byte[0]));
        list = this.hexParse.toInt8(2, 3);
        assertArrayEquals(new Byte[]{(byte) 0xFF, (byte) 0x81, (byte) 0x00}, list.toArray(new Byte[0]));
    }

    @Test
    public void toUInt8() {
        List<Integer> list = this.hexParse.toUInt8(1, 2);
        assertArrayEquals(new Integer[]{0xFF, 0xFF}, list.toArray(new Integer[0]));
        list = this.hexParse.toUInt8(2, 3);
        assertArrayEquals(new Integer[]{0xFF, 0x81, 0x00}, list.toArray(new Integer[0]));
    }

    @Test
    public void toInt16() {
        List<Short> list = this.hexParse.toInt16(0, 2, false);
        assertArrayEquals(new Short[]{-1, (short) -127}, list.toArray(new Short[0]));
        list = this.hexParse.toInt16(4, 2, false);
        assertArrayEquals(new Short[]{0, (short) 25689}, list.toArray(new Short[0]));
    }

    @Test
    public void toUInt16() {
        List<Integer> list = this.hexParse.toUInt16(0, 2, false);
        assertArrayEquals(new Integer[]{65535, 65409}, list.toArray(new Integer[0]));
        list = this.hexParse.toUInt16(4, 2, false);
        assertArrayEquals(new Integer[]{0, 25689}, list.toArray(new Integer[0]));
    }

    @Test
    public void toInt32() {
        List<Integer> list = this.hexParse.toInt32(0, 1, false);
        assertArrayEquals(new Integer[]{-127}, list.toArray(new Integer[0]));
    }

    @Test
    public void toUInt32() {
        List<Long> list = this.hexParse.toUInt32(0, 1, false);
        assertArrayEquals(new Long[]{4294967169L}, list.toArray(new Long[0]));
    }

    @Test
    public void toFloat32() {
        List<Float> list = this.hexParse.toFloat32(8, 1, false);
        assertArrayEquals(new Float[]{-15.62f}, list.toArray(new Float[0]));
    }

    @Test
    public void toFloat64() {
        List<Double> list = this.hexParse.toFloat64(12, 1, false);
        assertArrayEquals(new Double[]{-56516.66664}, list.toArray(new Double[0]));
    }

    @Test
    public void toStringUtf8() {
        String actual = this.hexParse.toStringUtf8(20, 9);
        assertEquals("天气好", actual);
        actual = this.hexParse.toStringUtf8(29, 3);
        assertEquals("23A", actual);
    }
}
