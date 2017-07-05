package com.estrongs.android.pop.video.edit.cut;

import java.io.File;

import com.estrongs.android.pop.video.BaseFragment;
import com.estrongs.android.pop.video.R;
import com.estrongs.android.pop.video.player.VideoTimeBar;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ClippingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.FileDataSource;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

public class CutActivity extends AppCompatActivity {

    private String videoSrcFile;
    private SimpleExoPlayerView videoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cut);

        videoPlayer = (SimpleExoPlayerView) findViewById(R.id.videoView);

        Intent intent = getIntent();
        videoSrcFile = intent.getStringExtra("video");

        if (videoSrcFile == null) {
            finish();
            return;
        }


        initPlayer();
        initEdit();
    }


    private void initPlayer(){
        VideoTimeBar timeBar = (VideoTimeBar) findViewById(R.id.exo_progress);

        timeBar.setListener(new VideoTimeBar.Listener() {
            @Override
            public void onPositionChanged(long position) {

            }
        });


        Uri uri = Uri.fromFile(new File(videoSrcFile));
        DataSpec dataSpec = new DataSpec(uri);
        final FileDataSource fileDataSource = new FileDataSource();
        try {
            fileDataSource.open(dataSpec);
        } catch (Exception e) {
            e.printStackTrace();
        }

        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return fileDataSource;
            }
        };

        DefaultExtractorsFactory extractorFactory = new DefaultExtractorsFactory();
        MediaSource videoSource = new ExtractorMediaSource(fileDataSource.getUri(), factory, extractorFactory, null,
                null);
        ClippingMediaSource clippingMediaSource = new ClippingMediaSource(videoSource, 0, C.TIME_END_OF_SOURCE);

        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        videoPlayer.setPlayer(player);

        player.prepare(clippingMediaSource);
        player.setPlayWhenReady(false);
    }

    private void initEdit(){
        Bundle tBundle = new Bundle();
        tBundle.putString("video", videoSrcFile);
        Fragment fragment = Fragment.instantiate(this, CutFragment.class.getName(), tBundle);

        FragmentTransaction t = getSupportFragmentManager().beginTransaction();
        t.replace(R.id.content_frame, fragment);
        t.commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        videoPlayer.getPlayer().setPlayWhenReady(false);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        BaseFragment outer = (BaseFragment)fm.findFragmentById(R.id.content_frame);
        if(outer != null){
            if(outer.onBackPressed()){
                return;
            }
        }
        super.onBackPressed();
    }

    public void seekTo(long position){
        videoPlayer.getPlayer().setPlayWhenReady(false);
        videoPlayer.getPlayer().seekTo(position / 1000);
    }
}
