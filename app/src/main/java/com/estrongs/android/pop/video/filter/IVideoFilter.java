package com.estrongs.android.pop.video.filter;

public interface IVideoFilter {
    void init();
    int getProgram();
    void applyFilter(long pts);
}
