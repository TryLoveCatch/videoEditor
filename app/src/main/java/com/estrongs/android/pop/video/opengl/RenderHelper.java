package com.estrongs.android.pop.video.opengl;

import com.estrongs.android.pop.video.edit.muxer.TextureRender;
import com.estrongs.android.pop.video.edit.overlay.OverlayDrawer;
import com.estrongs.android.pop.video.filter.VideoFilterProvider;

import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.Surface;

/**
 * Created by lipeng21 on 2017/6/28.
 */

public class RenderHelper implements SurfaceTexture.OnFrameAvailableListener{
    private static final String TAG = RenderHelper.class.getSimpleName();


    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private TextureRender mTextureRender = new TextureRender();

    private volatile boolean mFrameAvailable;
    private Object mLock = new Object();

    public void init(int width, int height, int rotation) {
        mTextureRender.surfaceCreated(width, height, rotation);
        Log.d(TAG, "textureID=" + mTextureRender.getTextureId());
        mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());
        mSurfaceTexture.setOnFrameAvailableListener(this);
        mSurface = new Surface(mSurfaceTexture);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "new frame available");

        synchronized (mLock) {

            if (mFrameAvailable) {
                throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
            }

            mFrameAvailable = true;
            mLock.notifyAll();
        }
    }

    public void awaitNewImage() {
        final int TIMEOUT_MS = 500;

        synchronized (mLock) {
            while (!mFrameAvailable) {
                try {
                    mLock.wait(TIMEOUT_MS);
                    if (!mFrameAvailable) {
                        throw new RuntimeException("Surface frame wait timed out");
                    }
                } catch (InterruptedException ie) {
                    // shouldn't happen
                    throw new RuntimeException(ie);
                }
            }
            mFrameAvailable = false;
        }

        mSurfaceTexture.updateTexImage();
    }

    public void drawImage(long pts) {
        mTextureRender.drawFrame(mSurfaceTexture, pts);
    }

    public Surface getSurface() {
        return mSurface;
    }

    public void release() {
        mSurface.release();
        mTextureRender = null;
        mSurface = null;
        mSurfaceTexture = null;
    }

    public void addDrawer(OverlayDrawer drawer) {
        mTextureRender.addDrawer(drawer);
    }

    public void setOverlayDrawScale(float scale) {
        mTextureRender.setOverlayDrawScale(scale);
    }

    public void setFilterProvider(VideoFilterProvider filterProvider) {
        mTextureRender.setFilterProvider(filterProvider);
    }
}
