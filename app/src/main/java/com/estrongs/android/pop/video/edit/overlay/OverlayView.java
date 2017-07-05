package com.estrongs.android.pop.video.edit.overlay;

import com.estrongs.android.pop.video.MyApplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.Log;

/**
 * Created by lipeng21 on 2017/6/30.
 */

public class OverlayView {
    private static final String TAG = OverlayView.class.getSimpleName();

    public static final int HIT_NONE = 1;
    public static final int HIT_REAL_VIEW = 2;
    public static final int HIT_DELETE = 3;
    public static final int HIT_TRANSFORM = 4;

    protected long startTime;
    protected long endTime ;

    protected int x;
    protected int y;

    protected Paint paint;
    protected Bitmap bmpDraw;
    protected Matrix matrix;

    protected Bitmap bmpDelete;
    protected Bitmap bmpTransform;

    protected Rect rectOutside;
    protected Rect rectInside;
    protected Rect rectDelete;
    protected Rect rectTransform;

    protected PointF pointMiddle;
    protected PointF pointDown;

    public OverlayView(int deleteBmpId, int transformBmpId){
        bmpDelete = createBmp(deleteBmpId);


        bmpTransform = createBmp(transformBmpId);

        matrix = new Matrix();
    }

    public void draw(Canvas pCanvas, long pTime){
        if(!isInTime(pTime)){
            return;
        }
        _draw(pCanvas);
        drawCorner(pCanvas);
    }

    protected void _draw(Canvas pCanvas){
        pCanvas.drawBitmap(bmpDraw, matrix, paint);
    }
    protected void drawCorner(Canvas pCanvas){

    }

    public int isHit(PointF pointF){
        pointDown = pointF;

        if(rectDelete.contains((int)pointF.x, (int)pointF.y)){
            Log.e(TAG, "点击了删除");
            return HIT_DELETE;
        }else if(rectTransform.contains((int)pointF.x, (int)pointF.y)){
            Log.e(TAG, "点击了旋转");
            return HIT_TRANSFORM;
        }else if(rectOutside.contains((int)pointF.x, (int)pointF.y)){
            Log.e(TAG, "点击了移动");
            return HIT_REAL_VIEW;
        }else {
            return HIT_NONE;
        }
    }

    /**
     * 响应ACTION_MOVE
     * @param hitStatus
     * @param pointF
     */
    public void move(int hitStatus, PointF pointF){
        switch (hitStatus){
            case HIT_DELETE:
                break;
            case HIT_TRANSFORM:

                break;
            case HIT_REAL_VIEW:

                break;
            case HIT_NONE:
                break;
            default:
                break;
        }
    }

    protected boolean isInTime(long pTime){
        return pTime >= startTime && pTime <= endTime;
    }


    // 得到两个点的距离
    protected float spacing(PointF p1, PointF p2) {
        float x = p1.x - p2.x;
        float y = p1.y - p2.y;
        return (float) Math.sqrt(x * x + y * y);
    }

    // 得到两个点的中点
    protected PointF midPoint(PointF p1, PointF p2) {
        PointF point = new PointF();
        float x = p1.x + p2.x;
        float y = p1.y + p2.y;
        point.set(x / 2, y / 2);
        return point;
    }
    // 旋转
    protected float rotation(PointF p1, PointF p2) {
        double delta_x = (p1.x - p2.x);
        double delta_y = (p1.y - p2.y);
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    private Bitmap createBmp(int resId){
        return BitmapFactory.decodeResource(MyApplication.mInstance.getResources(), resId);
    }

}
