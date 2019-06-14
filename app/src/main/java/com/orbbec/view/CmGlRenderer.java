package com.orbbec.view;

import android.opengl.GLSurfaceView;
import com.orbbec.NativeNI.CmOpenGLES;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CmGlRenderer implements GLSurfaceView.Renderer{
    private GlFrameSurface mSurface;
    private ByteBuffer i420Buffer;

    public CmGlRenderer(GlFrameSurface surface){
        super();
        this.mSurface = surface;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        CmOpenGLES.init(mSurface.getFrameWidth(), mSurface.getFrameHeight());
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        CmOpenGLES.changeLayout(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        if (i420Buffer != null){
            CmOpenGLES.drawI420Frame(i420Buffer, mSurface.getFrameWidth(), mSurface.getFrameHeight());
        }
    }

    public void drawI420Frame(ByteBuffer i420Buffer){
        this.i420Buffer = i420Buffer;
    }

    public void release(){
        CmOpenGLES.release();
    }
}
