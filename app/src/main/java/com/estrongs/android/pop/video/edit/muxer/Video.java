package com.estrongs.android.pop.video.edit.muxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import com.estrongs.android.pop.video.opengl.EGLHelper;
import com.estrongs.android.pop.video.opengl.RenderHelper;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

/**
 * Created by lipeng21 on 2017/6/29.
 */

public class Video {
    private static final String TAG = Video.class.getSimpleName();

    private MediaExtractor mMediaExtractor;
    private VideoDecoder mVideoDecoder;
    private VideoEncoder mVideoEncoder;

    private MediaFormat mEncoderOutputFormat;
    private MediaFormat mDecoderOutputFormat;

    private IVideoCallback mVideoCallback;

    private RenderHelper mRenderHelper;

    private boolean mIsAsync;//同步or异步处理 默认异步
    private String mSrcFile;
    private int mWidth = -1;
    private int mHeight = -1;

    private long mLastSamplePresentationTime = 0;
    private long mDropedPresentationTime = 0;

    // 打印相关
    private int mExtractedFrameCount;
    private int mDecodedFrameCount;
    private boolean mDecoderDone;
    private boolean mExtractorDone;

    public Video(String pSrcFile, RenderHelper pRenderHelper){
        mSrcFile = pSrcFile;
        mRenderHelper = pRenderHelper;
        mIsAsync = true;
    }

    public interface IVideoCallback{
        void onError(MediaError error);
        void onOutputFormatChanged(MediaFormat pEncoderOutputFormat);
        void onOutputBufferAvailable(int index, MediaCodec.BufferInfo info);
        boolean isDropFrame(long time);
    }

    public void start(boolean pIsAsync, IVideoCallback pVideoCallback){
        mIsAsync = pIsAsync;
        mVideoCallback = pVideoCallback;
        try {
            mMediaExtractor = MediaHelper.createExtractor(mSrcFile);
        } catch (IOException e) {
            mMediaExtractor = null;
            e.printStackTrace();
            if(mVideoCallback!=null){
                mVideoCallback.onError(new MediaError("video MediaExtractor create failed"));
            }
            return;
        }
        int videoInputTrack = MediaHelper.getAndSelectVideoTrackIndex(mMediaExtractor);
        if(videoInputTrack==-1){
            if(mVideoCallback!=null){
                mVideoCallback.onError(new MediaError(MediaError.CODE_NO_VIDEO,
                        "missing video track in " + mSrcFile));
            }
            return;
        }
        MediaFormat inputFormat = mMediaExtractor.getTrackFormat(videoInputTrack);
        if (mWidth == -1 || mHeight == -1) {
            mWidth = inputFormat.getInteger(MediaFormat.KEY_WIDTH);
            mHeight = inputFormat.getInteger(MediaFormat.KEY_HEIGHT);
        }

        int rotation = 0;
        if (inputFormat.containsKey(MediaFormat.KEY_ROTATION)) {
            rotation = inputFormat.getInteger(MediaFormat.KEY_ROTATION);
        }

        /**
         * 通过一个surface充当介质
         * 联系起来解码器和编码器
         */
        AtomicReference<Surface> inputSurfaceReference = new AtomicReference<Surface>();

        MediaCodec tVideoEncoder = createVideoEncoder(rotation, inputSurfaceReference);
        if(tVideoEncoder == null){
            if(mVideoCallback!=null){
                mVideoCallback.onError(new MediaError("video Encoder create failed"));
            }
            return;
        }
        EGLHelper.getInstance().init(inputSurfaceReference.get());
        mRenderHelper.init(mWidth, mHeight, rotation);

        MediaCodec tVideoDecoder = createVideoDecoder(inputFormat, mRenderHelper.getSurface());
        if(tVideoDecoder == null){
            if(mVideoCallback!=null){
                mVideoCallback.onError(new MediaError("video decoder create failed"));
            }
            return;
        }
        EGLHelper.getInstance().releaseEGLContext();
    }

    public VideoEncoder getEncoder(){
        return mVideoEncoder;
    }

    public void release(){
        if(mVideoDecoder!=null){
            mVideoDecoder.release();
        }

        if(mVideoEncoder!=null){
            mVideoEncoder.release();
        }

        if(mMediaExtractor!=null){
            mMediaExtractor.release();
        }
    }


    private MediaCodec createVideoEncoder(
            int rotation,
            AtomicReference<Surface> surfaceReference){

        mVideoEncoder = new VideoEncoder();
        mVideoEncoder.init(rotation, mWidth, mHeight);

        MediaCodec encoder = mVideoEncoder.startEncode(surfaceReference, new MediaCodec.Callback() {

            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
                exception.printStackTrace();
                if(mVideoCallback!=null){
                    mVideoCallback.onError(new MediaError("audio encoder error " + exception.getMessage()));
                }
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                Log.d(TAG, "video encoder: output format changed");

                mEncoderOutputFormat = codec.getOutputFormat();
                if(mVideoCallback!=null){
                    mVideoCallback.onOutputFormatChanged(mEncoderOutputFormat);
//                    if (mOutputVideoTrack >= 0) {
//                        fail("video encoder changed its output format again?");
//                    }
                    //setupMuxer();
                }
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {
            }

            public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                Log.d(TAG, "video encoder: returned output buffer: " + index);
                Log.d(TAG, "video encoder: returned buffer of size " + info.size);
                if(mVideoCallback!=null) {
                    mVideoCallback.onOutputBufferAvailable(index, info);
                }
            }
        });
        return encoder;
    }


    private MediaCodec createVideoDecoder(MediaFormat inputFormat, Surface surface) {

        MediaCodec.Callback callback = new MediaCodec.Callback() {
            public void onError(MediaCodec codec, MediaCodec.CodecException exception) {
                exception.printStackTrace();
                if(mVideoCallback!=null){
                    mVideoCallback.onError(new MediaError("audio encoder error " + exception.getMessage()));
                }
            }

            public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                mDecoderOutputFormat = codec.getOutputFormat();
                Log.d(TAG, "video decoder: output format changed: "
                        + mDecoderOutputFormat);
            }

            public void onInputBufferAvailable(MediaCodec codec, int index) {

                ByteBuffer decoderInputBuffer = codec.getInputBuffer(index);
//                boolean tIsDone = false;
//                while (!tIsDone) {
//
//                    int size = mMediaExtractor.readSampleData(decoderInputBuffer, 0);
//                    long presentationTime = mMediaExtractor.getSampleTime();
//
//                    Log.d(TAG, "video extractor: returned buffer of size " + size);
//                    Log.d(TAG, "video extractor: returned buffer for time " + presentationTime);
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
//                    if (tIsDone) {
//                        Log.d(TAG, "video extractor: EOS");
//                        codec.queueInputBuffer(
//                                index,
//                                0,
//                                0,
//                                0,
//                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//                    }
//
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
                Log.d(TAG, "video decoder: returned output buffer: " + index);
                Log.d(TAG, "video decoder: returned buffer of size " + info.size);

                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    Log.d(TAG, "video decoder: codec config buffer");
                    codec.releaseOutputBuffer(index, false);
                    return;
                }

                Log.d(TAG, "video decoder: returned buffer for time " + info.presentationTimeUs);

                if (mLastSamplePresentationTime == 0)
                    mLastSamplePresentationTime = info.presentationTimeUs;

                boolean render = info.size != 0;

                if (render && mVideoCallback != null) {
                    if (mVideoCallback.isDropFrame(info.presentationTimeUs)) {
                        mDropedPresentationTime += info.presentationTimeUs - mLastSamplePresentationTime;
                        render = false;
                    }
                }

                mLastSamplePresentationTime = info.presentationTimeUs;

                codec.releaseOutputBuffer(index, render);
                if (render) {

                    EGLHelper.getInstance().makeCurrent();

                    Log.d(TAG, "output surface: await new image");

                    mRenderHelper.awaitNewImage();

                    Log.d(TAG, "output surface: draw image");
                    mRenderHelper.drawImage(info.presentationTimeUs);

                    EGLHelper.getInstance().setPresentationTime((info.presentationTimeUs - mDropedPresentationTime)* 1000);

                    Log.d(TAG, "input surface: swap buffers");
                    EGLHelper.getInstance().swapBuffers();

                    Log.d(TAG, "video encoder: notified of new frame");
                    EGLHelper.getInstance().releaseEGLContext();
                }

                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(TAG, "video decoder: EOS");
                    mDecoderDone = true;
                    mVideoEncoder.signalEndOfInputStream();
                }

                mDecodedFrameCount++;
                logState();
            }
        };

        mVideoDecoder = new VideoDecoder();
        MediaCodec decoder = mVideoDecoder.startDecoder(inputFormat, surface, callback);
        return decoder;
    }

    private void logState() {
        Log.d(TAG, String.format(
                "loop: "
                        + "V(){"
                        + "extracted:%d(done:%b) "
                        + "decoded:%d(done:%b) ",

                mExtractedFrameCount, mExtractorDone,
                mDecodedFrameCount, mDecoderDone));
    }

}
