package com.orbbec.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import java.nio.ByteBuffer;

public class GlFrameSurface extends GLSurfaceView {
    private CmGlRenderer mGLFRenderer;
    private int mFrameWidth;
    private int mFrameHeight;

    public GlFrameSurface(Context context) {
        super(context);
        init(context);
    }

    public GlFrameSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        super.setEGLContextClientVersion(2);

        mGLFRenderer = new CmGlRenderer(this);
        setRenderer(mGLFRenderer);
    }

    public void setFrameWidth(int width){
        mFrameWidth = width;
    }

    public void setFrameHeight(int height){
        mFrameHeight = height;
    }

    public int getFrameWidth(){
        return mFrameWidth;
    }

    public int getFrameHeight(){
        return mFrameHeight;
    }

    public void updateI420Frame(ByteBuffer i420Buffer){//true dzm
        if (mGLFRenderer != null){
            mGLFRenderer.drawI420Frame(i420Buffer);
            requestRender();
        }
    }
}
