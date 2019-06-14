package com.orbbec.utils;

public class GlobalDef {
    /**
     * 默认RGB摄像头分辨率
     */
    protected static final int RES_DEFAULT_COLOR_WIDTH = 640;
    protected static final int RES_DEFAULT_COLOR_HEIGHT = 480;

    /**
     * 默认深度图分辨率
     */
    protected static final int RES_DEFAULT_DEPTH_WIDTH = 640;
    protected static final int RES_DEFAULT_DEPTH_HEIGHT = 480;

    public static final int RES_DUODUO_DEPTH_WIDTH = 640;
    public static final int RES_DUODUO_DEPTH_HEIGHT = 400;

    public int getColorWidth(){
        return RES_DEFAULT_COLOR_WIDTH;
    }

    public int getColorHeight(){
        return RES_DEFAULT_COLOR_HEIGHT;
    }

    public int getDepthWidth(){
        return RES_DEFAULT_DEPTH_WIDTH;
    }

    public int getDepthHeight(){
        return RES_DEFAULT_DEPTH_HEIGHT;
    }

    public static final int GLOBAL_STREAM_TIMEOUT = 2000;
    public static final int NUMBER_2 = 2;
    public static final int NUMBER_3 = 3;
    public static final int PRO_MIX_DISTANCE = 500;

}
