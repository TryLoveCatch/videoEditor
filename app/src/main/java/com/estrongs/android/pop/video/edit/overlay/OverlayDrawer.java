package com.estrongs.android.pop.video.edit.overlay;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * 两个地方会调用
 *
 * 1、预览
 * 2、合成
 *
 * 所以需要传入Canvas，这样我只用画就可以
 * 不用关心是在那里绘制的
 *
 */
public interface OverlayDrawer {

    int DRAW_EMPTY   = 0;
    int DRAW_SAME    = 1;
    int DRAW_CHANGED = 2;

    int drawImage(long pts, Canvas canvas, Paint paint);
}
