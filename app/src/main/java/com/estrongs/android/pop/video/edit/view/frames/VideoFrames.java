package com.estrongs.android.pop.video.edit.view.frames;

import java.util.ArrayList;
import java.util.Collections;

import com.estrongs.android.pop.video.util.AndroidUtil;

import android.graphics.Bitmap;
import android.media.MediaFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.annotation.UiThread;

public class VideoFrames {


    public interface Listener {
        void onNewFrame();
        void onFrameLoaded();
    }

    private ArrayList<Bitmap> thumbs;
    private ArrayList<Listener> listeners = new ArrayList<>();
    private int frameCount = 0;
    private boolean frameLoaded = false;
    private long duration;
    private MediaFormat format;

    public VideoFrames() {
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
        thumbs = new ArrayList<>(frameCount);
    }

    public ArrayList<Bitmap> getThumbs() {
        return thumbs == null ? (ArrayList<Bitmap>) Collections.EMPTY_LIST : thumbs;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public void addFrame(final Bitmap frame) {
        if (!AndroidUtil.isUIThread()) {
            AndroidUtil.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addFrame(frame);
                }
            });

            return;
        }

        thumbs.add(frame);

        for (Listener listener : listeners) {
            listener.onNewFrame();
        }
    }

    public void onFrameLoaded() {
        frameLoaded = true;
        for (Listener listener : listeners) {
            listener.onFrameLoaded();
        }
    }

    public boolean isFrameLoaded() {
        return frameLoaded;
    }

    public void setFrameLoaded(boolean frameLoaded) {
        this.frameLoaded = frameLoaded;
    }

    public long getDuration() {
        return duration;
    }

    public MediaFormat getFormat() {
        return format;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void setFormat(MediaFormat format) {
        this.format = format;
        this.duration = format.getLong(MediaFormat.KEY_DURATION);
    }

    @UiThread
    public void destroy() {
        for (Bitmap bm : thumbs)
            bm.recycle();

        thumbs.clear();
    }
}
