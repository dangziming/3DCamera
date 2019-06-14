package com.orbbec.base;

public interface DeviceCallback {
    /**
     * 未检测到设备
     */
    void onNoDevices();

    /**
     * 打开设备失败
     */
    void onDeviceOpenFailed();

    /**
     * 打开设备成功
     */
    void onDeviceOpened();
}
