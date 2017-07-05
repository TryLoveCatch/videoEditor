package com.estrongs.android.pop.video.opengl;

import com.estrongs.android.pop.video.R;
import com.estrongs.android.pop.video.util.AndroidUtil;

import android.opengl.GLES20;
import android.util.Log;

/**
 * Created by luofeng01 on 17/5/23.
 */

public class GLHelper {

    private static final String TAG = "GLHelper";

    public static void checkGlError(String TAG, String op) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
        if (vertexShader == 0) {
            return 0;
        }

        int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
        if (pixelShader == 0) {
            return 0;
        }

        int program = GLES20.glCreateProgram();
        GLHelper.checkGlError(TAG, "glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "Could not create program");
        }
        GLES20.glAttachShader(program, vertexShader);
        GLHelper.checkGlError(TAG, "glAttachShader");

        GLES20.glAttachShader(program, pixelShader);
        GLHelper.checkGlError(TAG, "glAttachShader");

        GLES20.glLinkProgram(program);

        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "Could not link program: ");
            Log.e(TAG, GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    private static int loadShader(int shaderType, String source) {

        Log.d(TAG, "programe:");
        Log.d(TAG, source);

        int shader = GLES20.glCreateShader(shaderType);
        GLHelper.checkGlError(TAG, "glCreateShader type=" + shaderType);
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Could not compile shader " + shaderType + ":");
            Log.e(TAG, " " + GLES20.glGetShaderInfoLog(shader));
            Log.e(TAG, "source:");
            Log.e(TAG, source);
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public static String getVectexShaderString(int rawId) {
        return AndroidUtil.getRawString(rawId);
    }

    public static String getFragmentShaderString(int filterId) {
        String template = AndroidUtil.getRawString(R.raw.videoeditor_fragment_shader);
        if (filterId == 0)
            return template;

        String filterShade = AndroidUtil.getRawString(filterId);
        String shader = template;

        String beginMark = "/*begin*/";
        String endMark = "/*end*/";
        String declareMark = "/*uniforms*/";
        String functionMark = "/*functuons*/";

        int applyFilterBegin = template.indexOf(beginMark);
        int applyFilterEnd = template.indexOf(endMark);
        int declareMarkBegin = template.indexOf(declareMark);


        int functionBegin = filterShade.indexOf(functionMark);

        {
            StringBuilder builder = new StringBuilder();

            builder.append(template.substring(0, declareMarkBegin));
            if (functionBegin != -1) {
                builder.append(filterShade.substring(0, functionBegin));
            }

            builder.append(template.substring(declareMarkBegin + declareMark.length(), applyFilterBegin));

            if (functionBegin == -1)
                builder.append(filterShade);
            else
                builder.append(filterShade.substring(functionBegin + functionMark.length()));

            builder.append(template.substring(applyFilterEnd + endMark.length()));
            shader = builder.toString();
        }

        return shader;
    }
}
