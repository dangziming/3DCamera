package com.orbbec.keyguard;

import android.content.Context;
import android.view.SurfaceView;

import com.orbbec.R;
import com.orbbec.utils.AppDef;
import com.orbbec.utils.DrawUtil;
import com.orbbec.utils.GlobalDef;

import java.util.List;

import mobile.ReadFace.YMFace;

public class ObFacePresenter extends BaseFacePresenter {
    private static final String TAG = "ObFacePresenter";

    private SurfaceView mFaceTrackDrawView;
    private GlobalDef mDef = new AppDef();
    private int mVideoViewMarginLeft;
    private int mVideoViewMarginTop;
    private int mVideoViewWidth;
    private int mVideoViewHeitht;


    public ObFacePresenter(Context context) {
        super(context, context.getResources().getDimension(R.dimen.color_width), context.getResources().getDimension(R.dimen.color_height), new AppDef());
    }

    public void setFaceTrackDrawView(SurfaceView frameSurfaceView) {
        mFaceTrackDrawView = frameSurfaceView;
    }



    /**
     * 返回是否需要做活体验证
     * @param identifyPerson
     * @param nameFromPersonId
     * @param happy
     * @return
     */
    @Override
    public boolean needToCheckLiveness(int identifyPerson, String nameFromPersonId, int happy){
        return true;
    }



    @Override
    public void drawFaceTrack(List<YMFace> faces, boolean toFlip, float scaleBit, int videoWidth){

        if (needIdentificationFace && mFaceTrackDrawView != null) {
            DrawUtil.drawAnim(faces, mFaceTrackDrawView, mDataSource.isUVC(), scaleBit, mVideoViewMarginLeft, mVideoViewMarginTop, mVideoViewWidth, mDef.getColorWidth(),mContext);
        }
    }

    @Override
    public int faceOffsetPixel() {
        return 0;
    }

    /**
     * 视频view相对于画人脸框的位置
     * @param marginLeft
     * @param marginTop
     * @param viewWidth
     * @param viewHeight
     */
    public void setFaceTrackDrawViewMargin(int marginLeft, int marginTop, int viewWidth, int viewHeight){
        mVideoViewMarginLeft = marginLeft;
        mVideoViewMarginTop = marginTop;
        mVideoViewWidth = viewWidth;
        mVideoViewHeitht = viewHeight;
    }

}

