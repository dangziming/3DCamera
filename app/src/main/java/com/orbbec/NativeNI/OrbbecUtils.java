package com.orbbec.NativeNI;

import java.nio.ByteBuffer;

public class OrbbecUtils {
    static {
        System.loadLibrary("yuv");
        System.loadLibrary("OrbbecUtils");
    }
    public native static int getVersion();
    public native static int ByteBufferCopy(ByteBuffer src, ByteBuffer dst, int length);
    public native static int CropDepth(ByteBuffer depthSrcData, int dataLenght, int width, int height, ByteBuffer depthDstBuffer, int cropX, int cropY, int dstWidth, int dstHeight);
    public native static int CropYUY2toNV21AndI420(ByteBuffer yuyvSrcBuffer, int width, int height, ByteBuffer dsNv21tBuffer, ByteBuffer dstI420Buffer, int cropX, int cropY, int dstWidth, int dstHeight);
    public native static int CropNV21(byte[] nv21SrcData, int dataLenght, int width, int height, ByteBuffer nv21DstBuffer, int cropX, int cropY, int dstWidth, int dstHeight);
}
