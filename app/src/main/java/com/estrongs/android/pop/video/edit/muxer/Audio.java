package com.estrongs.android.pop.video.edit.muxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

/**
 * Created by lipeng21 on 2017/6/29.
 */

public class Audio {
    private static final String TAG = Audio.class.getSimpleName();

    private MediaExtractor mMediaExtractor;
    private AudioEncoder mAudioEncoder;
    private AudioDecoder mAudioDecoder;

    private MediaFormat mEncoderOutputFormat;
    private MediaFormat mDecoderOutputFormat;

    private LinkedList<Integer> mArrEncoderInputBufferIndex;
    private LinkedList<Integer> mArrDecoderOutputBufferIndex;
    private LinkedList<MediaCodec.BufferInfo> mArrDecoderOutputBufferInfos;


    private IAudioCallback mAudioCallback;

    private boolean mIsAsync;//同步or异步处理 默认异步
    private String mSrcFile;

    private long mLastSamplePresentationTime = 0;
    private long mDropedPresentationTime = 0;

    // 打印相关
    private int mExtractedFrameCount;
    private int mDecodedFrameCount;
    private boolean mDecoderDone;
    private boolean mExtractorDone;


    public Audio(String pSrcFile){
        mSrcFile = pSrcFile;
        mIsAsync = true;
        mArrEncoderInputBufferIndex = new LinkedList<>();
        mArrDecoderOutputBufferIndex = new LinkedList<>();
        mArrDecoderOutputBufferInfos = new LinkedList<>();
    }

    public interface IAudioCallback{
        void onError(MediaError error);
        void onOutputFormatChanged(MediaFormat pEncoderOutputFormat);
        void onOutputBufferAvailable(int index, MediaCodec.BufferInfo info);
        boolean isDropFrame(long time);
    }


    public void start(boolean pIsAsync, IAudioCallback pAudioCallback){
        mIsAsync = pIsAsync;
        mAudioCallback = pAudioCallback;
        try {
            mMediaExtractor = MediaHelper.createExtractor(mSrcFile);
        } catch (IOException e) {
            mMediaExtractor = null;
            e.printStackTrace();
            if(mAudioCallback!=null){
                mAudioCallback.onError(new MediaError("audio MediaExtractor create failed"));
            }
            return;
        }
        int audioInputTrack = MediaHelper.getAndSelectAudioTrackIndex(mMediaExtractor);

        if(audioInputTrack == -1){

            if(mAudioCallback!=null){
                mAudioCallback.onError(new MediaError(MediaError.CODE_NO_AUDIO,
                        "missing audio track in " + mSrcFile));
            }
            return;

        }
        MediaFormat inputFormat = mMediaExtractor.getTrackFormat(audioInputTrack);

        int sampleRateHz = -1;
        int channelCount = -1;
        int bitRate = -1;
        int accProfile = -1;

        if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
            sampleRateHz = inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        }

        if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            channelCount = inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        }

        if (inputFormat.containsKey(MediaFormat.KEY_BIT_RATE)) {
            bitRate = inputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
        }

        if (inputFormat.containsKey(MediaFormat.KEY_AAC_PROFILE)) {
            accProfile = inputFormat.getInteger(MediaFormat.KEY_AAC_PROFILE);
        }

        mAudioEncoder = new AudioEncoder();
        mAudioEncoder.init(sampleRateHz, channelCount, bitRate, accProfile);

        MediaCodec tAudioEncoder = createAudioEncoder();
        if (tAudioEncoder == null) {
            if(mAudioCallback!=null){
                mAudioCallback.onError(new MediaError("audio Encoder create failed"));
            }
            return;
        }
        mAudioDecoder = new AudioDecoder();
        MediaCodec tAudioDecoder = createAudioDecoder(inputFormat);
        if (tAudioDecoder == null) {
            if(mAudioCallback!=null){
                mAudioCallback.onError(new MediaError("audio decoder create failed"));
            }
            return;
        }
    }

    public AudioEncoder getEncoder(){
        return mAudioEncoder;
    }

    public void release(){
        if(mAudioDecoder!=null){
            mAudioDecoder.release();
        }

        if(mAudioEncoder!=null){
            mAudioEncoder.release();
        }

        if(mMediaExtractor!=null){
            mMediaExtractor.release();
        }
    }


    private MediaCodec createAudioEncoder() {
        MediaCodec encoder = mAudioEncoder.startEncode(new MediaCodec.Callback() {

            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
                exception.printStackTrace();
                if(mAudioCallback!=null){
                    mAudioCallback.onError(new MediaError("audio encoder error " + exception.getMessage()));
                }
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                Log.d(TAG, "audio encoder: output format changed");
                mEncoderOutputFormat = codec.getOutputFormat();
                if(mAudioCallback!=null){
                    mAudioCallback.onOutputFormatChanged(mEncoderOutputFormat);
                }
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                Log.d(TAG, "audio encoder: returned input buffer: " + index);
                mArrEncoderInputBufferIndex.add(index);
                tryCreateEncoderInputBuffer();
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                Log.d(TAG, "audio encoder: returned output buffer: " + index);
                Log.d(TAG, "audio encoder: returned buffer of size " + info.size);
                if(mAudioCallback!=null) {
                    mAudioCallback.onOutputBufferAvailable(index, info);
                }
            }
        });
        return encoder;
    }

    private MediaCodec createAudioDecoder(MediaFormat inputFormat) {
        MediaCodec tMediaCodec = mAudioDecoder.startDecoder(inputFormat, new MediaCodec.Callback() {

            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
                exception.printStackTrace();
                if(mAudioCallback!=null){
                    mAudioCallback.onError(new MediaError("audio encoder error " + exception.getMessage()));
                }
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                mDecoderOutputFormat = codec.getOutputFormat();
                Log.d(TAG, "audio decoder: output format changed: " + mDecoderOutputFormat);
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
                ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);
//                boolean tIsDone = false;
//                while (!tIsDone) {
//
//                    int size = mMediaExtractor.readSampleData(decoderInputBuffer, 0);
//                    long presentationTime = mMediaExtractor.getSampleTime();
//                    Log.d(TAG, "audio extractor: returned buffer of size " + size);
//                    Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);
//
//                    if (size >= 0) {
//                        codec.queueInputBuffer(
//                                index,
//                                0,
//                                size,
//                                presentationTime,
//                                mMediaExtractor.getSampleFlags());
//                    }
//
//                    tIsDone = !mMediaExtractor.advance();
//
//                    // 没有更多数据了
//                    if (tIsDone) {
//                        Log.e(TAG, "audio extractor: EOS");
//                        codec.queueInputBuffer(
//                                index,
//                                0,
//                                0,
//                                0,
//                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                         mExtractorDone = true;
//                    }
//                    mExtractedFrameCount++;
//                    logState();
//                    if (size >= 0)
//                        break;
//                }

                int size = mMediaExtractor.readSampleData(decoderInputBuffer, 0);
                long presentationTime = mMediaExtractor.getSampleTime();
                Log.d(TAG, "audio extractor: returned buffer of size " + size);
                Log.d(TAG, "audio extractor: returned buffer for time " + presentationTime);

                if (size >= 0) {
                    codec.queueInputBuffer(
                            index,
                            0,
                            size,
                            presentationTime,
                            mMediaExtractor.getSampleFlags());
                    mMediaExtractor.advance();
                }else{
                    Log.e(TAG, "audio extractor: EOS");
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    mExtractorDone = true;
                }
                mExtractedFrameCount++;
                logState();

            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                Log.d(TAG, "audio decoder: returned output buffer: " + index);
                Log.d(TAG, "audio decoder: returned buffer of size " + info.size);

                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "audio decoder: codec config buffer");
                    codec.releaseOutputBuffer(index, false);
                    return;
                }

                Log.d(TAG, "audio decoder: returned buffer for time " + info.presentationTimeUs);

                mDecodedFrameCount++;

                mArrDecoderOutputBufferIndex.add(index);
                mArrDecoderOutputBufferInfos.add(info);

                logState();
                tryCreateEncoderInputBuffer();
            }
        });
        return tMediaCodec;
    }


    private void tryCreateEncoderInputBuffer() {

        if (mArrEncoderInputBufferIndex.size() == 0 ||
                mArrDecoderOutputBufferIndex.size() == 0) {
            return;
        }

        int decoderIndex = mArrDecoderOutputBufferIndex.poll();
        MediaCodec.BufferInfo info = mArrDecoderOutputBufferInfos.poll();
        int size = info.size;
        long presentationTime = info.presentationTimeUs;

        if (mLastSamplePresentationTime == 0) {
            mLastSamplePresentationTime = info.presentationTimeUs;
        }

        if (size >= 0) {

            boolean drop;
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                drop = false;
            }else {
                drop = mAudioCallback != null && mAudioCallback.isDropFrame(presentationTime);
            }

            if (drop) {
                mDropedPresentationTime += presentationTime - mLastSamplePresentationTime;
            }

            if (!drop) {

                if (mDropedPresentationTime > 0) {
                    presentationTime -= mDropedPresentationTime;
                }

                int encoderIndex = mArrEncoderInputBufferIndex.poll();

                ByteBuffer encoderInputBuffer = mAudioEncoder.getInputBuffer(encoderIndex);

                Log.d(TAG, "audio decoder: processing pending buffer: " + decoderIndex);
                Log.d(TAG, "audio decoder: pending buffer of size " + size);
                Log.d(TAG, "audio decoder: pending buffer for time " + presentationTime);

                ByteBuffer decoderOutputBuffer = mAudioDecoder.getOutputBuffer(decoderIndex).duplicate();
                decoderOutputBuffer.position(info.offset);
                decoderOutputBuffer.limit(info.offset + size);
                encoderInputBuffer.position(0);
                encoderInputBuffer.put(decoderOutputBuffer);

                mAudioEncoder.queueInputBuffer(
                        encoderIndex,
                        0,
                        size,
                        presentationTime,
                        info.flags);
            }

        }

        mLastSamplePresentationTime = info.presentationTimeUs;
        mAudioDecoder.releaseOutputBuffer(decoderIndex, false);

        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            Log.e(TAG, "audio decoder: EOS");
            mDecoderDone = true;
        }

        logState();
    }



    private void logState() {
        Log.d(TAG, String.format(
                "loop: "
                + "Audio(){"
                + "extracted:%d(done:%b) "
                + "decoded:%d(done:%b) ",
                mExtractedFrameCount, mExtractorDone,
                mDecodedFrameCount, mDecoderDone));
    }

}
