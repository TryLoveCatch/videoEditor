/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.estrongs.android.pop.video.edit.view.frames;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.estrongs.android.pop.video.opengl.GLHelper;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class VideoFrameExtractor {

    public interface Listener {
        void onFormat(MediaFormat format);
        void onFrame(int idx, Bitmap bitmap, long pts);
        void onCompleted();
        void onFailed(int error);
        int  getRequiredFrameCount();
    }

    private static final String TAG = "VideoFrameExtractor";
    private static final boolean VERBOSE = true;           // lots of logging

    private String videoFile;

    private int outputWidth = -1;
    private int outputHeight = -1;
    private long seekInterval = -1;
    private long iFrameInterval = -1;
    private long firstKeyFramePTS = 0;
    private long nextKeyFrmePTS = 0;
    private MediaFormat format;

    private Listener listener;

    public VideoFrameExtractor(String videoFile, int width, int height, Listener listener) {
        this.outputWidth = width;
        this.outputHeight = height;
        this.videoFile = videoFile;
        this.listener = listener;
    }

    public void start(boolean sync) {
        Thread thread = new Thread(){

            public void run() {
                try {
                    extractFrames();
                } catch (Exception e) {
                    listener.onFailed(-1);
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

    private void extractFrames() throws Exception {

        MediaCodec decoder = null;
        CodecOutputSurface outputSurface = null;
        MediaExtractor extractor = null;

        try {
            File inputFile = new File(videoFile);   // must be an absolute path

            if (!inputFile.canRead()) {
                throw new FileNotFoundException("Unable to read " + inputFile);
            }

            extractor = new MediaExtractor();
            extractor.setDataSource(inputFile.toString());
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                throw new RuntimeException("No video track found in " + inputFile);
            }
            extractor.selectTrack(trackIndex);

            format = extractor.getTrackFormat(trackIndex);
            int width = format.getInteger(MediaFormat.KEY_WIDTH);
            int height = format.getInteger(MediaFormat.KEY_HEIGHT);

            if (VERBOSE) {
                Log.d(TAG, "Video size is " + width + "x" + height);
            }

            int rotation = 0;
            if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                rotation = format.getInteger(MediaFormat.KEY_ROTATION);
            }

            if (outputWidth != -1 && outputHeight != -1) {

                if (outputWidth > width || outputHeight > height) {
                    outputWidth = width;
                    outputHeight = height;
                } else {
                    float r = width / (float) height;
                    outputHeight = (int)(outputWidth / r);
                }

                if (rotation == 90 || rotation == 270)
                    outputSurface = new CodecOutputSurface(outputHeight, outputWidth);
                else
                    outputSurface = new CodecOutputSurface(outputWidth, outputHeight);
            } else {
                if (rotation == 90 || rotation == 270)
                    outputSurface = new CodecOutputSurface(height, width);
                else
                    outputSurface = new CodecOutputSurface(width, height);
            }

            long duration = format.getLong(MediaFormat.KEY_DURATION);
            if (duration > 0) {
                // 这里传进来了10帧
                int requiredCount = listener.getRequiredFrameCount();
                // 根据传进来的帧数 算出来需要跳过的间隔
                if (requiredCount > 0) {
                    seekInterval = duration / requiredCount;
                }
            }

            listener.onFormat(format);

            String mime = format.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            decoder.configure(format, outputSurface.getSurface(), null, 0);
            decoder.start();

            doExtract(extractor, trackIndex, decoder, outputSurface);

        } finally {
            if (outputSurface != null) {
                outputSurface.release();
            }

            if (decoder != null) {
                decoder.stop();
                decoder.release();
            }

            if (extractor != null) {
                extractor.release();
            }
        }
    }

    private int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }

        return -1;
    }

    private boolean isKeyFrame(MediaExtractor extractor) {
        boolean ret = (extractor.getSampleFlags() & MediaExtractor.SAMPLE_FLAG_SYNC) != 0;
        return ret;
    }

    private void doExtract(MediaExtractor extractor,
                          int trackIndex,
                          MediaCodec decoder,
                          CodecOutputSurface outputSurface) throws Exception {

        final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int decodeCount = 0;
        long frameSaveTime = 0;

        boolean outputDone = false;
        boolean inputDone = false;

        long nextFrameTime = 0;

        {
            MediaExtractor iframeExtractor = new MediaExtractor();
            iframeExtractor.setDataSource(videoFile);

            int videoTrack = selectTrack(extractor);
            iframeExtractor.selectTrack(videoTrack);

            int maxSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            ByteBuffer buffers = ByteBuffer.allocate(maxSize);

            int chunkSize = iframeExtractor.readSampleData(buffers, 0);
            long tm1 = iframeExtractor.getSampleTime();

            /**
             * 1、读到第一个关键帧
             * 2、然后 继续读下一关键帧
             * 3、记录两个帧的时间间隔
             */
            if (chunkSize > 0 && isKeyFrame(iframeExtractor)) {

                iframeExtractor.advance();
                chunkSize = iframeExtractor.readSampleData(buffers, 0);
                long tm2 = iframeExtractor.getSampleTime();

                if (chunkSize > 0 && isKeyFrame(iframeExtractor)) {
                    iFrameInterval = tm2 - tm1;
                } else {

                    iframeExtractor.seekTo(tm2, MediaExtractor.SEEK_TO_NEXT_SYNC);
                    chunkSize = iframeExtractor.readSampleData(buffers, 0);
                    tm2 = iframeExtractor.getSampleTime();

                    if (chunkSize > 0 && isKeyFrame(iframeExtractor)) {
                        iFrameInterval = tm2 - tm1;
                    }
                }
            }

            iframeExtractor.release();
        }

        while (!outputDone) {

            if (!inputDone) {

                int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {

                    ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];

                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    if (chunkSize < 0) {
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        if (VERBOSE)
                            Log.e(TAG, "sent input EOS");

                    } else {

                        if (extractor.getSampleTrackIndex() != trackIndex) {
                            Log.e(TAG, "WEIRD: got sample from track " + extractor.getSampleTrackIndex() + ", " + "expected " + trackIndex);
                        }

                        long presentationTimeUs = extractor.getSampleTime();
                        boolean isKeyFrame = isKeyFrame(extractor);
                        if (iFrameInterval == -1) {
                            if (isKeyFrame) {
                                if (firstKeyFramePTS == 0)
                                    firstKeyFramePTS = presentationTimeUs;
                                else {
                                    iFrameInterval = presentationTimeUs - firstKeyFramePTS;
                                    Log.e(TAG, "key frames interval: " + iFrameInterval);
                                }
                            }
                        } else {
                            if (isKeyFrame)
                                nextKeyFrmePTS = presentationTimeUs + iFrameInterval;
                        }

                        decoder.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0 /*flags*/);
                        if (VERBOSE) {
                            Log.e(TAG, "submitted frame " + inputChunk + " to dec, size=" + chunkSize + ", pts :" + presentationTimeUs);
                        }

                        inputChunk++;

                        boolean seeked = false;
                        if (seekInterval != -1) {
                            if (nextFrameTime > nextKeyFrmePTS) {
                                // 跳到nextFrameTime，根据间隔时间
                                extractor.seekTo(nextFrameTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                                Log.e(TAG, "seek to next key frame:" + nextFrameTime);
                                seeked = true;
                            }
                        }

                        if (!seeked){
                            extractor.advance();
                        }

                    }

                } else {

                    if (VERBOSE)
                        Log.d(TAG, "input buffer not available");
                }
            }

            if (!outputDone) {

                int decoderStatus = decoder.dequeueOutputBuffer(info, TIMEOUT_USEC);

                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {

                    if (VERBOSE)
                        Log.d(TAG, "no output from decoder available");

                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {

                    if (VERBOSE)
                        Log.d(TAG, "decoder output buffers changed");

                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                    MediaFormat newFormat = decoder.getOutputFormat();
                    if (VERBOSE)
                        Log.d(TAG, "decoder output format changed: " + newFormat);

                } else if (decoderStatus < 0) {

                    throw new Exception("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus);

                } else { // decoderStatus >= 0

                    if (VERBOSE)
                        Log.d(TAG, "surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        if (VERBOSE)
                            Log.d(TAG, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0);

                    if (doRender && seekInterval != -1) {
                        if (info.presentationTimeUs < nextFrameTime) {
                            Log.e(TAG, "ignore a frame ...");
                            doRender = false;
                        } else {

                            nextFrameTime += seekInterval;
                        }
                    }

                    decoder.releaseOutputBuffer(decoderStatus, doRender);

                    if (doRender) {

                        Log.e(TAG, "get a frame :" + decodeCount);

                        outputSurface.awaitNewImage();
                        outputSurface.drawImage(true);

                        long startWhen = System.nanoTime();
                        Bitmap bmp = outputSurface.getFrameBitmap();

                        listener.onFrame(decodeCount, bmp, info.presentationTimeUs);

                        frameSaveTime += System.nanoTime() - startWhen;

                        decodeCount++;

                        if (listener.getRequiredFrameCount() > 0 && decodeCount >= listener.getRequiredFrameCount()) {
                            break;
                        }
                    }
                }
            }
        }

        Log.d(TAG, "Saving " + decodeCount + " frames took " + (frameSaveTime / decodeCount / 1000) + " us per frame");
    }

    private static class CodecOutputSurface implements SurfaceTexture.OnFrameAvailableListener {

        private STextureRender mTextureRender;
        private SurfaceTexture mSurfaceTexture;
        private Surface mSurface;

        private EGLDisplay mEGLDisplay = EGL14.EGL_NO_DISPLAY;
        private EGLContext mEGLContext = EGL14.EGL_NO_CONTEXT;
        private EGLSurface mEGLSurface = EGL14.EGL_NO_SURFACE;
        int mWidth;
        int mHeight;

        private Object mFrameSyncObject = new Object();     // guards mFrameAvailable
        private boolean mFrameAvailable;

        private ByteBuffer mPixelBuf;                       // used by saveFrame()

        public CodecOutputSurface(int width, int height) {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException();
            }
            mWidth = width;
            mHeight = height;

            eglSetup();
            makeCurrent();
            setup();
        }

        private void setup() {
            mTextureRender = new STextureRender();
            mTextureRender.surfaceCreated();

            if (VERBOSE)
                Log.d(TAG, "textureID=" + mTextureRender.getTextureId());

            mSurfaceTexture = new SurfaceTexture(mTextureRender.getTextureId());

            mSurfaceTexture.setOnFrameAvailableListener(this);

            mSurface = new Surface(mSurfaceTexture);

            mPixelBuf = ByteBuffer.allocateDirect(mWidth * mHeight * 4);
            mPixelBuf.order(ByteOrder.LITTLE_ENDIAN);
        }

        private void eglSetup() {

            mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
            if (mEGLDisplay == EGL14.EGL_NO_DISPLAY) {
                throw new RuntimeException("unable to get EGL14 display");
            }

            int[] version = new int[2];
            if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
                mEGLDisplay = null;
                throw new RuntimeException("unable to initialize EGL14");
            }

            int[] attribList = {
                    EGL14.EGL_RED_SIZE, 8,
                    EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8,
                    EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_NONE
            };

            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!EGL14.eglChooseConfig(mEGLDisplay, attribList, 0, configs, 0, configs.length, numConfigs, 0)) {
                throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
            }

            int[] attrib_list = {
                    EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL14.EGL_NONE
            };

            mEGLContext = EGL14.eglCreateContext(mEGLDisplay, configs[0], EGL14.EGL_NO_CONTEXT, attrib_list, 0);
            checkEglError("eglCreateContext");
            if (mEGLContext == null) {
                throw new RuntimeException("null context");
            }

            int[] surfaceAttribs = {
                    EGL14.EGL_WIDTH, mWidth,
                    EGL14.EGL_HEIGHT, mHeight,
                    EGL14.EGL_NONE
            };

            mEGLSurface = EGL14.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttribs, 0);
            checkEglError("eglCreatePbufferSurface");
            if (mEGLSurface == null) {
                throw new RuntimeException("surface was null");
            }
        }

        public void release() {

            if (mEGLDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface);
                EGL14.eglDestroyContext(mEGLDisplay, mEGLContext);
                EGL14.eglReleaseThread();
                EGL14.eglTerminate(mEGLDisplay);
            }

            mEGLDisplay = EGL14.EGL_NO_DISPLAY;
            mEGLContext = EGL14.EGL_NO_CONTEXT;
            mEGLSurface = EGL14.EGL_NO_SURFACE;

            mSurface.release();

            mTextureRender = null;
            mSurface = null;
            mSurfaceTexture = null;
        }

        public void makeCurrent() {
            if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
                throw new RuntimeException("eglMakeCurrent failed");
            }
        }

        public Surface getSurface() {
            return mSurface;
        }

        public void awaitNewImage() {
            final int TIMEOUT_MS = 2500;

            synchronized (mFrameSyncObject) {
                while (!mFrameAvailable) {
                    try {
                        // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                        // stalling the test if it doesn't arrive.
                        mFrameSyncObject.wait(TIMEOUT_MS);
                        if (!mFrameAvailable) {
                            // TODO: if "spurious wakeup", continue while loop
                            throw new RuntimeException("frame wait timed out");
                        }
                    } catch (InterruptedException ie) {
                        // shouldn't happen
                        throw new RuntimeException(ie);
                    }
                }
                mFrameAvailable = false;
            }

            GLHelper.checkGlError(TAG, "before updateTexImage");
            mSurfaceTexture.updateTexImage();
        }

        public void drawImage(boolean invert) {
            mTextureRender.drawFrame(mSurfaceTexture, invert);
        }

        @Override
        public void onFrameAvailable(SurfaceTexture st) {
            if (VERBOSE)
                Log.d(TAG, "new frame available");

            synchronized (mFrameSyncObject) {
                if (mFrameAvailable) {
                    throw new RuntimeException("mFrameAvailable already set, frame could be dropped");
                }
                mFrameAvailable = true;
                mFrameSyncObject.notifyAll();
            }
        }

        public Bitmap getFrameBitmap() throws IOException {
            mPixelBuf.rewind();
            GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mPixelBuf);

            Bitmap bmp = null;
            bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            mPixelBuf.rewind();
            bmp.copyPixelsFromBuffer(mPixelBuf);

            return bmp;
        }

        private void checkEglError(String msg) {
            int error;
            if ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
                throw new RuntimeException(msg + ": EGL error: 0x" + Integer.toHexString(error));
            }
        }
    }

    private static class STextureRender {
        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;

        private final float[] mTriangleVerticesData = {-1f, -1f, 0, 1f, 0f, 1f, -1f, 0f, 0f, 0f, -1f, 1f, 0, 1f, 1f, 1f, 1f, 0, 0.f, 1f,};

        private FloatBuffer mTriangleVertices;

        private static final String VERTEX_SHADER =
                "uniform mat4 uMVPMatrix;\n" +
                "uniform mat4 uSTMatrix;\n" +
                "attribute vec4 aPosition;\n" +
                "attribute vec4 aTextureCoord;\n" +
                "varying vec2 vTextureCoord;\n" +
                "void main() {\n" +
                "    gl_Position = uMVPMatrix * aPosition;\n" +
                "    vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                "}\n";

        private static final String FRAGMENT_SHADER =
                "#extension GL_OES_EGL_image_external : require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";

        private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private float[] mMatrix = new float[16];
        private float[] identifyMatrix = new float[16];


        private int mProgram;
        private int mTextureID = -12345;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;

        STextureRender() {
            mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMatrix, 0);
        }

        int getTextureId() {
            return mTextureID;
        }

        public void drawFrame(SurfaceTexture st, boolean invert) {

            GLHelper.checkGlError(TAG, "onDrawFrame start");
            st.getTransformMatrix(mSTMatrix);

            st.getTransformMatrix(mMatrix);
            if(invert){
                Matrix.setIdentityM(identifyMatrix, 0);
                Matrix.translateM(identifyMatrix, 0, 1, 1, 0);
                Matrix.rotateM(identifyMatrix, 0, 180, 0, 0, 1);
                Matrix.multiplyMM(mSTMatrix, 0, identifyMatrix, 0, mMatrix,0);
            }

            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            GLES20.glUseProgram(mProgram);
            GLHelper.checkGlError(TAG, "glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle,
                                         3,
                                         GLES20.GL_FLOAT,
                                         false,
                                         TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                                         mTriangleVertices);
            GLHelper.checkGlError(TAG, "glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            GLHelper.checkGlError(TAG, "glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle,
                                         2,
                                         GLES20.GL_FLOAT,
                                         false,
                                         TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                                         mTriangleVertices);
            GLHelper.checkGlError(TAG, "glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            GLHelper.checkGlError(TAG, "glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLHelper.checkGlError(TAG, "glDrawArrays");

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        }

        public void surfaceCreated() {

            mProgram = GLHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
            if (mProgram == 0) {
                throw new RuntimeException("failed creating program");
            }

            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkLocation(maPositionHandle, "aPosition");
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkLocation(maTextureHandle, "aTextureCoord");

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkLocation(muMVPMatrixHandle, "uMVPMatrix");
            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
            checkLocation(muSTMatrixHandle, "uSTMatrix");

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
            GLHelper.checkGlError(TAG, "glBindTexture mTextureID");

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLHelper.checkGlError(TAG, "glTexParameter");
        }

        public static void checkLocation(int location, String label) {
            if (location < 0) {
                throw new RuntimeException("Unable to locate '" + label + "' in program");
            }
        }
    }
}