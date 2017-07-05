package com.estrongs.android.pop.video.edit.muxer;

import java.io.IOException;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

/**
 * Created by lipeng21 on 2017/6/28.
 */

public class VideoDecoder {
    private static final String TAG = Muxer.class.getSimpleName();

    private Object mLock = new Object();

    private HandlerThread mHandlerThread;
    private Handler mHandler ;

    private volatile boolean isCreateDone;
    private String mMime;
    private MediaCodec.Callback mCallback;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    public void init(){
    }

    /**
     * 会阻塞
     */
    public MediaCodec startDecoder(MediaFormat mediaFormat, Surface surface, MediaCodec.Callback callback){
        Log.e("hahaha", "video startDecoder");
        mMediaFormat = mediaFormat;
        mMime = MediaHelper.getMimeTypeFor(mediaFormat);
        mCallback = callback;
        mHandlerThread = new HandlerThread("DecoderThread");
        mHandlerThread.start();
        mHandler = new DecoderHandler(mHandlerThread.getLooper());

        mHandler.sendEmptyMessage(0);
        //阻塞 保证MediaCodec创建成功
        synchronized (mLock){
            while(!isCreateDone){
                try {
                    mLock.wait();
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }

        if(mMediaCodec!=null){
            mMediaCodec.configure(mediaFormat, surface, null, 0);
            mMediaCodec.start();
        }

        Log.e("hahaha", "video startDecoder mMediaCodec: " + mMediaCodec);

        return mMediaCodec;

    }

    public void release(){
        if (mHandlerThread != null) {
            mHandlerThread.quitSafely();
        }
        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
        } catch(Exception e) {
            Log.e(TAG, "error while releasing VideoDecoder", e);
        }
    }


    private class DecoderHandler extends Handler{

        DecoderHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                mMediaCodec = MediaCodec.createDecoderByType(mMime);
                mMediaCodec.setCallback(mCallback);
            } catch (IOException e) {
                e.printStackTrace();
                mMediaCodec = null;
            }
            synchronized (mLock) {
                isCreateDone = true;
                mLock.notifyAll();
            }

        }

    }
}
