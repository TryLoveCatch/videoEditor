package com.estrongs.android.pop.video.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.WorkerThread;

public class BitmapUtil {

    @WorkerThread
    public static Bitmap extractThumbnail(Bitmap bitmap, final int width, final int height) {

        Bitmap bmp = null;
        try {

            if (bitmap == null || bitmap.isRecycled()) {
                return null;
            }

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Rect src = new Rect();
            Rect dst = new Rect(0, 0, width, height);

            int bWidth = bitmap.getWidth();
            int bHeight = bitmap.getHeight();

            float sX = width / (float) bWidth;
            float sY = height / (float) bHeight;

            float x0 = bWidth / 2f;
            float y0 = bHeight / 2f;

            float s = sX > sY ? sX : sY;

            float tmpW = (width / 2f) / s;
            float tmpH = (height /2f) / s;

            src.left = (int) (x0 - tmpW);
            src.right = (int) (x0 + tmpW);
            src.top = (int) (y0 - tmpH);
            src.bottom = (int) (y0 + tmpH);

            bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bmp.setHasAlpha(true);
            bmp.eraseColor(Color.argb(0, 0, 0, 0));

            Canvas canvas = new Canvas(bmp);
            canvas.drawBitmap(bitmap, src, dst, paint);
            canvas.setBitmap(null);

        } catch (Exception | OutOfMemoryError e) {
            e.printStackTrace();
        }

        return bmp;
    }
}
