package com.github.xingshuangs.iot.protocol.modbus.model;

import com.github.xingshuangs.iot.protocol.modbus.enums.EMbFunctionCode;
import org.junit.Test;

import static org.junit.Assert.*;


public class MbWriteSingleRegisterRequestTest {

    @Test
    public void toByteArray() {
        byte[] actual = new byte[]{(byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x03};
        MbWriteSingleRegisterRequest mb = new MbWriteSingleRegisterRequest();
        mb.setFunctionCode(EMbFunctionCode.WRITE_SINGLE_REGISTER);
        mb.setAddress(1);
        mb.setValue(3);
        assertEquals(5, mb.byteArrayLength());
        assertArrayEquals(actual, mb.toByteArray());
    }

    @Test
    public void fromBytes() {
        byte[] data = new byte[]{(byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x03};
        MbWriteSingleRegisterRequest mb = MbWriteSingleRegisterRequest.fromBytes(data);
        assertEquals(EMbFunctionCode.WRITE_SINGLE_REGISTER, mb.functionCode);
        assertEquals(1, mb.getAddress());
        assertEquals(3, mb.getValue());
    }

}