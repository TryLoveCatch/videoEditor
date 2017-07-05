package com.estrongs.android.pop.video.edit.muxer;

public interface ProgressListener {

    void onStart();
    void onProgress(int progress);
    void onCompleted();
    void onError();
}
