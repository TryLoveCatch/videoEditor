package com.estrongs.android.pop.video.edit.muxer;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import com.estrongs.android.pop.video.opengl.EGLHelper;
import com.estrongs.android.pop.video.opengl.RenderHelper;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

/**
 * 合成器，主要作用
 *
 * 1、音频
 *      1.1、源文件解码输入
 *      1.2、目标文件编码输出
 *      1.3、源文件->目标文件 会有剪切处理
 * 2、视频
 *      2.1、源文件解码输入
 *      2.2、OpenGL渲染
 *          2.2.1、添加overlay
 *          2.2.2、添加滤镜
 *      2.3、目标文件编码输出
 *      2.4、源文件->目标文件 会有剪切处理
 *
 *
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class Muxer {

    private static final String TAG = Muxer.class.getSimpleName();
    private static final boolean VERBOSE = true;

    private Builder mBuilder;

    /**
     * 是否有音轨和视轨
     */
    private boolean mIsHaveVideo = true;
    private boolean mIsHaveAudio = true;

    private MediaMuxer mMuxer = null;
    private boolean mIsMuxing = false;

    private Audio mAudio;
    private Video mVideo;

    private MediaFormat mEncoderOutputVideoFormat = null;
    private int mOutputVideoTrack = -1;
    private boolean mVideoEncoderDone = false;
    private LinkedList<Integer> mArrVideoEncoderOutputBufferIndex;
    private LinkedList<MediaCodec.BufferInfo> mArrVideoEncoderOutputBufferInfos;

    private MediaFormat mEncoderOutputAudioFormat = null;
    private int mOutputAudioTrack = -1;
    private boolean mAudioEncoderDone = false;
    private LinkedList<Integer> mArrAudioEncoderOutputBufferIndex;
    private LinkedList<MediaCodec.BufferInfo> mArrAudioEncoderOutputBufferInfos;


    private int mVideoEncodedFrameCount = 0;
    private int mAudioEncodedFrameCount = 0;



    private Muxer(Builder tBuilder) {
        this.mBuilder = tBuilder;
    }


    public static class Builder{
        private String mDstFile; // 必选
        private String mSrcFile; // 必选
        protected RenderHelper mRenderHelper; // 必选
        /**
         * 是否解压缩视频和音频
         */
        protected boolean mCopyVideo = true;
        protected boolean mCopyAudio = true;

        protected ProgressListener mProgressListener;
        protected IMuxerController mMuxerController;


        public Builder setSrcFile(String pSrcFile) {
            mSrcFile = pSrcFile;
            return this;
        }

        public Builder setDstFile(String pDstFile) {
            mDstFile = pDstFile;
            return this;
        }

        public Builder setCopyVideo(boolean val) {
            mCopyVideo = val;
            return this;
        }

        public Builder setCopyAudio(boolean val) {
            mCopyAudio = val;
            return this;
        }

        public Builder setProgressListener(ProgressListener listener) {
            mProgressListener = listener;
            return this;
        }

        public Builder setMuxerController(IMuxerController controller) {
            mMuxerController = controller;
            return this;
        }

        public Builder setRenderHelper(RenderHelper pRenderHelper) {
            mRenderHelper = pRenderHelper;
            return this;
        }

        public Muxer create(){
            if(TextUtils.isEmpty(mSrcFile) || TextUtils.isEmpty(mDstFile)){
                return null;
            }

            if(mRenderHelper == null){
                return null;
            }

            Muxer tMuxer = new Muxer(this);
            return tMuxer;
        }
    }

    public void start(boolean sync) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    extractDecodeEditEncodeMux();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        thread.start();
        if (sync) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void extractDecodeEditEncodeMux() {

        if (mBuilder.mProgressListener != null) {
            mBuilder.mProgressListener.onStart();
        }

        clear();

        try {
            mMuxer = new MediaMuxer(mBuilder.mDstFile, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            if (mBuilder.mCopyVideo) {
                mVideo = new Video(mBuilder.mSrcFile, mBuilder.mRenderHelper);
                mVideo.start(true, new Video.IVideoCallback() {
                    @Override
                    public void onError(MediaError error) {
                        Log.e(TAG, error.msg);
                        if(error.code == MediaError.CODE_NO_VIDEO){
                            mIsHaveVideo = false;
                        }else {
                            if(mBuilder.mProgressListener!=null) {
                                mBuilder.mProgressListener.onError();
                            }
                        }
                    }

                    @Override
                    public void onOutputFormatChanged(MediaFormat pEncoderOutputFormat) {
                        if (mOutputVideoTrack >= 0) {
                            Log.e(TAG, "video encoder changed its output format again?");
                            return;
                        }
                        mEncoderOutputVideoFormat = pEncoderOutputFormat;
                        setupMuxer();
                    }

                    @Override
                    public void onOutputBufferAvailable(int index, MediaCodec.BufferInfo info) {
                        muxVideo(index, info);
                    }

                    @Override
                    public boolean isDropFrame(long time) {
                        return mBuilder.mMuxerController!=null && mBuilder.mMuxerController.isDropFrame(time);
                    }
                });

            }

            if(mBuilder.mCopyAudio) {
                mAudio = new Audio(mBuilder.mSrcFile);
                mAudio.start(true, new Audio.IAudioCallback() {
                    @Override
                    public void onError(MediaError error) {
                        Log.e(TAG, error.msg);
                        if(error.code == MediaError.CODE_NO_AUDIO){
                            mIsHaveAudio = false;
                        }else {
                            if(mBuilder.mProgressListener!=null) {
                                mBuilder.mProgressListener.onError();
                            }
                        }
                    }

                    @Override
                    public void onOutputFormatChanged(MediaFormat pEncoderOutputFormat) {
                        if (mOutputAudioTrack >= 0) {
                            Log.e(TAG, "audio encoder changed its output format again?");
                            return;
                        }
                        mEncoderOutputAudioFormat = pEncoderOutputFormat;
                        setupMuxer();
                    }

                    @Override
                    public void onOutputBufferAvailable(int index, MediaCodec.BufferInfo info) {
                        muxAudio(index, info);
                    }

                    @Override
                    public boolean isDropFrame(long time) {
                        return mBuilder.mMuxerController!=null && mBuilder.mMuxerController.isDropFrame(time);
                    }
                });
            }


            awaitEncode();

            if (mBuilder.mProgressListener != null) {
                mBuilder.mProgressListener.onCompleted();
            }

        }catch (Exception e){
            e.printStackTrace();
            if(mBuilder.mProgressListener!=null) {
                mBuilder.mProgressListener.onError();
            }
        } finally {

            if (VERBOSE)
                Log.d(TAG, "releasing extractor, decoder, encoder, and muxer");

            try {
                if (mBuilder.mRenderHelper != null) {
                    mBuilder.mRenderHelper.release();
                }
            } catch(Exception e) {
                e.printStackTrace();
                Log.e(TAG, "error while releasing outputSurface", e);
            }

            try {
                if (mMuxer != null) {
                    mMuxer.stop();
                    mMuxer.release();
                }
            } catch(Exception e) {
                e.printStackTrace();
                Log.e(TAG, "error while releasing muxer", e);
            }

            try {
                EGLHelper.getInstance().release();
            } catch(Exception e) {
                e.printStackTrace();
                Log.e(TAG, "error while releasing inputSurface", e);
            }
            if(mAudio!=null){
                mAudio.release();
            }
            if(mVideo!=null){
                mVideo.release();
            }

            mMuxer = null;
            mAudio = null;
            mVideo = null;
        }
    }

    private void setupMuxer() {
        if(mIsMuxing){
            return;
        }

        boolean tIsReturn = false;

        if(mBuilder.mCopyVideo){
            if(mIsHaveVideo){
                if(mEncoderOutputVideoFormat == null){
                    tIsReturn = true;
                }
            }
        }

        if(mBuilder.mCopyAudio){
            if(mIsHaveAudio){
                if(mEncoderOutputAudioFormat == null){
                    tIsReturn = true;
                }
            }
        }

        if(tIsReturn){
            return;
        }


        if (mBuilder.mCopyVideo && mIsHaveVideo) {
            Log.d(TAG, "muxer: adding video track.");
            mOutputVideoTrack = mMuxer.addTrack(mEncoderOutputVideoFormat);
        }

        if (mBuilder.mCopyAudio && mIsHaveAudio) {
            Log.d(TAG, "muxer: adding audio track.");
            mOutputAudioTrack = mMuxer.addTrack(mEncoderOutputAudioFormat);
        }

        Log.d(TAG, "muxer: starting");
        mMuxer.start();
        mIsMuxing = true;

        MediaCodec.BufferInfo info;
        while ((info = mArrVideoEncoderOutputBufferInfos.poll()) != null) {
            int index = mArrVideoEncoderOutputBufferIndex.poll().intValue();
            muxVideo(index, info);
        }

        while ((info = mArrAudioEncoderOutputBufferInfos.poll()) != null) {
            int index = mArrAudioEncoderOutputBufferIndex.poll().intValue();
            muxAudio(index, info);
        }
    }

    private void muxVideo(int index, MediaCodec.BufferInfo info) {
        if (!mIsMuxing) {
            mArrVideoEncoderOutputBufferIndex.add(new Integer(index));
            mArrVideoEncoderOutputBufferInfos.add(info);
            return;
        }

        ByteBuffer encoderOutputBuffer = mVideo.getEncoder().getOutputBuffer(index);
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {

            if (VERBOSE)
                Log.d(TAG, "video encoder: codec config buffer");

            mVideo.getEncoder().releaseOutputBuffer(index, false);
            return;
        }

        if (VERBOSE) {
            Log.d(TAG, "video encoder: returned buffer for time " + info.presentationTimeUs);
        }

        if (info.size != 0) {
            mMuxer.writeSampleData(mOutputVideoTrack, encoderOutputBuffer, info);
        }

        mVideo.getEncoder().releaseOutputBuffer(index, false);
        mVideoEncodedFrameCount++;
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE)
                Log.d(TAG, "video encoder: EOS");

            synchronized (this) {
                mVideoEncoderDone = true;
                notifyAll();
            }
        }

        logState();
    }

    private void muxAudio(int index, MediaCodec.BufferInfo info) {
        if (!mIsMuxing) {
            mArrAudioEncoderOutputBufferIndex.add(new Integer(index));
            mArrAudioEncoderOutputBufferInfos.add(info);
            return;
        }

        ByteBuffer encoderOutputBuffer = mAudio.getEncoder().getOutputBuffer(index);
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            if (VERBOSE)
                Log.d(TAG, "audio encoder: codec config buffer");

            mAudio.getEncoder().releaseOutputBuffer(index, false);
            return;
        }

        if (VERBOSE) {
            Log.d(TAG, "audio encoder: returned buffer for time " + info.presentationTimeUs);
        }

        if (info.size != 0) {
            mMuxer.writeSampleData(mOutputAudioTrack, encoderOutputBuffer, info);
        }

        mAudio.getEncoder().releaseOutputBuffer(index, false);
        mAudioEncodedFrameCount++;
        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            if (VERBOSE) Log.e(TAG, "audio encoder: EOS");
            synchronized (this) {
                mAudioEncoderDone = true;
                notifyAll();
            }
        }
        logState();
    }


    private void awaitEncode() {
        synchronized (this) {
            while ((mBuilder.mCopyVideo && mIsHaveVideo && !mVideoEncoderDone)
                    || (mBuilder.mCopyAudio && mIsHaveAudio && !mAudioEncoderDone)) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
            }
        }
    }


    private void clear(){
        mEncoderOutputVideoFormat = null;
        mEncoderOutputAudioFormat = null;

        mOutputVideoTrack = -1;
        mOutputAudioTrack = -1;
        mVideoEncoderDone = false;
        mAudioEncoderDone = false;
        mArrVideoEncoderOutputBufferIndex = new LinkedList<>();
        mArrVideoEncoderOutputBufferInfos = new LinkedList<>();
        mArrAudioEncoderOutputBufferIndex = new LinkedList<>();
        mArrAudioEncoderOutputBufferInfos = new LinkedList<>();
        mIsMuxing = false;
        mVideoEncodedFrameCount = 0;
        mAudioEncodedFrameCount = 0;
    }

    private void logState() {
        if (VERBOSE) {
            Log.d(TAG, String.format(
                    "loop: "
                            + "V(%b){"
                            + "encoded:%d(done:%b)} "

                            + "A(%b){"
                            + "encoded:%d(done:%b) "

                            + "muxing:%b(V:%d,A:%d)",

                    mBuilder.mCopyVideo,
                    mVideoEncodedFrameCount, mVideoEncoderDone,

                    mBuilder.mCopyAudio,
                    mAudioEncodedFrameCount, mAudioEncoderDone,

                    mIsMuxing, mOutputVideoTrack, mOutputAudioTrack));
        }
    }

}
