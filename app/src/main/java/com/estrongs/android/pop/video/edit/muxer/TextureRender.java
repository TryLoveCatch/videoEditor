package com.estrongs.android.pop.video.edit.muxer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import com.estrongs.android.pop.video.MyApplication;
import com.estrongs.android.pop.video.R;
import com.estrongs.android.pop.video.edit.overlay.OverlayDrawer;
import com.estrongs.android.pop.video.filter.IVideoFilter;
import com.estrongs.android.pop.video.filter.VideoFilterProvider;
import com.estrongs.android.pop.video.opengl.GLHelper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

public class TextureRender {
    private static final String TAG = "TextureRender";

    private static final int FLOAT_SIZE_BYTES = 4;
    private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
    private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
    private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
    private final float[] mTriangleVerticesData = {
            -0.5f, -0.5f, 0f, 0f, 0f,
            0.5f, -0.5f, 0f, 1f, 0f,
            -0.5f, 0.5f, 0f, 0f, 1f,
            0.5f, 0.5f, 0f, 1f, 1f,};

    private FloatBuffer mTriangleVertices;
    private float[] mMVPMatrix = new float[16];
    private float[] mSTMatrix = new float[16];

    private int mTextureID = -12345;
    private int mOverlayTextureId;

    private int mProgram;

    private int glsluMVPMatrix;
    private int glsluSTMatrix;
    private int glslAPosition;
    private int glslATexture;
    private int glslOverlayCoord;

    private int glslOverFlag;
    private int glslOverlayTexture;

    private OverlayEffect overlayEffect = new OverlayEffect();

    VideoFilterProvider filterProvider;





    private Context context;
    private final float[] sPos={
            -1.0f,1.0f,
            -1.0f,-1.0f,
            1.0f,1.0f,
            1.0f,-1.0f
    };

    private final float[] sCoord={
            0.0f,0.0f,
            0.0f,1.0f,
            1.0f,0.0f,
            1.0f,1.0f,
            //            0.5f,0.5f,
            //            0.5f,0.5f,
            //            0.5f,0.5f,
            //            0.5f,0.5f,
    };
    private FloatBuffer bufferPos;
    private FloatBuffer bufferCoord;

    private int program;

    private final float[] matrix = new float[16];
    private static final String U_MATRIX = "u_Matrix";
    private int uMatrix;

    private static final String A_COORDINATE = "a_Coordinate";//和.glsl文件里面定义的一样
    private int attribCoord;
    private static final String A_POSITION = "a_Position";//和.glsl文件里面定义的一样
    private int attribPosition;

    private static final String U_TEXTURE = "u_Texture";//和.glsl文件里面定义的一样
    private int uTexture;
    private int mTexture;
    private Bitmap mBitmap;

    public TextureRender() {
        mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);

        Matrix.setIdentityM(mSTMatrix, 0);


        bufferPos = ByteBuffer
                .allocateDirect(sPos.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        bufferPos.put(sPos);

        bufferCoord = ByteBuffer
                .allocateDirect(sCoord.length * FLOAT_SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();

        bufferCoord.put(sCoord);


        mBitmap = BitmapFactory.decodeResource(MyApplication.mInstance.getResources(), R.drawable.fengj);
    }

    public int getTextureId() {
        return mTextureID;
    }

    public void addDrawer(OverlayDrawer drawer) {
        overlayEffect.addDrawer(drawer);
    }

    private void applyOverlayEffect(long pts) {
        overlayEffect.applayOverlay(pts, mOverlayTextureId, glslOverlayTexture, glslOverFlag, glslOverlayCoord);
    }

    public void surfaceCreated(int width, int height, int rotation) {

        String VERTEX_SHADER = GLHelper.getVectexShaderString(R.raw.videoeditor_verctex_shader);
        String FRAGMENT_SHADER = GLHelper.getFragmentShaderString(0);

        mProgram = GLHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (mProgram == 0) {
            throw new RuntimeException("failed creating program");
        }

        glslAPosition = GLES20.glGetAttribLocation(mProgram, "aPosition");
        GLHelper.checkGlError(TAG, "glGetAttribLocation aPosition");
        if (glslAPosition == -1) {
            throw new RuntimeException("Could not get attrib location for aPosition");
        }

        glslATexture = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        GLHelper.checkGlError(TAG, "glGetAttribLocation aTextureCoord");
        if (glslATexture == -1) {
            throw new RuntimeException("Could not get attrib location for aTextureCoord");
        }

        glsluMVPMatrix = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        GLHelper.checkGlError(TAG, "glGetUniformLocation uMVPMatrix");
        if (glsluMVPMatrix == -1) {
            throw new RuntimeException("Could not get attrib location for uMVPMatrix");
        }

        glsluSTMatrix = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
        GLHelper.checkGlError(TAG, "glGetUniformLocation uSTMatrix");
        if (glsluSTMatrix == -1) {
            throw new RuntimeException("Could not get attrib location for uSTMatrix");
        }

        glslOverlayTexture = GLES20.glGetUniformLocation(mProgram, "oTexture");
        GLHelper.checkGlError(TAG, "glGetUniformLocation oTexture");
        if (glslOverlayTexture == -1) {
            throw new RuntimeException("Could not get attrib location for overlayFlag");
        }

        glslOverFlag = GLES20.glGetUniformLocation(mProgram, "overlayFlag");
        GLHelper.checkGlError(TAG, "glGetUniformLocation overlayFlag");
        if (glslOverFlag == -1) {
            throw new RuntimeException("Could not get attrib location for overlayFlag");
        }

        glslOverlayCoord = GLES20.glGetAttribLocation(mProgram, "aOverlayCoord");
        GLHelper.checkGlError(TAG, "glGetAttribLocation aOverlayCoord");
        if (glslOverlayCoord == -1) {
            throw new RuntimeException("Could not get attrib location for aOverlayCoord");
        }

        int[] textures = new int[3];
        GLES20.glGenTextures(3, textures, 0);

        mTextureID = textures[0];
        mOverlayTextureId = textures[1];
        mTexture = textures[2];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLHelper.checkGlError(TAG, "glBindTexture mTextureID");

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLHelper.checkGlError(TAG, "glTexParameter");


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mOverlayTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        overlayEffect.onSurfaceCreated(width, height, rotation);

        if (filterProvider != null)
            filterProvider.initFilters();



        VERTEX_SHADER = GLHelper.getVectexShaderString(R.raw.simple_vertex_shader2);
        FRAGMENT_SHADER = GLHelper.getVectexShaderString(R.raw.simple_fragment_shader2);

        program = GLHelper.createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (program == 0) {
            throw new RuntimeException("failed creating program");
        }

        attribCoord = GLES20.glGetAttribLocation(program, A_COORDINATE);//获取 a_Color 在shader 中的指针
        GLHelper.checkGlError(TAG, "glGetAttribLocation attribCoord");
        attribPosition = GLES20.glGetAttribLocation(program, A_POSITION);//获取 a_Position 在 shader 中的指针
        GLHelper.checkGlError(TAG, "glGetAttribLocation attribPosition");
        uTexture = GLES20.glGetUniformLocation(program, U_TEXTURE);//获取 a_Position 在 shader 中的指针
        GLHelper.checkGlError(TAG, "glGetUniformLocation uTexture");
        uMatrix = GLES20.glGetUniformLocation(program, U_MATRIX);
        GLHelper.checkGlError(TAG, "glGetUniformLocation uMatrix");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
        GLHelper.checkGlError(TAG, "glBindTexture attribCoord");
        //设置缩小过滤为使用纹理中坐标最接近的一个像素的颜色作为需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR);
        //设置放大过滤为使用纹理中坐标最接近的若干个颜色，通过加权平均算法得到需要绘制的像素颜色
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR);
        //设置环绕方向S，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE);
        //设置环绕方向T，截取纹理坐标到[1/2n,1-1/2n]。将导致永远不会与border融合
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE);
        //根据以上指定的参数，生成一个2D纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, mBitmap, 0);
        GLHelper.checkGlError(TAG, "texImage2D attribCoord");


        final float tRatio = width > height ? 1.0f * width / height : 1.0f * height / width;
        if(width > height){
            Matrix.orthoM(matrix, 0, -tRatio, tRatio, -1f, 1f, -1f, 1f);
        }else{
            Matrix.orthoM(matrix, 0, -1f, 1f, -tRatio, tRatio,  -1f, 1f);
        }
    }

    public void drawFrame(SurfaceTexture st, long pts) {

        GLHelper.checkGlError(TAG, "onDrawFrame start");
        st.getTransformMatrix(mSTMatrix);

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);



        GLES20.glUseProgram(program);//使用这个程序
        GLHelper.checkGlError(TAG, "glUseProgram");

        GLES20.glUniformMatrix4fv(uMatrix, 1, false, matrix, 0);

        bufferPos.position(0);
        GLES20.glVertexAttribPointer(attribPosition, 2, GLES20.GL_FLOAT, false, 0, bufferPos);
        GLHelper.checkGlError(TAG, "glVertexAttribPointer attribPosition");
        GLES20.glEnableVertexAttribArray(attribPosition);
        GLHelper.checkGlError(TAG, "glEnableVertexAttribArray attribPosition");

        bufferCoord.position(0);
        GLES20.glVertexAttribPointer(attribCoord, 2, GLES20.GL_FLOAT, false, 0, bufferCoord);
        GLHelper.checkGlError(TAG, "glVertexAttribPointer attribCoord");
        GLES20.glEnableVertexAttribArray(attribCoord);
        GLHelper.checkGlError(TAG, "glEnableVertexAttribArray attribCoord");


        GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
        GLHelper.checkGlError(TAG, "glActiveTexture attribCoord");
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
//        GLHelper.checkGlError(TAG, "glBindTexture attribCoord");
//        GLES20.glUniform1i(uTexture, 2);
//        GLHelper.checkGlError(TAG, "glUniform1i attribCoord");

        /**
         * 将2改成0
         *
         * 这个修改，可以实现背景图就是每一帧的图像 实时变化
         * 也就是背景的纹理对象和播放器的纹理对象用的是同一个纹理单元
         */
        GLES20.glUniform1i(uTexture, 0);
        GLHelper.checkGlError(TAG, "glUniform1i attribCoord");

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4);
        GLHelper.checkGlError(TAG, "glDrawArrays attribCoord");


        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc (GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        IVideoFilter videoFilter = null;
        if (filterProvider != null) {
            videoFilter = filterProvider.getFilter(pts);
        }

        int program = mProgram;

        if (videoFilter != null) {
            program = videoFilter.getProgram();
        }

        GLES20.glUseProgram(program);
        GLHelper.checkGlError(TAG, "glUseProgram");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
        GLES20.glVertexAttribPointer(glslAPosition,
                                     3,
                                     GLES20.GL_FLOAT,
                                     false,
                                     TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                                     mTriangleVertices);
        GLHelper.checkGlError(TAG, "glVertexAttribPointer maPosition");

        GLES20.glEnableVertexAttribArray(glslAPosition);
        GLHelper.checkGlError(TAG, "glEnableVertexAttribArray glslAPosition");

        mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
        GLES20.glVertexAttribPointer(glslATexture,
                                     2,
                                     GLES20.GL_FLOAT,
                                     false,
                                     TRIANGLE_VERTICES_DATA_STRIDE_BYTES,
                                     mTriangleVertices);
        GLHelper.checkGlError(TAG, "glVertexAttribPointer glslATexture");
        GLES20.glEnableVertexAttribArray(glslATexture);
        GLHelper.checkGlError(TAG, "glEnableVertexAttribArray glslATexture");

        Matrix.setIdentityM(mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(glsluMVPMatrix, 1, false, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(glsluSTMatrix, 1, false, mSTMatrix, 0);

        if (videoFilter != null) {
            videoFilter.applyFilter(pts);
        }

        applyOverlayEffect(pts);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLHelper.checkGlError(TAG, "glDrawArrays");
        GLES20.glFinish();
    }

    public void setOverlayDrawScale(float scale) {
        overlayEffect.setOverlayDrawScale(scale);
    }

    public void setFilterProvider(VideoFilterProvider filterProvider) {
        this.filterProvider = filterProvider;
    }
}
