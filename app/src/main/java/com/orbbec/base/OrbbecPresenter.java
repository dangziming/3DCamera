package com.orbbec.base;

import android.view.SurfaceView;

import org.openni.SensorType;

import java.nio.ByteBuffer;

public interface OrbbecPresenter {
    /**
     * 设置启动回调
     *
     * @param deviceCallback
     */
    void setDeviceCallback(DeviceCallback deviceCallback);

    /**
     * 设置数据源
     *
     * @param dataSource
     */
    void setOBSource(ObSource dataSource);



    /**
     * 未检测到设备
     */
    void onNoDevice();

    /**
     * 设备已打开
     */
    void onDeviceOpened();

    /**
     * 设备打开失败
     */
    void onDeviceOpenFailed();

    /**
     * uvc 彩色数据回调
     *
     * @param data nv21
     */
    void onColorUpdate(byte[] data);

    /**
     * astra color 彩色数据回调
     *
     * @param data          rgb888
     * @param strideInBytes
     */
    void onColorUpdate(ByteBuffer data, int strideInBytes);

    /**
     * 深度数据回调
     *
     * @param data
     * @param width
     * @param height
     * @param sensorType
     * @param strideInBytes
     */
    void onDepthUpdate(ByteBuffer data, int width, int height, SensorType sensorType, int strideInBytes);

    /**
     * 启动人脸跟踪，这一步会在 onColorUpdate(...) 和 onDepthUpdate(...) 调用后才调用，可用于人脸跟踪的需求
     */
    void onFaceTrack();

    /**
     * android 相机框架回调使用
     *
     * @param surfaceView
     */
    void setSurfaceView(SurfaceView surfaceView);
}
