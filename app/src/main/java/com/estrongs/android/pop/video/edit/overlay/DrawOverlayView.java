package com.estrongs.android.pop.video.edit.overlay;

import java.util.List;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by lipeng21 on 2017/6/30.
 */

public class DrawOverlayView extends View{

    private List<OverlayView> mArrViews;

    public DrawOverlayView(Context context) {
        super(context);
    }

    public DrawOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DrawOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }




}
