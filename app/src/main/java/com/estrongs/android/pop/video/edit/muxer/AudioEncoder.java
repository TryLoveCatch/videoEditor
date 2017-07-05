package com.estrongs.android.pop.video.edit.muxer;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

/**
 * Created by lipeng21 on 2017/6/28.
 */

public class AudioEncoder {
    private static final String TAG = Muxer.class.getSimpleName();
    private String OUTPUT_AUDIO_MIME_TYPE = "audio/mp4a-latm";  // Advanced Audio Coding
    private int OUTPUT_AUDIO_CHANNEL_COUNT = 2;                 // Must match the input stream.
    private int OUTPUT_AUDIO_BIT_RATE = 128 * 1024;
    private int OUTPUT_AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectHE;
    private int OUTPUT_AUDIO_SAMPLE_RATE_HZ = 44100;            // Must match the input stream.


    private MediaCodec.Callback mCallback;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;

    public void init(int sampleRateHz, int channelCount, int bitRate, int accProfile){

        if(sampleRateHz == -1){
            sampleRateHz = OUTPUT_AUDIO_SAMPLE_RATE_HZ;
        }
        if(channelCount == -1){
            channelCount = OUTPUT_AUDIO_CHANNEL_COUNT;
        }
        if(bitRate == -1){
            bitRate = OUTPUT_AUDIO_BIT_RATE;
        }
        if(accProfile == -1){
            accProfile = OUTPUT_AUDIO_AAC_PROFILE;
        }

        mMediaFormat = MediaFormat.createAudioFormat(OUTPUT_AUDIO_MIME_TYPE, sampleRateHz, channelCount);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mMediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, accProfile);

        Log.d(TAG, "audio format: " + mMediaFormat);
    }

    /**
     * 会阻塞
     */
    public MediaCodec startEncode(MediaCodec.Callback callback){
        Log.e("hahaha", "audio startEncode");
        mCallback = callback;

        MediaCodecInfo codecInfo = MediaHelper.selectCodec(OUTPUT_AUDIO_MIME_TYPE);
        if(codecInfo == null){
            Log.e(TAG, "Unable to find an appropriate codec for " + OUTPUT_AUDIO_MIME_TYPE);
            return null;
        }
        try {
            mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(mMediaCodec!=null){
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mMediaCodec.start();
        }

        Log.e("hahaha", "audio startEncode mMediaCodec: " + mMediaCodec);

        return mMediaCodec;

    }

    public ByteBuffer getInputBuffer(int index){
        if(mMediaCodec!=null){
            return mMediaCodec.getInputBuffer(index);
        }
        return null;
    }

    public void queueInputBuffer(int index,
                                 int offset, int size, long presentationTimeUs, int flags){
        if(mMediaCodec!=null){
            mMediaCodec.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
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
            Log.e(TAG, "error while releasing audioEncoder", e);
        }
    }


}
