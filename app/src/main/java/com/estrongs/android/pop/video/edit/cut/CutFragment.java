package com.estrongs.android.pop.video.edit.cut;

import static android.app.Activity.RESULT_OK;

import com.estrongs.android.pop.video.BaseFragment;
import com.estrongs.android.pop.video.R;
import com.estrongs.android.pop.video.edit.EditCacheManager;
import com.estrongs.android.pop.video.edit.EditManager;
import com.estrongs.android.pop.video.edit.view.frames.VideoFrameExtractor;
import com.estrongs.android.pop.video.edit.view.frames.VideoFrames;
import com.estrongs.android.pop.video.edit.view.frames.VideoFramesView;
import com.estrongs.android.pop.video.util.AndroidUtil;
import com.estrongs.android.pop.video.util.BitmapUtil;
import com.estrongs.android.pop.video.util.DensityUtil;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lipeng21 on 2017/6/27.
 */

public class CutFragment extends BaseFragment{

    private VideoFramesView framesView;
    private String mVideoFile;


    private EditManager mEditManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState, R.layout.fragment_cut);
    }

    @Override
    public void initViewProperty() {
        mRoot.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        framesView = (VideoFramesView) mRoot.findViewById(R.id.frames);
        framesView.reset();
        framesView.setVideoFrames(mEditManager.getVideoFrames());
        framesView.addListener(new VideoFramesView.Listener() {
            @Override
            public void onSeekPositionChange(long pos) {
                onSeekTo(pos);
            }

            @Override
            public void onSeekTo(long pos) {
                ((CutActivity)getActivity()).seekTo(pos);
            }
        });
    }

    @Override
    public void initData() {
        Bundle tData = getArguments();
        if(tData!=null){
            mVideoFile = tData.getString("video");
        }

        if(TextUtils.isEmpty(mVideoFile)){
            getActivity().finish();
            return;
        }
        mEditManager = EditCacheManager.getInstance().getManager(mVideoFile);
        mEditManager.initCutManager();
        mEditManager.startExtraFrames();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        framesView.setVideoFrames(null);
    }

    private void save(){
        Intent intent = new Intent();
        long tStartTime = framesView.getStartTime();
        long tEndTime = framesView.getEndTime();
        mEditManager.setCutTime(tStartTime, tEndTime);
        intent.putExtra("startTime", tStartTime);
        intent.putExtra("endTime", tEndTime);
        getActivity().setResult(RESULT_OK, intent);
        getActivity().onBackPressed();
    }


}
