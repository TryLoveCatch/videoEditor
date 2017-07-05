package com.estrongs.android.pop.video;

import com.estrongs.android.pop.video.util.AndroidUtil;

import android.app.Application;

/**
 * Created by lipeng21 on 2017/6/27.
 */

public class MyApplication extends Application{

    public static MyApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        AndroidUtil.init(this);

    }
}
