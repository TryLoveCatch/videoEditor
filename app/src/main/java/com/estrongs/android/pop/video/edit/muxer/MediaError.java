package com.estrongs.android.pop.video.edit.muxer;

/**
 * Created by lipeng21 on 2017/6/29.
 */

public class MediaError {
    public static final int CODE_NO_AUDIO = 1;
    public static final int CODE_NO_VIDEO = 2;
    public static final int CODE_UNKNOWN = -1;

    public int code;
    public String msg;

    public MediaError(String msg){
        this.code = CODE_UNKNOWN;
        this.msg = msg;
    }
    public MediaError(int code, String msg){
        this.code = code;
        this.msg = msg;
    }
}
