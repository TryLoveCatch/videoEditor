package com.estrongs.android.pop.video.edit.muxer;

import static com.google.android.exoplayer2.mediacodec.MediaCodecInfo.TAG;

import java.io.IOException;
import java.nio.ByteBuffer;

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

public class AudioDecoder {

    private MediaCodec mMediaCodec;

    public void init(){
    }

    /**
     * 会阻塞
     */
    public MediaCodec startDecoder(MediaFormat mediaFormat, MediaCodec.Callback callback){
        Log.e("hahaha", "audio startDecoder");
        try {
            mMediaCodec = MediaCodec.createDecoderByType(MediaHelper.getMimeTypeFor(mediaFormat));
            mMediaCodec.setCallback(callback);
            mMediaCodec.configure(mediaFormat, null, null, 0);
            mMediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
            mMediaCodec = null;
        }
        Log.e("hahaha", "audio startDecoder mMediaCodec: " + mMediaCodec);

        return mMediaCodec;

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
            Log.e(TAG, "error while releasing VideoDecoder", e);
        }
    }


}
