package com.orbbec.base;

import android.graphics.Rect;

public interface FacePresenter {


    /**
     * 人脸框位置
     *
     * @param rect  识别范围的裁切，这是以渲染视频的view的像素大小为基准的裁切
     */
    void setIdentificationRect(Rect rect);

    /**
     * 初始化人脸检测
     */
    void initFaceTrack();

    /**
     * 开始人脸检测
     */
    void startFaceTrack();

    /**
     * 结束人脸检测
     */
    void stopFaceTrack();

    /**
     * 启动人脸识别检测
     */
    void startIdentification();


}
