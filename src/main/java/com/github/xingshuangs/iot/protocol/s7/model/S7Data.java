package com.github.xingshuangs.iot.protocol.s7.model;


import com.github.xingshuangs.iot.protocol.common.IObjectByteArray;
import com.github.xingshuangs.iot.protocol.common.buff.ByteWriteBuff;
import com.github.xingshuangs.iot.protocol.s7.enums.EErrorClass;
import com.github.xingshuangs.iot.protocol.s7.enums.EFunctionCode;
import lombok.Data;

import java.util.Arrays;
import java.util.List;

/**
 * S7数据结构
 *
 * @author xingshuang
 */
@Data
public class S7Data implements IObjectByteArray {

    /**
     * TPKT
     */
    private TPKT tpkt;

    /**
     * COTP
     */
    private COTP cotp;

    /**
     * 头
     */
    private Header header;

    /**
     * 参数
     */
    private Parameter parameter;

    /**
     * 数据
     */
    private Datum datum;

    @Override
    public int byteArrayLength() {
        int length = 0;
        length += this.tpkt != null ? this.tpkt.byteArrayLength() : 0;
        length += this.cotp != null ? this.cotp.byteArrayLength() : 0;
        length += this.header != null ? this.header.byteArrayLength() : 0;
        length += this.parameter != null ? this.parameter.byteArrayLength() : 0;
        length += this.datum != null ? this.datum.byteArrayLength() : 0;
        return length;
    }

    @Override
    public byte[] toByteArray() {
        ByteWriteBuff buff = ByteWriteBuff.newInstance(this.byteArrayLength());
        if (this.tpkt != null) {
            buff.putBytes(this.tpkt.toByteArray());
        }
        if (this.cotp != null) {
            buff.putBytes(this.cotp.toByteArray());
        }
        if (this.header != null) {
            buff.putBytes(this.header.toByteArray());
        }
        if (this.parameter != null) {
            buff.putBytes(this.parameter.toByteArray());
        }
        if (this.datum != null) {
            buff.putBytes(this.datum.toByteArray());
        }
        return buff.getData();
    }

    /**
     * 自我数据校验
     */
    public void selfCheck() {
        if (this.header != null) {
            this.header.setDataLength(0);
            this.header.setParameterLength(0);
        }
        if (this.parameter != null && this.header != null) {
            this.header.setParameterLength(this.parameter.byteArrayLength());
        }
        if (this.datum != null && this.header != null) {
            this.header.setDataLength(this.datum.byteArrayLength());
        }
        if (this.tpkt != null) {
            this.tpkt.setLength(this.byteArrayLength());
        }
    }

    /**
     * 根据字节数据解析S7协议数据
     *
     * @param data 数据字节
     * @return s7数据
     */
    public static S7Data fromBytes(final byte[] data) {
        byte[] tpktBytes = Arrays.copyOfRange(data, 0, TPKT.BYTE_LENGTH);
        TPKT tpkt = TPKT.fromBytes(tpktBytes);
        byte[] remainBytes = Arrays.copyOfRange(data, TPKT.BYTE_LENGTH, data.length);
        return fromBytes(tpkt, remainBytes);
    }

    /**
     * 根据字节数据解析S7协议数据
     *
     * @param tpkt   tpkt
     * @param remain 剩余字节数据
     * @return s7数据
     */
    public static S7Data fromBytes(TPKT tpkt, final byte[] remain) {
        // tpkt
        S7Data s7Data = new S7Data();
        s7Data.tpkt = tpkt;
        // cotp
        COTP cotp = COTPBuilder.fromBytes(remain);
        s7Data.cotp = cotp;
        if (cotp == null || remain.length <= cotp.byteArrayLength()) {
            return s7Data;
        }

        //-----------------------------S7通信部分的内容--------------------------------------------
        byte[] lastBytes = Arrays.copyOfRange(remain, cotp.byteArrayLength(), remain.length);
        // header
        Header header = HeaderBuilder.fromBytes(lastBytes);
        s7Data.header = header;
        if (header == null) {
            return s7Data;
        }
        // parameter
        if (header.getParameterLength() > 0) {
            byte[] parameterBytes = Arrays.copyOfRange(lastBytes, header.byteArrayLength(), header.byteArrayLength() + header.getParameterLength());
            s7Data.parameter = ParameterBuilder.fromBytes(parameterBytes, s7Data.header.getMessageType());
        }
        // datum
        if (header.getDataLength() > 0) {
            byte[] dataBytes = Arrays.copyOfRange(lastBytes, header.byteArrayLength() + header.getParameterLength(), header.byteArrayLength() + header.getParameterLength() + header.getDataLength());
            s7Data.datum = Datum.fromBytes(dataBytes, s7Data.header.getMessageType(), s7Data.parameter.getFunctionCode());
        }
        return s7Data;
    }

    /**
     * 创建连接请求
     *
     * @param local  本地参数
     * @param remote 远程参数
     * @return s7data数据
     */
    public static S7Data createConnectRequest(int local, int remote) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPConnection.crConnectRequest(local, remote);
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建连接确认
     *
     * @param request 请求参数
     * @return s7data数据
     */
    public static S7Data createConnectConfirm(S7Data request) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPConnection.crConnectConfirm((COTPConnection) request.cotp);
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建连接setup
     *
     * @param pduLength PDU长度
     * @return s7data数据
     */
    public static S7Data createConnectDtData(int pduLength) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = SetupComParameter.createDefault(pduLength);
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建连接响应setup
     *
     * @param request 请求数据
     * @return s7data数据
     */
    public static S7Data createConnectAckDtData(S7Data request) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = request.cotp;
        s7Data.header = AckHeader.createDefault(request.header, EErrorClass.NO_ERROR, 0);
        s7Data.parameter = request.parameter;
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建错误响应
     *
     * @param request    请求对象
     * @param errorClass 错误类
     * @param errorCode  错误码
     * @return S7数据
     */
    public static S7Data createErrorResponse(S7Data request, EErrorClass errorClass, int errorCode) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = AckHeader.createDefault(request.header, errorClass, errorCode);
        s7Data.parameter = ReadWriteParameter.createAckParameter((ReadWriteParameter) request.parameter);
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建默认读对象
     *
     * @param requestItems 请求项
     * @return S7Data
     */
    public static S7Data createReadRequest(List<RequestItem> requestItems) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = ReadWriteParameter.createReqParameter(EFunctionCode.READ_VARIABLE, requestItems);
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建默认写对象
     *
     * @param requestItems 请求项
     * @param dataItems    数据项
     * @return S7Data
     */
    public static S7Data createWriteRequest(List<RequestItem> requestItems, List<DataItem> dataItems) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = ReadWriteParameter.createReqParameter(EFunctionCode.WRITE_VARIABLE, requestItems);
        s7Data.datum = Datum.createDatum(dataItems);
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建读写响应
     *
     * @param request     请求对象
     * @param returnItems 返回数据内容
     * @return 响应数据
     */
    public static S7Data createReadWriteResponse(S7Data request, List<ReturnItem> returnItems) {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = AckHeader.createDefault(request.header, EErrorClass.NO_ERROR, 0);
        s7Data.parameter = ReadWriteParameter.createAckParameter((ReadWriteParameter) request.parameter);
        s7Data.datum = Datum.createDatum(returnItems);
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建热启动
     *
     * @return S7Data
     */
    public static S7Data createHotRestart() {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = PlcControlParameter.hotRestart();
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建冷启动命令
     *
     * @return S7Data
     */
    public static S7Data createColdRestart() {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = PlcControlParameter.coldRestart();
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建PLC停止命令
     *
     * @return S7Data
     */
    public static S7Data createPlcStop() {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = PlcStopParameter.createDefault();
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建复制Ram到Rom的命令
     *
     * @return S7Data
     */
    public static S7Data createCopyRamToRom() {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = PlcControlParameter.copyRamToRom();
        s7Data.selfCheck();
        return s7Data;
    }

    /**
     * 创建压缩命令
     *
     * @return S7Data
     */
    public static S7Data createCompress() {
        S7Data s7Data = new S7Data();
        s7Data.tpkt = new TPKT();
        s7Data.cotp = COTPData.createDefault();
        s7Data.header = Header.createDefault();
        s7Data.parameter = PlcControlParameter.compress();
        s7Data.selfCheck();
        return s7Data;
    }
}
