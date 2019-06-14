package com.orbbec.keyguard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;

import com.lzh.easythread.EasyThread;
import com.orbbec.NativeNI.OrbbecUtils;
import com.orbbec.base.DeviceCallback;
import com.orbbec.base.FacePresenter;
import com.orbbec.base.ObSource;
import com.orbbec.base.OrbbecPresenter;
import com.orbbec.utils.GlobalDef;
import com.orbbec.view.GlFrameSurface;

import org.openni.SensorType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import mobile.ReadFace.YMFace;
import mobile.ReadFace.YMFaceTrack;

public abstract class BaseFacePresenter implements OrbbecPresenter,FacePresenter {

    public static final int LIVENESS_STATUS_CHECKETING = 0;
    public static final int LIVENESS_STATUS_CHECK_SUCCESS = 1;
    public static final int LIVENESS_STATUS_CHECK_FAIL = 2;

    private int mColorWidth;
    private int mColorHeight;
    private int mDepthWidth;
    private int mDepthHeight;
    protected Context mContext;
    private Rect mDstRect;
    private Rect mSrcRect;
    private float mColorViewWidth;
    private float mColorViewHeight;
    private Rect mDetectReferVideoSize;
    private EasyThread easyThread = null;
    protected boolean needIdentificationFace = false;
    protected ObSource mDataSource;
    private YMFaceTrack mYmFaceTrack;
    private boolean mFaceTrackExit = false;
    private volatile boolean isBufferready = false;
    private Object facesLock = new Object();
    private volatile boolean isFaceListUpdata = false;
    private List<YMFace> mYMFaceList;
    private Object colorBufferLock = new Object();
    private Object depthBufferLock = new Object();
    private ByteBuffer tempColorBuffer;
    private ByteBuffer tempDepthBuffer;
    private ByteBuffer mDepthBuffer;
    private ByteBuffer mColorBuffer;
    private int cropX = -1;
    private int cropY = -1;
    private int cropWidth = -1;
    private int cropHeitht = -1;
    private Rect mDetectReferColorView;
    private SparseArray<YMFace> faceArrays = new SparseArray<>();
    private List<YMFace> faceList = new ArrayList<>();
    protected boolean needRegisterFace = false;
    boolean isLiveness;
    private int livenessFailCount = 0;
    private int livenessCount = 0;
    private int tooFarOrCloseCount = 0;
    private int livenessStatus;
    private SurfaceView mSurfaceView;
    private int lastRecognitionFaceIndex = -1;

    private ByteBuffer tempI420ColorBuffer;
    private ByteBuffer temp422toNV21Buffer;
    private DeviceCallback mDeviceCallback;

    public BaseFacePresenter(Context context, float colorViewWidth, float colorViewHeight, GlobalDef def) {

        mColorWidth = def.getColorWidth();
        mColorHeight = def.getColorHeight();
        mDepthWidth = def.getDepthWidth();
        mDepthHeight = def.getDepthHeight();
        mContext = context;
        mDstRect = new Rect();
        mSrcRect = new Rect();
        mDetectReferVideoSize = new Rect();
        mColorViewWidth = colorViewWidth;
        mColorViewHeight = colorViewHeight;

        easyThread = EasyThread.Builder.createFixed(6).build();
    }

    @Override
    public void setDeviceCallback(DeviceCallback deviceCallback) {
        mDeviceCallback = deviceCallback;
    }

    @Override
    public void setOBSource(ObSource dataSource) {
        mDataSource = dataSource;
        if (mDataSource != null) {
            mDataSource.setSurfaceView(mSurfaceView);
            mDataSource.initDevice();
        }
    }

    @Override
    public void onNoDevice() {
        if (mDeviceCallback != null) {
            mDeviceCallback.onNoDevices();
        }
    }

    @Override
    public void onDeviceOpened() {
        if (mDeviceCallback != null) {
            mDeviceCallback.onDeviceOpened();
        }
    }

    @Override
    public void onDeviceOpenFailed() {
        if (mDeviceCallback != null) {
            mDeviceCallback.onDeviceOpenFailed();
        }
    }

    @Override
    public void onColorUpdate(byte[] data) {

    }

    @Override
    public void onColorUpdate(ByteBuffer data, int strideInBytes) {
        if (mDetectReferColorView == null) {
            return;
        }
        int width = getRenderFrameWidth();
        int height = getRenderFrameHeight();
        if (mColorBuffer == null) {
            /** TODO: 这里得到的getFaceTrackWidth其实还是视频宽度，因为  {@link this#setIdentificationRect(Rect)} 还没有被调用 */

            mColorBuffer = ByteBuffer.allocateDirect(getFaceTrackWidth() * getFaceTrackHeight() * 3 / 2);
            tempColorBuffer = ByteBuffer.allocateDirect(getFaceTrackWidth() * getFaceTrackHeight() * 3 / 2);

            tempI420ColorBuffer = ByteBuffer.allocateDirect(width * height * 3 / 2);
            temp422toNV21Buffer = ByteBuffer.allocateDirect(width * height * 3 / 2);
        }

        OrbbecUtils.CropYUY2toNV21AndI420(data, mColorWidth, mColorHeight, temp422toNV21Buffer, tempI420ColorBuffer, 0, 0, width, height);

        if (mSurfaceView instanceof GlFrameSurface) {
            ((GlFrameSurface) mSurfaceView).updateI420Frame(tempI420ColorBuffer);
        }

        synchronized (mDataSource) {
            if (isCrop()) {
                // 裁切YUV
                handlerImageClip(temp422toNV21Buffer.array(), tempColorBuffer);
            } else {
                tempColorBuffer = temp422toNV21Buffer;
            }
            isBufferready = true;
        }
        /*if (mDetectionCallback != null && mDetectionCallback.needColorBitmap()) {
            if (mColorBitmap == null) {
                mColorBitmap = Bitmap.createBitmap(getRenderFrameWidth(), getRenderFrameHeight(), Bitmap.Config.ARGB_8888);
            }
            OrbbecUtils.NV21ToRGB32(getRenderFrameWidth(), getRenderFrameHeight(), tempColorBuffer.array(), mColorBitmap, true);
            mDetectionCallback.onDrawColor(mColorBitmap);
        }*/
    }

    @Override
    public void onDepthUpdate(ByteBuffer data, int width, int height, SensorType sensorType, int strideInBytes) {
        if (mDetectReferColorView == null) {
            return;
        }

        if (mDepthBuffer == null) {
            /** TODO: 这里得到的getFaceTrackWidth其实还是视频宽度，因为  {@link this#setIdentificationRect(Rect)} 还没有被调用 */
            mDepthBuffer = ByteBuffer.allocateDirect(getFaceTrackWidth() * getFaceTrackHeight() * 2);
            tempDepthBuffer = ByteBuffer.allocateDirect(getFaceTrackWidth() * getFaceTrackHeight() * 2);
            //字节顺序。
            mDepthBuffer.order(ByteOrder.nativeOrder());
            tempDepthBuffer.order(ByteOrder.nativeOrder());
        }

        synchronized (depthBufferLock) {
            if (isCrop()) {
                //裁剪深度
                handlerDepthClip(data, tempDepthBuffer);
            } else {
                OrbbecUtils.ByteBufferCopy(data, tempDepthBuffer, width * height * 2);
            }
        }

    }


    /**
     * 裁切深度图
     *
     * @param srcData
     * @param dstBuffer
     */
    private void handlerDepthClip(ByteBuffer srcData, ByteBuffer dstBuffer) {
        int width = mDepthWidth;
        int height = mDepthHeight;
        OrbbecUtils.CropDepth(srcData, width * height * 2, width, height, dstBuffer, cropX, cropY, cropWidth, cropHeitht);
    }


    /**
     * 裁剪图片
     *
     * @param data
     */
    private void handlerImageClip(byte[] data, ByteBuffer dst) {
        OrbbecUtils.CropNV21(data, data.length, getRenderFrameWidth(), getRenderFrameHeight(), dst, cropX, cropY, cropWidth, cropHeitht);
    }

    @Override
    public void onFaceTrack() {
        if (mFaceTrackExit) {
            return;
        }
        if (!isBufferready) {
            return;
        }
        if (mYmFaceTrack == null) {
            return;
        }
        if (mDetectReferColorView == null) {
            return;
        }

        synchronized (facesLock) {

            /* FixMe: 这步很快，可以跟踪到人脸的位置、关键点、角度、trackid */
            mYMFaceList = mYmFaceTrack.trackMulti(tempColorBuffer.array(), getFaceTrackWidth(), getFaceTrackHeight());

            if (mYMFaceList != null && mYMFaceList.size() > 0) {

                /* 判断最大人脸是否变更，以清除人脸跟踪框的显示 */
                checkMaxFaceIndexIsChangeOrNot(mYMFaceList);
                isFaceListUpdata = true;
                /** 这里会唤醒沉睡的 {@linkplain this#faceTrackThread()} */
                facesLock.notifyAll();

            } else {
                /*  人脸丢失或者没有人脸 */
                livenessStatus = LIVENESS_STATUS_CHECKETING;
                livenessCount = 0;
                livenessFailCount = 0;
            }
        }

        float scanle = (float) 1.3;
        drawFaceTrack(mYMFaceList, (mDataSource.isUVC() ? true : false), scanle, mColorWidth);
    }



    /**
     * 判断最大人脸是否变更，以清除人脸跟踪框的显示
     *
     * @param ymFaces
     */
    private void checkMaxFaceIndexIsChangeOrNot(List<YMFace> ymFaces) {

        int faceIndex = -1;
        float maxFace = 0;
        for (int i = 0; i < ymFaces.size(); i++) {
            YMFace face = ymFaces.get(i);
            float faceSize = face.getRect()[2] * face.getRect()[3];
            if (faceSize > maxFace) {
                maxFace = faceSize;
                faceIndex = i;
            }
        }

        if (faceIndex >= 0 && faceIndex != lastRecognitionFaceIndex) {
            lastRecognitionFaceIndex = faceIndex;
            livenessCount = 0;
            livenessFailCount = 0;
            livenessStatus = LIVENESS_STATUS_CHECKETING;
        }
    }

    @Override
    public void setSurfaceView(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    /**
     * 返回是否需要做活体验证
     * 若返回false，则跳过活体验证直接回调 {@linkplain IdentifyCallback#onLiveness(boolean, int, String, int)}}
     *
     * @param identifyPerson
     * @param nameFromPersonId
     * @param happy
     * @return
     */
    public abstract boolean needToCheckLiveness(int identifyPerson, String nameFromPersonId, int happy);


    /**
     * 画人脸跟中框
     *
     * @param faces
     * @param scaleBit
     * @param videoWidth     每帧RGB原始数据源的宽，face的定位坐标是以此为依据的

     */
    public abstract void drawFaceTrack(List<YMFace> faces, boolean toFlip, float scaleBit, int videoWidth);


    @Override
    public void startIdentification() {
        needIdentificationFace = true;
    }


    /**
     * 检测人脸对边框的间隔，避免半张脸录入的情况
     * 也避免深度图不全判断为太远或者太近或非活体的情况
     *
     * @return
     */
    public abstract int faceOffsetPixel();


    /**
     * 识别范围的设置
     *
     * @param rect 这是以渲染视频的view的像素大小为基准的裁切
     */
    @Override
    public void setIdentificationRect(Rect rect) {
        mDetectReferColorView = rect;

        mDetectReferVideoSize.left = (int) (mDetectReferColorView.left * mColorWidth / mColorViewWidth);
        mDetectReferVideoSize.right = (int) (mDetectReferColorView.right * mColorWidth / mColorViewWidth);
        mDetectReferVideoSize.top = (int) (mDetectReferColorView.top * mColorHeight / mColorViewHeight);
        mDetectReferVideoSize.bottom = (int) (mDetectReferColorView.bottom * mColorHeight / mColorViewHeight);


        Rect r = mDetectReferColorView;
        r = mDetectReferVideoSize;

        cropX = mDetectReferVideoSize.left;
        cropY = mDetectReferVideoSize.top;
        cropWidth = mDetectReferVideoSize.right - mDetectReferVideoSize.left;
        cropHeitht = mDetectReferVideoSize.bottom - mDetectReferVideoSize.top;


        if (cropWidth >= mColorWidth) {
            cropX = -1;
            cropY = -1;
            cropWidth = -1;
            cropHeitht = -1;
        }
        if (cropWidth % GlobalDef.NUMBER_2 != 0) {
            cropWidth--;
        }
        if (cropHeitht % GlobalDef.NUMBER_2 != 0) {
            cropHeitht--;
        }
    }



    @Override
    public void initFaceTrack() {
        if (mYmFaceTrack == null) {
            mYmFaceTrack = new YMFaceTrack();
            mYmFaceTrack.initTrack(mContext, YMFaceTrack.FACE_0, YMFaceTrack.RESIZE_WIDTH_640);
            mYmFaceTrack.setRecognitionConfidence(75);
        }
    }


    @Override
    public void stopFaceTrack() {
        if (mFaceTrackExit) {
            return;
        }

        mFaceTrackExit = true;
        synchronized (facesLock) {
            facesLock.notifyAll();  // 让等待面部识别的等待线程跳过等待
        }

        try {
            easyThread.getExecutor().awaitTermination(GlobalDef.PRO_MIX_DISTANCE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        easyThread.getExecutor().shutdownNow();
        if (mYmFaceTrack != null) {
            mYmFaceTrack.onRelease();
        }
    }

    @Override
    public void startFaceTrack() {
        mFaceTrackExit = false;
        faceTrackThread();
    }


    private void faceTrackThread() {
        easyThread.execute(new Runnable() {
            @Override
            public void run() {
                while (!mFaceTrackExit) {
                    if (!isBufferready) {
                        sleepTime(20);
                        continue;
                    }
                    if (mFaceTrackExit) {
                        continue;
                    }
                    if (mYmFaceTrack == null) {
                        sleepTime(20);
                        continue;
                    }
                    if (mFaceTrackExit) {
                        continue;
                    }

                    ArrayList<YMFace> ymFaceList;
                    synchronized (facesLock) {

                        while (!isFaceListUpdata || mYMFaceList == null) {
                            try {
                                /** 这里沉睡，直到下一帧数据来到被 {@linkplain BaseFacePresenter#onFaceTrack()} 唤醒 */
                                facesLock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (mFaceTrackExit) {
                                break;
                            }
                        }
                        if (mFaceTrackExit) {
                            continue;
                        }
                        ymFaceList = new ArrayList<>(mYMFaceList);
                        isFaceListUpdata = false;
                    }

                    if (mYMFaceList != null) {
                        synchronized (depthBufferLock) {
                            if (tempDepthBuffer != null && mDepthBuffer != null) {
                                OrbbecUtils.ByteBufferCopy(tempDepthBuffer, mDepthBuffer, getFaceTrackWidth() * getFaceTrackHeight() * 2);
                            }
                        }
                        synchronized (colorBufferLock) {
                            OrbbecUtils.ByteBufferCopy(tempColorBuffer, mColorBuffer, getFaceTrackWidth() * getFaceTrackHeight() * 3 / 2);
                        }
                        identification(ymFaceList);
                    }

                }
            }
        });
    }


    /**
     * 人脸识别的方法
     *
     * @param faces
     */
    private void identification(List<YMFace> faces) {//dzm 人脸识别

        if (mDetectReferColorView == null) {
            return;
        }
        if (faceArrays.size() > 0) {
            faceArrays.clear();
        }
        if (faceList != null && !faceList.isEmpty() && faceList.size() > 0) {
            faceList.clear();
        }

        if (faces != null && !faces.isEmpty()) {
            int i = 0;
            for (YMFace face : faces) {
                int offSet = faceOffsetPixel();
                if (offSet > 0) {//true dzm
                    float[] rect = face.getRect();
                    mSrcRect.set((int) rect[0], (int) rect[1], (int) (rect[0] + rect[2]), (int) (rect[1] + rect[3]));
                    int cx = mSrcRect.centerX() + mDetectReferVideoSize.left;
                    int cy = mSrcRect.centerY() + mDetectReferVideoSize.top;
                    if (mDetectReferVideoSize.contains(cx, cy)) {
                        if (mDetectReferVideoSize.contains(cx - offSet, cy - offSet, cx + offSet, cy - offSet)) {
                            faceArrays.append(i, face);
                            faceList.add(face);
                        }
                    }
                } else {
                    float[] rect = face.getRect();
                    mSrcRect.set((int) rect[0], (int) rect[1], (int) (rect[0] + rect[2]), (int) (rect[1] + rect[3]));
                    faceArrays.append(i, face);
                }
                i++;
            }
            if (faceArrays.size() > 0) {
                if (faceArrays.size() == 1) {
                    if(!needRegisterFace){
                        recognitionFace(faceArrays.keyAt(0));
                    }
                } else {
                    Collections.sort(faceList, comparator);
                    int faceIndex = faceArrays.indexOfValue(faceList.get(0));
                    YMFace face = faceList.get(0);
                    mSrcRect.set((int) face.getRect()[0], (int) face.getRect()[1], (int) (face.getRect()[0] + face.getRect()[2]), (int) (face.getRect()[1] + face.getRect()[3]));
                    if(!needRegisterFace){
                        recognitionFace(faceIndex);
                    }
                }
            }
        }
    }


    /**
     * 最大人脸识别
     *
     * @param faceIndex
     */
    private void recognitionFace(int faceIndex) {


        int personId = mYmFaceTrack.identifyPerson(faceIndex);


        // 单纯为了判断活体检测的显示（人脸跟踪框）

        isLiveness = mYmFaceTrack.ObIsLiveness(mColorBuffer.array(), mDepthBuffer, faceIndex, getFaceTrackWidth(), getFaceTrackHeight());
        updateLivenessStatus(isLiveness);
    }


    /**
     * 判断活体检测的显示（人脸跟踪框）
     *
     * @param isLiveness
     */
    private void updateLivenessStatus(boolean isLiveness) {

        tooFarOrCloseCount = 0;
        if (!isLiveness) {
            livenessCount = 0;
            if (livenessFailCount < GlobalDef.NUMBER_3) {
                livenessFailCount++;
            } else {
                livenessStatus = LIVENESS_STATUS_CHECK_FAIL;
            }
        } else {
            livenessFailCount = 0;
            if (livenessCount < GlobalDef.NUMBER_3) {
                livenessCount++;
            } else {
                livenessStatus = LIVENESS_STATUS_CHECK_SUCCESS;
            }
        }
    }



    /**
     * 定义人脸比较的方法
     */
    private Comparator<YMFace> comparator = new Comparator<YMFace>() {
        @Override
        public int compare(YMFace o1, YMFace o2) {
            float[] rect = o1.getRect();
            float[] rect1 = o2.getRect();
            float area = rect[2] * rect[3];
            float area1 = rect1[2] * rect1[3];
            return (int) (area1 - area);
        }
    };

    private void sleepTime(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    private boolean isCrop() {
        return cropX >= 0;
    }

    private int getRenderFrameWidth() {

        return mDataSource.isDuoDuo() ? GlobalDef.RES_DUODUO_DEPTH_WIDTH : mColorWidth;
    }

    private int getRenderFrameHeight() {

        return mDataSource.isDuoDuo() ? GlobalDef.RES_DUODUO_DEPTH_HEIGHT : mColorHeight;
    }

    private int getFaceTrackWidth() {
        return isCrop() ? cropWidth : (mDataSource.isDuoDuo() ? GlobalDef.RES_DUODUO_DEPTH_WIDTH : mColorWidth);
    }

    private int getFaceTrackHeight() {
        return isCrop() ? cropHeitht : (mDataSource.isDuoDuo() ? GlobalDef.RES_DUODUO_DEPTH_HEIGHT : mColorHeight);
    }
}
