package com.estrongs.android.pop.video.edit.view.frames;

import java.util.ArrayList;

import com.estrongs.android.pop.video.util.DensityUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class VideoFramesView extends View {

    public interface Listener {

        void onSeekPositionChange(long pos);
        void onSeekTo(long pos);
    }

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final static int HANDLE_NONE = -1;
    private final static int HANDLE_LEFT = 0;
    private final static int HANDLE_RIGHT = 1;

    private final static int MOTION_NONE = 0;
    private final static int MOTION_DRAG_LEFT_HANDLE = 1;
    private final static int MOTION_DRAG_RIGHT_HANDLE = 2;

    public final static int SHADOW_INSIDE = 0;
    public final static int SHADOW_OUTSIDE = 1;

    private int durationTextColor = 0xFF777777;

    private int outLineColor = 0xffff7043;
    private int outLineWidth = DensityUtil.dip2px(3);
    private int handleWidth = DensityUtil.dp6;
    private int handleTouchWidth = DensityUtil.dp20;
    private int padding = DensityUtil.dp20;
    private int titleSize = DensityUtil.dp12;

    private int framePannelHeight = DensityUtil.dip2px(40);
    private int titlePannelHeight = DensityUtil.dip2px(30);
    private int viewHeight = framePannelHeight + titlePannelHeight + DensityUtil.dp3;
    private int framePannelTop = titlePannelHeight;
    private int framePannelBottom = framePannelHeight + titlePannelHeight;

    private int currentHandle = HANDLE_LEFT;

    private int leftHendlePos = 0;
    private int rightHendlePos = -1;

    private int thumbRectLeft  = 0;
    private int thumbRectRight = 0;
    private int thumbRectWidth = 0;

    private VideoFrames videoFrames;
    private Rect srcRect = new Rect();
    private Rect destRect = new Rect();

    private int motionStatus = MOTION_NONE;

    private int thumbWidth = 0;
    private int shadowDir = SHADOW_OUTSIDE;
    private int shadowColor = 0xAA000000;

    private ArrayList<Listener> listeners = new ArrayList<>();

    private float lastX, lastY;

    private VideoFrames.Listener frameListener = new VideoFrames.Listener() {
        @Override
        public void onNewFrame() {
            postInvalidate();
        }

        @Override
        public void onFrameLoaded() {
            postInvalidate();
        }
    };

    public VideoFramesView(Context context) {
        super(context);
    }

    public VideoFramesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoFramesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VideoFramesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @UiThread
    public void setVideoFrames(VideoFrames videoFrames) {
        if (videoFrames != null) {
            this.videoFrames = videoFrames;
            this.videoFrames.addListener(frameListener);
            postInvalidate();
        } else {
            if (this.videoFrames != null)
                this.videoFrames.removeListener(frameListener);
            this.videoFrames = null;
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), viewHeight);

        if (thumbWidth == 0) {

            int width = getMeasuredWidth();

            thumbRectLeft = padding + handleWidth;
            thumbRectRight = width - handleWidth - padding;
            thumbWidth = (thumbRectRight - thumbRectLeft) / videoFrames.getFrameCount();
            thumbRectWidth = thumbRectRight - thumbRectLeft;

            leftHendlePos = thumbRectLeft;
            rightHendlePos = thumbRectRight;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawFrames(canvas);
        drawShadow(canvas);
        drawOutline(canvas);
        drawTitle(canvas);
    }

    /***
     * 1、绘制开始时间
     * 2、绘制结束时间
     * 3、绘制持续时间
     * @param canvas
     */
    private void drawTitle(Canvas canvas) {
        int y = titlePannelHeight - DensityUtil.dp5 - (titleSize >> 1);

        int color = paint.getColor();

        paint.setTextSize(titleSize);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(outLineColor);
        paint.setUnderlineText(true);

        canvas.drawText(getStartText(), thumbRectLeft, y, paint);
        canvas.drawText(getEndText(), thumbRectRight, y, paint);

        int x = (thumbRectLeft + thumbRectRight) >> 1;
        paint.setColor(durationTextColor);
        paint.setUnderlineText(false);
        canvas.drawText(getDurationText(), x, y, paint);

        paint.setColor(color);
    }

    /**
     * 拖动handle之后 绘制不可用的视频阴影
     * @param canvas
     */
    private void drawShadow(Canvas canvas) {

        int color = paint.getColor();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(shadowColor);

        if (shadowDir == SHADOW_OUTSIDE) {
            if (leftHendlePos > thumbRectLeft) {
                canvas.drawRect(thumbRectLeft, framePannelTop, leftHendlePos, framePannelBottom, paint);
            }

            if (rightHendlePos < thumbRectRight) {
                canvas.drawRect(rightHendlePos, framePannelTop, thumbRectRight, framePannelBottom, paint);
            }
        } else {
            canvas.drawRect(leftHendlePos, framePannelTop, rightHendlePos, framePannelBottom, paint);
        }

        paint.setColor(color);
    }

    /**
     * 绘制每一帧数据
     * @param canvas
     */
    private void drawFrames(Canvas canvas) {
        if (videoFrames != null) {
            resetDestRect();
            int i = 0;
            for (Bitmap thumb : videoFrames.getThumbs()) {

                i++;

                getBitmapRect(thumb, srcRect);
                canvas.drawBitmap(thumb, srcRect, destRect, paint);

                moveDestRect();
            }
        }
    }

    /**
     *
     * 1、绘制外边框
     * 2、绘制两个handle
     * @param canvas
     */
    private void drawOutline(Canvas canvas) {

        int color = paint.getColor();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(outLineWidth);
        paint.setColor(outLineColor);
        canvas.drawRect(leftHendlePos, framePannelTop, rightHendlePos, framePannelBottom, paint);

        //draw handles
        drawHandle(canvas, leftHendlePos,  currentHandle == HANDLE_LEFT);
        drawHandle(canvas, rightHendlePos, currentHandle == HANDLE_RIGHT);

        paint.setColor(color);
    }

    private void drawHandle(Canvas canvas, int pos, boolean select) {

        paint.setColor(outLineColor);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeWidth(outLineWidth);

        int left = pos - handleWidth;
        int right = pos + handleWidth;
        int top = framePannelTop;
        int bottom = framePannelBottom;

        canvas.drawRect(left, top, right, bottom, paint);

        if (!select) {
            paint.setColor(0xFFFFFFFF);
            int t = DensityUtil.dp2;
            canvas.drawRect(left + t, top + t, right - t, bottom - t, paint);
        }

        int color = select ? 0xFFFFFFFF : 0xFF999999;
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        int y = (framePannelTop + framePannelBottom) >> 1;
        int h = DensityUtil.dp3;
        int w = 1;
        canvas.drawLine(pos - w, y - h, pos - w, y + h, paint);
        canvas.drawLine(pos + w, y - h, pos + w, y + h, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX = x;
                lastY = y;

                int handle = hitHandle(x, y);
                if (handle == HANDLE_LEFT) {
                    motionStatus = MOTION_DRAG_LEFT_HANDLE;
                    currentHandle = HANDLE_LEFT;
                    postInvalidate();
                    return true;
                } else if (handle == HANDLE_RIGHT) {
                    motionStatus = MOTION_DRAG_RIGHT_HANDLE;
                    currentHandle = HANDLE_RIGHT;
                    postInvalidate();
                    return true;
                } else {
                    motionStatus = MOTION_NONE;
                }

                break;
            case MotionEvent.ACTION_MOVE:

                float dx = x - lastX;
                float dy = y - lastY;

                lastX = x;
                lastY = y;

                if (motionStatus == MOTION_DRAG_LEFT_HANDLE || motionStatus == MOTION_DRAG_RIGHT_HANDLE) {
                    dragHandle(motionStatus, dx, dy);

                    for(Listener listener : listeners) {
                        if (motionStatus == MOTION_DRAG_LEFT_HANDLE) {
                            listener.onSeekPositionChange(getTimeByPosition(leftHendlePos));
                        } else if (motionStatus == MOTION_DRAG_RIGHT_HANDLE) {
                            listener.onSeekPositionChange(getTimeByPosition(rightHendlePos));
                        }
                    }

                    return true;
                }

                break;
            case MotionEvent.ACTION_UP:

                if (motionStatus == MOTION_DRAG_LEFT_HANDLE) {
                    for(Listener listener : listeners)
                        listener.onSeekTo(getTimeByPosition(leftHendlePos));
                } else if (motionStatus == MOTION_DRAG_RIGHT_HANDLE) {
                    for(Listener listener : listeners)
                        listener.onSeekTo(getTimeByPosition(rightHendlePos));
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    private void dragHandle(int motionStatus, float dx, float dy) {
        if (motionStatus == MOTION_DRAG_LEFT_HANDLE) {
            float x = leftHendlePos + dx;
            if (x < thumbRectLeft || x >= rightHendlePos)
                return;

            leftHendlePos = (int) x;
            postInvalidate();
        } else if (motionStatus == MOTION_DRAG_RIGHT_HANDLE) {

            float x = rightHendlePos + dx;
            if (x > thumbRectRight || x <= leftHendlePos)
                return;

            rightHendlePos = (int) x;
            postInvalidate();
        }
    }

    private int hitHandle(float x, float y) {

        if (x > leftHendlePos - handleTouchWidth && x < leftHendlePos + handleTouchWidth) {
            return HANDLE_LEFT;
        }

        if (x > rightHendlePos - handleTouchWidth && x < rightHendlePos + handleTouchWidth) {
            return HANDLE_RIGHT;
        }

        return HANDLE_NONE;
    }

    /////////////////
    // GLHelper functions
    private final void getBitmapRect(Bitmap bm, Rect rect) {
        rect.left = 0;
        rect.right = bm.getWidth();
        rect.top = 0;
        rect.bottom = bm.getHeight();
    }

    private final void moveDestRect() {
        destRect.left += thumbWidth;
        destRect.right += thumbWidth;
    }

    private final void resetDestRect() {
        destRect.left = thumbRectLeft;
        destRect.top = framePannelTop;
        destRect.right = destRect.left + thumbWidth;
        destRect.bottom = framePannelBottom;
    }

    /**
     * times
     */
    private static final int MIN = 60 * 1000000;
    private StringBuilder builder = new StringBuilder();
    private String getTimeText(long time) {
        long min = time / MIN;
        long t = time - min * MIN;
        long sec = t / 1000000;
        long ms = (t % 1000000) / 100000;

        builder.setLength(0);
        builder.append(min).append(":");

        if (sec < 10)
            builder.append("0");
        builder.append(sec).append(":");

        if (ms < 10)
            builder.append("0");

        builder.append(ms);

        return builder.toString();
    }

    /**
     * 根据handle的位置和时长
     *
     * 计算当前视频播放的时间位置
     *
     * 跟getStartTime 没什么区别啊
     * @param positon
     * @return
     */
    private long getTimeByPosition(int positon) {
        float r = (positon - thumbRectLeft) / (float)thumbRectWidth;
        long time = (long)(videoFrames.getDuration() * r);
        return time;
    }

    private String getStartText() {
        return getTimeText(getStartTime());
    }

    private String getEndText() {
        return getTimeText(getEndTime());
    }

    private String getDurationText() {
        float r = (float)(rightHendlePos - leftHendlePos) / (float)thumbRectWidth;
        long duration = (long)(r * videoFrames.getDuration());

        long min = duration / MIN;
        long t = duration - min * MIN;
        long sec = t / 1000000;
        long ms = (t % 1000000) / 100000;
        if (ms > 5)
            sec++;

        builder.setLength(0);
        if (min == 0)
            builder.append(sec).append("s");
        else
            builder.append(min).append("m").append(sec).append("s");

        return builder.toString();
    }

    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.listeners.remove(listener);
    }

    public void reset() {
        thumbWidth = 0;
        leftHendlePos = 0;
        rightHendlePos = -1;
        requestLayout();
    }

    public void setShadowDir(int dir) {
        this.shadowDir = dir;
    }

    /**
     * 根据视频长度和当前区域来计算开始时间
     * @return
     */
    public long getStartTime() {
        float r = (leftHendlePos - thumbRectLeft) / (float)thumbRectWidth;
        long start = (long)(videoFrames.getDuration() * r);
        return start;
    }
    /**
     * 根据视频长度和当前区域来计算结束时间
     * @return
     */
    public long getEndTime() {
        float r = (float)(thumbRectRight - rightHendlePos) / (float)thumbRectWidth;
        long end = (long) ((1 - r) * videoFrames.getDuration());
        return end;
    }

    public void setTimeRange(long start, long end) {
        float r = start / (float) videoFrames.getDuration();
        leftHendlePos = (int)(thumbRectWidth * r) + thumbRectLeft;

        r = (videoFrames.getDuration() - end ) / (float) videoFrames.getDuration();
        rightHendlePos = (int)(thumbRectRight - r * (float)thumbRectWidth);

        postInvalidate();
    }
}
