package com.orbbec.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.SurfaceView;
import android.view.View;

import java.util.List;

import dou.utils.DisplayUtil;
import mobile.ReadFace.YMFace;

import static com.orbbec.keyguard.BaseFacePresenter.LIVENESS_STATUS_CHECK_FAIL;
import static com.orbbec.keyguard.BaseFacePresenter.LIVENESS_STATUS_CHECK_SUCCESS;

public class DrawUtil {

    private static boolean isClearDrawed = false;
    private static FaceRect[] faceRectArray = new FaceRect[10];
    private static int strokeWidth;

    /**
     * @param faces
     * @param renderView
     * @param scaleBit     画人脸框的缩放值
     * @param marginLeft   视频view相对于父view的左边距
     * @param marginTop    视频view相对于父view的顶边距
     * @param viewWidth    渲染RGB视频数据的view的跨度
     * @param videoWidth   每帧RGB原始数据源的宽，face的定位坐标是以此为依据的
     */
    public static void drawAnim(List<YMFace> faces, View renderView, boolean toFlip, float scaleBit,
                                int marginLeft, int marginTop, int viewWidth, int videoWidth, Context mContext) {

        // 当人脸数量为0时清除绘图（仅清一次）
        if (faces == null || faces.size() <= 0) {
            if (!isClearDrawed) {
                Canvas canvas = ((SurfaceView) renderView).getHolder().lockCanvas();
                if (canvas != null) {
                    try {
                        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        ((SurfaceView) renderView).getHolder().unlockCanvasAndPost(canvas);
                    }
                }
            }
            isClearDrawed = true;
            return;
        }
        isClearDrawed = false;

        if (faces.size() > faceRectArray.length) {
            faceRectArray = new FaceRect[faces.size()];
        }

        Paint paint = new Paint();
        Canvas canvas = ((SurfaceView) renderView).getHolder().lockCanvas();
        if (canvas != null) {
            try {

                int viewH = renderView.getHeight();
                int viewW = renderView.getWidth();
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                if (faces == null || faces.size() == 0) {
                    return;
                }
                int maxIndex = 0;

                float maxFaceValue = 0;
                for (int i = 0; i < faces.size(); i++) {
                    if (faces.get(i).getRect()[2] > maxFaceValue) {
                        maxFaceValue = faces.get(i).getRect()[2];
                        maxIndex = i;
                    }
                }

                if (strokeWidth <= 0) {
                    strokeWidth = DisplayUtil.dip2px(mContext, 3);
                }
                for (int i = 0; i < faces.size(); i++) {

                    YMFace ymFace = faces.get(i);
                    paint.setARGB(255, 4, 195, 124);
                    paint.setStrokeWidth(strokeWidth);
                    paint.setStyle(Paint.Style.STROKE);
                    float[] rect = ymFace.getRect();
                    float faceDX = (float) (rect[0] - (scaleBit - 1) * rect[2] / 2.0);
                    float faceDY = (float) (rect[1] - (scaleBit - 1) * rect[3] / 2.0);
                    float faceDWidth = rect[2] * scaleBit;
                    float faceDHeight = rect[3] * scaleBit;

                    float rectWidth = faceDWidth * viewWidth / videoWidth;
                    float rectHeight = faceDHeight * viewWidth / videoWidth;

                    //画外部的框
                    float rectX = marginLeft + faceDX * viewWidth / videoWidth;
                    if (toFlip) {
                        float dx1 = videoWidth - faceDX - faceDWidth;
                        rectX = marginLeft + dx1 * viewWidth / videoWidth;
                    }
                    float rectY = marginTop + faceDY * viewWidth / videoWidth;

                    faceRectArray[i] = new FaceRect(rectX, rectY, rectWidth, rectHeight);

                    //  网格外 图框
                    RectF rectf = new RectF(rectX, rectY, rectX + rectWidth, rectY + rectWidth);
                    canvas.drawRect(rectf, paint);
                    //  draw grid
                    int line = 10;
                    int smailSize = DisplayUtil.dip2px(mContext, 1.5f);
                    paint.setStrokeWidth(smailSize);


                    paint.setStrokeWidth(strokeWidth);
                    paint.setColor(Color.WHITE);
                    //                    注意前置后置摄像头问题

                    float length = faceDHeight / 5;
                    float width = rectWidth;
                    float heng = strokeWidth / 2;
                    canvas.drawLine(rectX - heng, rectY, rectX + length, rectY, paint);
                    canvas.drawLine(rectX, rectY - heng, rectX, rectY + length, paint);

                    rectX = rectX + width;
                    canvas.drawLine(rectX + heng, rectY, rectX - length, rectY, paint);
                    canvas.drawLine(rectX, rectY - heng, rectX, rectY + length, paint);

                    rectY = rectY + width;
                    canvas.drawLine(rectX + heng, rectY, rectX - length, rectY, paint);
                    canvas.drawLine(rectX, rectY + heng, rectX, rectY - length, paint);

                    rectX = rectX - width;
                    canvas.drawLine(rectX - heng, rectY, rectX + length, rectY, paint);
                    canvas.drawLine(rectX, rectY + heng, rectX, rectY - length, paint);

                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                ((SurfaceView) renderView).getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }


    static class FaceRect {
        float x;
        float y;
        float width;
        float height;

        public FaceRect(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
