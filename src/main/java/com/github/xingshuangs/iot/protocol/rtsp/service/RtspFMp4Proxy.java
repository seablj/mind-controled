package com.github.xingshuangs.iot.protocol.rtsp.service;


import com.github.xingshuangs.iot.protocol.common.IObjectByteArray;
import com.github.xingshuangs.iot.protocol.mp4.model.*;
import com.github.xingshuangs.iot.protocol.rtp.enums.EFrameType;
import com.github.xingshuangs.iot.protocol.rtp.enums.EH264NaluType;
import com.github.xingshuangs.iot.protocol.rtp.model.frame.H264VideoFrame;
import com.github.xingshuangs.iot.protocol.rtsp.model.sdp.RtspTrackInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author xingshuang
 */
@Slf4j
public class RtspFMp4Proxy {

    private final Object objLock = new Object();

    /**
     * RTSP客户端
     */
    private final RtspClient client;

    /**
     * 轨道信息
     */
    private RtspTrackInfo trackInfo;

    /**
     * 接收帧数据的序列号
     */
    private long sequenceNumber = 1;

    /**
     * 数据缓存
     */
    private final LinkedList<IObjectByteArray> buffers = new LinkedList<>();

    /**
     * FMp4数据事件
     */
    private Consumer<byte[]> fmp4DataHandle;

    /**
     * codec的处理事件
     */
    private Consumer<String> codecHandle;

    /**
     * 是否终止
     */
    private boolean terminal = false;

    /**
     * MP4的头
     */
    private Mp4Header mp4Header;

    /**
     * 轨道信息
     */
    private Mp4TrackInfo mp4TrackInfo;

    /**
     * 是否异步步发送
     */
    private boolean asyncSend = false;

    private H264VideoFrame lastFrame;

    private List<H264VideoFrame> gop = new ArrayList<>();

    private CompletableFuture<Void> future;

    public Mp4Header getMp4Header() {
        return mp4Header;
    }

    public void onFmp4DataHandle(Consumer<byte[]> fmp4DataHandle) {
        this.fmp4DataHandle = fmp4DataHandle;
    }

    public void onCodecHandle(Consumer<String> codecHandle) {
        this.codecHandle = codecHandle;
    }

    public RtspFMp4Proxy(RtspClient client) {
        this(client, false);
    }

    public RtspFMp4Proxy(RtspClient client, boolean asyncSend) {
        this.client = client;
        this.client.onFrameHandle(x -> {
            H264VideoFrame f = (H264VideoFrame) x;
            this.initHeaderHandle();
            this.frameHandle(f);
        });
        this.asyncSend = asyncSend;
        if (this.asyncSend) {
            this.future = CompletableFuture.runAsync(this::executeHandle);
        }
    }

    /**
     * 初始化头部事件
     */
    private void initHeaderHandle() {
        if (this.trackInfo == null) {
            this.trackInfo = this.client.getTrackInfo();
            log.debug(this.trackInfo.toString());
            if (this.codecHandle != null) {
                this.codecHandle.accept(this.trackInfo.getCodec());
            }

            this.mp4TrackInfo = this.toMp4TrackInfo(this.trackInfo);
            this.mp4Header = new Mp4Header(mp4TrackInfo);
            this.addFMp4Data(mp4Header);
        }
    }

    /**
     * 帧处理事件
     *
     * @param frame 数据帧
     */
    private void frameHandle(H264VideoFrame frame) {
        if (frame.getFrameType() == EFrameType.AUDIO) {
            return;
        }
        if (frame.getNaluType() == EH264NaluType.PPS
                || frame.getNaluType() == EH264NaluType.SPS
                || frame.getNaluType() == EH264NaluType.SEI) {
            return;
        }
        // 这里的作用是缓存10个帧数据，重新排序，因为可能收到的帧时间戳不是按时间顺序排列
        this.gop.add(frame);
        if (this.gop.size() < 10) {
            return;
        }
        this.gop.sort((a, b) -> (int) (a.getTimestamp() - b.getTimestamp()));
        H264VideoFrame videoFrame = this.gop.remove(0);

        if (this.lastFrame == null) {
            this.lastFrame = videoFrame;
        }
        if (this.lastFrame.getTimestamp() > videoFrame.getTimestamp()) {
            log.error("出现一帧数据时间戳小于之前的一帧的时间戳");
        }
        this.lastFrame = videoFrame;

        Mp4SampleData sampleData = new Mp4SampleData();
        sampleData.setData(videoFrame.getFrameSegment());
        sampleData.setTimestamp(videoFrame.getTimestamp());
        sampleData.getFlags().setDependedOn(videoFrame.getNaluType() == EH264NaluType.IDR_SLICE ? 2 : 1);
        sampleData.getFlags().setIsNonSync(videoFrame.getNaluType() == EH264NaluType.IDR_SLICE ? 0 : 1);

        if (videoFrame.getNaluType() == EH264NaluType.IDR_SLICE) {
            // 当前是IDR帧，发送并清空之前的数据，然后发送IDR帧
            if (!this.mp4TrackInfo.getSampleData().isEmpty()) {
                this.addSampleData();
            }
            this.mp4TrackInfo.getSampleData().add(sampleData);
            this.addSampleData();
        } else {
            // 当前不是IDR帧，等数据量足够的时候再发送
            this.mp4TrackInfo.getSampleData().add(sampleData);
            if (this.mp4TrackInfo.getSampleData().size() >= 5) {
                this.addSampleData();
            }
        }
    }

    private void addSampleData() {
        Mp4SampleData first = this.mp4TrackInfo.getSampleData().get(0);
        this.addFMp4Data(new Mp4MoofBox(this.sequenceNumber, first.getTimestamp(), this.mp4TrackInfo));
        this.addFMp4Data(new Mp4MdatBox(this.mp4TrackInfo.totalSampleData()));
        // 更新mp4TrackInfo，用新的数据副本
        this.mp4TrackInfo = this.toMp4TrackInfo(this.trackInfo);
        this.sequenceNumber++;
    }

    /**
     * 数据转换，包装成Mp4需要的轨道信息
     *
     * @param track 轨道信息
     * @return Mp4TrackInfo
     */
    private Mp4TrackInfo toMp4TrackInfo(RtspTrackInfo track) {
        Mp4TrackInfo info = new Mp4TrackInfo();
        info.setId(track.getId());
        info.setType(track.getType());
        info.setCodec(track.getCodec());
        info.setTimescale(track.getTimescale());
        info.setDuration(track.getDuration());
        info.setWidth(track.getWidth());
        info.setHeight(track.getHeight());
        info.setSps(track.getSps());
        info.setPps(track.getPps());
        return info;
    }

    /**
     * 添加FMp4数据
     *
     * @param iObjectByteArray 数据
     */
    private void addFMp4Data(IObjectByteArray iObjectByteArray) {
        if (this.asyncSend) {
            this.buffers.offer(iObjectByteArray);
            synchronized (this.objLock) {
                this.objLock.notifyAll();
            }
        } else {
            if (this.fmp4DataHandle != null) {
                this.fmp4DataHandle.accept(iObjectByteArray.toByteArray());
            }
        }
    }

    /**
     * 事件执行
     */
    private void executeHandle() {
        log.debug("开启代理服务端发送FMp4字节数据的异步线程");
        while (!this.terminal) {
            // 没数据的时候等待
            while (this.buffers.isEmpty() && !this.terminal) {
                synchronized (this.objLock) {
                    try {
                        this.objLock.wait();
                    } catch (InterruptedException e) {
                        // NOOP
                    }
                }
            }
            // 有数据的时候发送出去
            int size = this.buffers.size();
            for (int i = 0; i < size; i++) {
                IObjectByteArray pop = this.buffers.pop();
                if (this.fmp4DataHandle != null && pop != null) {
                    try {
                        this.fmp4DataHandle.accept(pop.toByteArray());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
        log.debug("关闭代理服务端发送FMp4字节数据的异步线程");
    }

    /**
     * 开始
     *
     * @return 异步结果
     */
    public CompletableFuture<Void> start() {
        log.info("开启FMp4代理服务端，模式：[{}]", this.asyncSend ? "异步" : "同步");
        return this.client.start();
    }

    /**
     * 结束
     */
    public void stop() {
        if (this.asyncSend) {
            this.terminal = true;
            synchronized (this.objLock) {
                this.objLock.notifyAll();
            }
            if (this.future != null && !this.future.isDone()) {
                this.future.join();
            }
        }
        this.client.stop();
        log.info("关闭FMp4代理服务端");
    }
}
