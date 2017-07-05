package com.estrongs.android.pop.video.edit;

import com.estrongs.android.pop.video.edit.cut.CutManager;
import com.estrongs.android.pop.video.edit.muxer.IMuxerController;
import com.estrongs.android.pop.video.edit.muxer.Muxer;
import com.estrongs.android.pop.video.edit.muxer.ProgressListener;
import com.estrongs.android.pop.video.edit.view.frames.VideoFrames;
import com.estrongs.android.pop.video.opengl.RenderHelper;

/**
 * Created by lipeng21 on 2017/6/27.
 */

public class EditManager {

    private String mSrcFile;

    //剪切
    private CutManager mCutManager;

    // 合成
    private Muxer.Builder mMuxerBuilder;
    private Muxer mMuxer;

    // 渲染
    private RenderHelper mRenderHelper;

    public EditManager(String pSrcFile){
        this.mSrcFile = pSrcFile;
        EditCacheManager.getInstance().addManager(mSrcFile, this);
    }


    //=================================cut start====================================
    public void initCutManager(){
        if(mCutManager==null){
            mCutManager = new CutManager(mSrcFile);
        }
    }

    public VideoFrames getVideoFrames(){
        return mCutManager.getVideoFrames();
    }

    public void startExtraFrames() {
        mCutManager.startExtraFrames();
    }

    public void setCutTime(long pStartTime, long pEndTime){
        mCutManager.setEndTime(pEndTime);
        mCutManager.setStartTime(pStartTime);
    }

    //=================================cut end====================================

    //=================================muxer start====================================
    public void initMuxerManager(String pSrcFile, String pDstFile){
        mRenderHelper = new RenderHelper();
//        tRenderHelper.setOverlayDrawScale(rate);
//        for (VideoDrawItem item : videoDrawView.getItems()) {
//            tRenderHelper.addOverlayDrawer(item.createOverlayDrawer());
//        }

//        tRenderHelper.setFilterProvider(filterProvider);

        mMuxerBuilder = new Muxer.Builder()
                .setSrcFile(pSrcFile)
                .setDstFile(pDstFile)
                .setRenderHelper(mRenderHelper)
                .setMuxerController(new IMuxerController() {
            @Override
            public boolean isDropFrame(long presentationTime) {
                if(mCutManager==null){
                    return false;
                }

                if(!mCutManager.isTimeValid()){
                    return false;
                }

                long tStartTime = mCutManager.getStartTime();
                long tEndTime = mCutManager.getEndTime();


                if (presentationTime < tStartTime) {
                    return true;
                }

                if (presentationTime > tEndTime) {
                    return true;
                }
                return false;
            }
        });
    }

    public void setMuxerProgressListener(ProgressListener pProgressListener){
        mMuxerBuilder.setProgressListener(pProgressListener);
    }

    public boolean startMuxer(boolean sync) {
        mMuxer = mMuxerBuilder.create();
        if(mMuxer == null){
            return false;
        }else{
            mMuxer.start(sync);
            return true;
        }
    }
    //=================================muxer end====================================
}
