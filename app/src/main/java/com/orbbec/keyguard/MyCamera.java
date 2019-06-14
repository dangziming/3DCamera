package com.orbbec.keyguard;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.orbbec.base.DeviceCallback;
import com.orbbec.utils.AppDef;
import com.orbbec.utils.GlobalDef;
import com.orbbec.utils.OpenNiHelper;
import com.orbbec.R;
import com.orbbec.view.GlFrameSurface;

import static android.os.Build.VERSION_CODES.M;

public class MyCamera extends Activity implements Runnable,DeviceCallback {
    private GlFrameSurface mGLSurface;
    private SurfaceView drawView;
    private ObFacePresenter mPresenter;
    private AppDef mAppDef = new AppDef();
    private ObDataSource mObDataSource;
    private Rect rect = new Rect();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_mycamera);
        initView();

        if (Build.VERSION.SDK_INT >= M) {
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) | ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA}, 0);
            } else if (checkPermission()) {
                openInit();
            }
        } else {
            openInit();
        }
    }

    private void initView() {
        mGLSurface = (GlFrameSurface) findViewById(R.id.gl_surface);
        mGLSurface.setFrameWidth(640);
        mGLSurface.setFrameHeight(480);

        drawView = (SurfaceView) findViewById(R.id.pointView);
        drawView.setZOrderOnTop(true);
        drawView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    private void openInit(){
        if (OpenNiHelper.hasObUsbDevice(getApplicationContext())) {
            mPresenter = new ObFacePresenter(this);
            //设备回调
            mPresenter.setDeviceCallback(this);
            mPresenter.startIdentification();
            mPresenter.setSurfaceView(mGLSurface);
            mPresenter.setFaceTrackDrawView(drawView);

            mObDataSource = new ObDataSource(this, mPresenter, mAppDef);
            //这个里面就会检测设备vid,pid
            mPresenter.setOBSource(mObDataSource);

            mPresenter.initFaceTrack();
            mPresenter.startFaceTrack();

        }
    }

    private boolean checkPermission() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onNoDevices() {

    }

    @Override
    public void onDeviceOpenFailed() {

    }

    @Override
    public void onDeviceOpened() {
        updateSurfaceView();
        mObDataSource.startDepth();
        mObDataSource.startColor();
    }

    private void updateSurfaceView() {

         mGLSurface.post(new Runnable() {
             @Override
             public void run() {
                 int w = mGLSurface.getWidth();
                 int h = mGLSurface.getHeight();
                 rect.set(0, 0, w, h);
                 Log.d("neostra","recognition w = " + w +"  h = " + h);
                 mPresenter.setIdentificationRect(rect);
                 mPresenter.setFaceTrackDrawViewMargin(mGLSurface.getLeft(), mGLSurface.getTop(), mGLSurface.getWidth(), mGLSurface.getHeight());
             }
            });

    }

    @Override
    public void run() {
        if (mObDataSource != null) {
            mObDataSource.stopDepth();
        }
    }
}
