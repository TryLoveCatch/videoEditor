package com.estrongs.android.pop.video.edit.cut;

import com.estrongs.android.pop.video.edit.view.frames.VideoFrameExtractor;
import com.estrongs.android.pop.video.edit.view.frames.VideoFrames;
import com.estrongs.android.pop.video.util.BitmapUtil;
import com.estrongs.android.pop.video.util.DensityUtil;

import android.graphics.Bitmap;
import android.media.MediaFormat;
import android.util.Log;

/**
 * Created by lipeng21 on 2017/6/27.
 */

public class CutManager {
    private String mVideoFile;
    private VideoFrames mVideoFrames;
    private long mStartTime = -1L;
    private long mEndTime = -1L;

    public CutManager(String pVideoFile){
        this.mVideoFile = pVideoFile;
        mVideoFrames = new VideoFrames();
        mVideoFrames.setFrameCount(10);
    }


    public VideoFrames getVideoFrames(){
        return mVideoFrames;
    }
    public void startExtraFrames() {
        VideoFrameExtractor extractor = new VideoFrameExtractor(mVideoFile, 320, 320, getFrameExtractorListener());
        extractor.start(false);
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(long pStartTime) {
        this.mStartTime = pStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(long pEndTime) {
        this.mEndTime = pEndTime;
    }

    public boolean isTimeValid(){
        return mStartTime == -1 || mEndTime == -1 ? false : true;
    }

    private VideoFrameExtractor.Listener getFrameExtractorListener() {
        return new VideoFrameExtractor.Listener() {

            int frameThumbWidth = DensityUtil.dip2px(40);
            int frameThumbHeight = DensityUtil.dip2px(40);

            @Override
            public void onFormat(final MediaFormat format) {

                mVideoFrames.setFormat(format);

            }

            @Override
            public void onFrame(int idx, Bitmap bitmap, long pts) {
                //                if(framesView.getVisibility() == View.GONE){
                //                    AndroidUtil.runOnUiThread(new Runnable() {
                //                        @Override
                //                        public void run() {
                //                            framesView.setVisibility(View.VISIBLE);
                //                        }
                //                    });
                //                }

                if (bitmap != null) {
                    Bitmap thumb = BitmapUtil.extractThumbnail(bitmap, frameThumbWidth, frameThumbHeight);
                    mVideoFrames.addFrame(thumb);
                    bitmap.recycle();
                    Log.e("videoFrameExtractorTest", "onFrame: " + idx + ", pts:" + pts);
                }
            }

            @Override
            public void onCompleted() {
                mVideoFrames.onFrameLoaded();
            }

            @Override
            public void onFailed(int error) {
            }

            @Override
            public int getRequiredFrameCount() {
                return 10;
            }
        };
    }

}
