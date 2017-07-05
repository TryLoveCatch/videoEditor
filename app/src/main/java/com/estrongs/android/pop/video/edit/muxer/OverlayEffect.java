package com.estrongs.android.pop.video.edit.muxer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.LinkedList;

import com.estrongs.android.pop.video.edit.overlay.OverlayDrawer;
import com.estrongs.android.pop.video.opengl.GLHelper;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class OverlayEffect {

    private static final String TAG = "OverlayEffect";

    private Bitmap drawBitmap;
    private float drawScale = 1;
    private int rotation = 0;

    private LinkedList<OverlayDrawer> drawers = new LinkedList<>();
    private Canvas canvas;
    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int OFFSET_0   = 0;
    private static final int OFFSET_90  = 2;
    private static final int OFFSET_180 = 4;
    private static final int OFFSET_270 = 6;

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int OVERLAY_COORD_VERTICES_DATA_STRIDE_BYTES = 8 * FLOAT_SIZE_BYTES;

    private final float[] overlayCoordDatas = {
            1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f
    };

    private FloatBuffer overlayCoordVects;

    public void addDrawer(OverlayDrawer drawer) {
        drawers.add(drawer);
    }

    public void setOverlayDrawScale(float scale) {
        this.drawScale = scale;
    }

    public void onSurfaceCreated(int width, int height, int rotation) {
        this.rotation = rotation + 180;
        if (this.rotation > 360)
            this.rotation -= 360;

        if (rotation == 90 || rotation == 180)
            drawBitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888);
        else
            drawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        canvas = new Canvas(drawBitmap);

        overlayCoordVects = ByteBuffer.allocateDirect(overlayCoordDatas.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        overlayCoordVects.put(overlayCoordDatas).position(0);
    }

    public void applayOverlay(long pts,
                              int textureId,
                              int overlayTextureHandle,
                              int flagHandle,
                              int overlyMatrixHandle) {

        drawBitmap.eraseColor(Color.argb(0, 0, 0, 0));

        canvas.setBitmap(drawBitmap);

        canvas.save();

        if (drawScale != 1) {
            canvas.scale(drawScale, drawScale);
        }

        int status = OverlayDrawer.DRAW_EMPTY;
        for (OverlayDrawer drawer : drawers) {
            int t = drawer.drawImage(pts, canvas, paint);
            if (t > status)
                status = t;
        }

        canvas.restore();

        updateTexture(status, textureId, overlayTextureHandle, flagHandle, overlyMatrixHandle);
    }

    private void updateTexture(int status,
                               int textureId,
                               int overlayTextureHandle,
                               int flagHandle,
                               int overlayCoordHandle
    ) {
        if (status == OverlayDrawer.DRAW_EMPTY) {
            GLES20.glUniform1i(flagHandle, 0);
            GLES20.glUniform1i(overlayTextureHandle, 1);
        } else {

            GLES20.glUniform1i(flagHandle, 1);

            int offset = 0;
            if (rotation == 0)
                offset = OFFSET_0;
            else if (rotation == 90)
                offset = OFFSET_90;
            else if (rotation == 180)
                offset = OFFSET_180;
            else if (rotation == 270)
                offset = OFFSET_270;

            overlayCoordVects.position(offset);
            GLES20.glVertexAttribPointer(overlayCoordHandle,
                    2,
                    GLES20.GL_FLOAT,
                    false,
                    OVERLAY_COORD_VERTICES_DATA_STRIDE_BYTES,
                    overlayCoordVects);
            GLHelper.checkGlError(TAG, "glVertexAttribPointer aOverlayCoord");
            GLES20.glEnableVertexAttribArray(overlayCoordHandle);
            GLHelper.checkGlError(TAG, "glEnableVertexAttribArray aOverlayCoord");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLHelper.checkGlError(TAG, "glBindTexture");

            if (status == OverlayDrawer.DRAW_CHANGED) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, drawBitmap, 0);
                GLHelper.checkGlError(TAG, "texImage2d");
            }

            GLES20.glUniform1i(overlayTextureHandle, 1);
            GLHelper.checkGlError(TAG, "oTextureHandle - glUniform1i");
        }
    }

}
