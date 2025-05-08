package com.github.xingshuangs.iot.protocol.s7.model;


import com.github.xingshuangs.iot.exceptions.S7CommException;
import com.github.xingshuangs.iot.protocol.s7.enums.EFunctionCode;
import com.github.xingshuangs.iot.utils.ByteUtil;
import com.github.xingshuangs.iot.utils.ShortUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 启动参数
 *
 * @author xingshuang
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class StartParameter extends Parameter implements IByteArray {

    public static final String P_PROGRAM = "P_PROGRAM";

    /**
     * 未知字节，固定参数 <br>
     * 字节大小：7 <br>
     * 字节序数：1-7
     */
    private byte[] unknownBytes = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xFD};

    /**
     * 参数块长度 <br>
     * 字节大小：2 <br>
     * 字节序数：8-9
     */
    private int parameterBlockLength = 0;

    /**
     * 参数块内容
     */
    private String parameterBlock = "";

    /**
     * 服务名长度 <br>
     * 字节大小：1 <br>
     * 字节序数：不定
     */
    private int lengthPart = 0;

    /**
     * 程序调用的服务名
     */
    private String piService = "";

    public void setParameterBlock(String parameterBlock) {
        this.parameterBlock = parameterBlock;
        this.parameterBlockLength = this.parameterBlock.length();
    }

    public void setPiService(String piService) {
        this.piService = piService;
        this.lengthPart = this.piService.length();
    }

    public StartParameter() {
        this.functionCode = EFunctionCode.PLC_CONTROL;
    }

    @Override
    public int byteArrayLength() {
        return 1 + 7 + 2 + this.parameterBlockLength + 1 + this.lengthPart;
    }

    @Override
    public byte[] toByteArray() {
        byte[] parameterLengthBytes = ShortUtil.toByteArray(this.parameterBlockLength);

        byte[] res = new byte[this.byteArrayLength()];
        int index = 0;
        res[index++] = this.functionCode.getCode();

        System.arraycopy(this.unknownBytes, 0, res, 1, this.unknownBytes.length);
        index += this.unknownBytes.length;

        res[index++] = parameterLengthBytes[0];
        res[index++] = parameterLengthBytes[1];

        byte[] blockBytes = this.parameterBlock.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(blockBytes, 0, res, index, blockBytes.length);
        index += blockBytes.length;

        res[index++] = ByteUtil.toByte(this.lengthPart);

        byte[] piServiceBytes = this.piService.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(piServiceBytes, 0, res, index, piServiceBytes.length);
        return res;
    }

    /**
     * 字节数组数据解析
     *
     * @param data 字节数组数据
     * @return StopParameter
     */
    public static StartParameter fromBytes(final byte[] data) {
        if (data.length < 11) {
            throw new S7CommException("StopParameter解析有误，StopParameter字节数组长度 < 7");
        }
        int offset = 0;
        StartParameter parameter = new StartParameter();
        parameter.functionCode = EFunctionCode.from(data[0]);
        parameter.unknownBytes = Arrays.copyOfRange(data, 1, 8);
        parameter.parameterBlockLength = ShortUtil.toUInt16(data, 8);
        parameter.parameterBlock = parameter.parameterBlockLength == 0 ? "" : new String(data, 10, parameter.parameterBlockLength, StandardCharsets.US_ASCII);
        offset = offset + 10 + parameter.parameterBlockLength;
        parameter.lengthPart = ByteUtil.toUInt8(data[offset++]);
        parameter.piService = parameter.lengthPart == 0 ? "" : new String(data, offset, parameter.lengthPart, StandardCharsets.US_ASCII);
        return parameter;
    }

    /**
     * 热重启
     *
     * @return startParameter
     */
    public static StartParameter hotRestart() {
        StartParameter parameter = new StartParameter();
        parameter.setParameterBlock("");
        parameter.setPiService(P_PROGRAM);
        return parameter;
    }

    /**
     * 冷启动
     *
     * @return startParameter
     */
    public static StartParameter coldRestart() {
        StartParameter parameter = new StartParameter();
        parameter.setParameterBlock("C ");
        parameter.setPiService(P_PROGRAM);
        return parameter;
    }
}
