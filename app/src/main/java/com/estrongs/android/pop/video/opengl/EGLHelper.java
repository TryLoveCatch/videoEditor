package com.estrongs.android.pop.video.opengl;

import static com.google.android.exoplayer2.mediacodec.MediaCodecInfo.TAG;

import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLExt;
import android.opengl.EGLSurface;
import android.util.Log;
import android.view.Surface;

/**
 * Created by lipeng21 on 2017/6/28.
 */

public class EGLHelper {
    private volatile static EGLHelper mInstance;

    public EGLDisplay mEglDisplay;
    public EGLConfig mEglConfig;
    public EGLSurface mEglSurface;
    public EGLContext mEglContext;

    private static final int EGL_RECORDABLE_ANDROID = 0x3142;

    private int red=8;
    private int green=8;
    private int blue=8;
    private int alpha=8;
    private int depth=16;
    private int renderType = EGL14.EGL_OPENGL_ES2_BIT;

    private EGLHelper(){

    }

    public static EGLHelper getInstance(){
        if(mInstance==null){
            synchronized (EGLHelper.class){
                if(mInstance==null){
                    mInstance = new EGLHelper();
                }
            }
        }
        return mInstance;
    }


    public void config(int red,int green,int blue,int alpha,int depth,int renderType){
        this.red=red;
        this.green=green;
        this.blue=blue;
        this.alpha=alpha;
        this.depth=depth;
        this.renderType=renderType;
    }

    public void init(Surface surface){
        init(surface, -1, -1);
    }

    public void init(Surface surface, int width, int height){
        //获取Display
        mEglDisplay=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (mEglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw new RuntimeException("unable to get EGL14 display");
        }

        int[] version=new int[2];    //主版本号和副版本号
        if (!EGL14.eglInitialize(mEglDisplay, version, 0, version, 1)) {
            mEglDisplay = null;
            throw new RuntimeException("unable to initialize EGL14");
        }
        //选择Config
        int[] attributes = new int[] {
                EGL14.EGL_RED_SIZE, red,  //指定RGB中的R大小（bits）
                EGL14.EGL_GREEN_SIZE, green, //指定G大小
                EGL14.EGL_BLUE_SIZE, blue,  //指定B大小
                EGL14.EGL_ALPHA_SIZE, alpha, //指定Alpha大小，以上四项实际上指定了像素格式
                EGL14.EGL_DEPTH_SIZE, depth, //指定深度缓存(Z Buffer)大小
                EGL14.EGL_RENDERABLE_TYPE, renderType, //指定渲染api版本, EGL14.EGL_OPENGL_ES2_BIT
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE };  //总是以EGL14.EGL_NONE结尾
        EGLConfig[] configs = new EGLConfig[1];
        int[] numConfigs = new int[1];
        if (!EGL14.eglChooseConfig(mEglDisplay, attributes, 0, configs, 0, configs.length,
                numConfigs, 0)) {
            throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
        }
        mEglConfig = configs[0];

        //创建Context
        int[] contextAttr=new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION,2,
                EGL14.EGL_NONE
        };
        mEglContext=EGL14.eglCreateContext(mEglDisplay,mEglConfig,EGL14.EGL_NO_CONTEXT,contextAttr, 0);
        checkEglError("eglCreateContext");
        if (mEglContext == null) {
            throw new RuntimeException("null context");
        }
        //创建Surface
        int[] surAttr;
        if(width == -1 && height == -1){
            surAttr = new int[] {
                    EGL14.EGL_NONE
            };
        }else {
            surAttr = new int[] {
                    EGL14.EGL_WIDTH, width,
                    EGL14.EGL_HEIGHT, height,
                    EGL14.EGL_NONE
            };
        }
        mEglSurface = EGL14.eglCreateWindowSurface(mEglDisplay,mEglConfig,surface,surAttr, 0);
        checkEglError("eglCreateWindowSurface");
        if (mEglSurface == null) {
            throw new RuntimeException("surface was null");
        }
        makeCurrent();
    }

    public void makeCurrent(){
        EGL14.eglMakeCurrent(mEglDisplay,mEglSurface,mEglSurface,mEglContext);
    }

    public void releaseEGLContext() {
        if (!EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)) {
            throw new RuntimeException("releaseEGLContext failed");
        }
    }
    public void release(){
        EGL14.eglMakeCurrent(mEglDisplay, EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        EGL14.eglDestroySurface(mEglDisplay, mEglSurface);
        EGL14.eglDestroyContext(mEglDisplay, mEglContext);
        EGL14.eglTerminate(mEglDisplay);

        mEglSurface = null;
        mEglDisplay = null;
        mEglConfig = null;
        mEglContext = null;
    }

    /**
     * 因为双缓存机制，需要调用这个方法，来显示
     *
     * 交换缓冲区数据，拷贝颜色缓冲到本地窗口，实现送显功能。
     *
     * 相当于提交数据到encoder
     * @return
     */
    public boolean swapBuffers() {
        return EGL14.eglSwapBuffers(mEglDisplay, mEglSurface);
    }

    /**
     * 设置当前帧在视频中的时间
     *
     * 单位纳秒
     *
     * @param nsecs
     */
    public void setPresentationTime(long nsecs) {
        EGLExt.eglPresentationTimeANDROID(mEglDisplay, mEglSurface, nsecs);
        checkEglError("eglPresentationTimeANDROID");
    }

    private void checkEglError(String msg) {
        boolean failed = false;
        int error;
        while ((error = EGL14.eglGetError()) != EGL14.EGL_SUCCESS) {
            Log.e(TAG, msg + ": EGL error: 0x" + Integer.toHexString(error));
            failed = true;
        }
        if (failed) {
            throw new RuntimeException("EGL error encountered (see log)");
        }
    }

}
