package com.estrongs.android.pop.video.edit.muxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

/**
 * Created by lipeng21 on 2017/6/28.
 */

public class VideoEncoder {
    private static final String TAG = Muxer.class.getSimpleName();

    private String OUTPUT_VIDEO_MIME_TYPE = "video/avc";
    private int OUTPUT_VIDEO_BIT_RATE = 2000000;         // 2Mbps
    private int OUTPUT_VIDEO_FRAME_RATE = 15;            // 15fps
    private int OUTPUT_VIDEO_IFRAME_INTERVAL = 10;       // 10 seconds between I-frames
    private int OUTPUT_VIDEO_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;


    private MediaCodec.Callback mCallback;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    public void init(int rotation, int pWidth, int pHeight){
        if (rotation == 0) {
            mMediaFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, pWidth, pHeight);
        }else {
            mMediaFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, pHeight, pWidth);
        }
        mMediaFormat = MediaFormat.createVideoFormat(OUTPUT_VIDEO_MIME_TYPE, pWidth, pHeight);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, OUTPUT_VIDEO_COLOR_FORMAT);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, OUTPUT_VIDEO_BIT_RATE);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, OUTPUT_VIDEO_FRAME_RATE);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, OUTPUT_VIDEO_IFRAME_INTERVAL);

        if (rotation != 0) {
            mMediaFormat.setInteger(MediaFormat.KEY_ROTATION, rotation);
        }
        Log.d(TAG, "video format: " + mMediaFormat);
    }

    public MediaCodec startEncode(AtomicReference<Surface> surfaceReference, MediaCodec.Callback callback){
        Log.e("hahaha", "video startEncode");
        mCallback = callback;

        MediaCodecInfo codecInfo = MediaHelper.selectCodec(OUTPUT_VIDEO_MIME_TYPE);
        if(codecInfo == null){
            Log.e(TAG, "Unable to find an appropriate codec for " + OUTPUT_VIDEO_MIME_TYPE);
            return null;
        }
        try {
            mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(mMediaCodec!=null){
            mMediaCodec.setCallback(callback);
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surfaceReference.set(mMediaCodec.createInputSurface());
            mMediaCodec.start();
        }
        Log.e("hahaha", "video startEncode mMediaCodec: " + mMediaCodec);

        return mMediaCodec;

    }

    public void signalEndOfInputStream(){
        if(mMediaCodec!=null){
            mMediaCodec.signalEndOfInputStream();
        }
    }

    public ByteBuffer getOutputBuffer(int index){
        if(mMediaCodec!=null){
            return mMediaCodec.getOutputBuffer(index);
        }
        return null;
    }

    public void releaseOutputBuffer(int index, boolean render){
        if(mMediaCodec!=null){
            mMediaCodec.releaseOutputBuffer(index, render);
        }
    }

    public void release(){

        try {
            if (mMediaCodec != null) {
                mMediaCodec.stop();
                mMediaCodec.release();
            }
        } catch(Exception e) {
            Log.e(TAG, "error while releasing VideoEncoder", e);
        }
    }


}
