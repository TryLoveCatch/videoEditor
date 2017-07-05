package com.estrongs.android.pop.video;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.estrongs.android.pop.video.edit.EditCacheManager;
import com.estrongs.android.pop.video.edit.EditFragment;
import com.estrongs.android.pop.video.edit.EditManager;
import com.estrongs.android.pop.video.edit.muxer.ProgressListener;
import com.estrongs.android.pop.video.player.VideoTimeBar;
import com.estrongs.android.pop.video.util.AndroidUtil;
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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private String videoSrcFile;
    private SimpleExoPlayerView videoPlayer;

    private long mStartTime;
    private long mEndTime;

    private EditManager mEditManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("视频编辑");

//        toolbar.setNavigationIcon(R.drawable.chevron_left);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        videoPlayer = (SimpleExoPlayerView) findViewById(R.id.videoView);

        Intent intent = getIntent();
        videoSrcFile = intent.getStringExtra("video");

        if (videoSrcFile == null) {
            videoSrcFile = "/sdcard/2.mp4";
        }


        initPlayer();
        initEdit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_save:
                save();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void initPlayer(){
        VideoTimeBar timeBar = (VideoTimeBar) findViewById(R.id.exo_progress);

        timeBar.setListener(new VideoTimeBar.Listener() {
            @Override
            public void onPositionChanged(long position) {

            }
        });




        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
        videoPlayer.setPlayer(player);

        player.prepare(createMediaSource(0, C.TIME_END_OF_SOURCE));
        player.setPlayWhenReady(false);
    }

    private void initEdit(){
        mEditManager = EditCacheManager.getInstance().getManager(videoSrcFile);
        if(mEditManager==null) {
            mEditManager = new EditManager(videoSrcFile);
        }



        Fragment fragment = Fragment.instantiate(this, EditFragment.class.getName(), null);

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
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.getPlayer().release();
        EditCacheManager.getInstance().removeManager(videoSrcFile);
        mEditManager = null;
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

    private MediaSource createMediaSource(long startTime, long endTime){
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
        ClippingMediaSource clippingMediaSource = new ClippingMediaSource(videoSource, startTime, endTime);
        return clippingMediaSource;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            mStartTime = data.getLongExtra("startTime", 0L);
            mEndTime = data.getLongExtra("endTime", C.TIME_END_OF_SOURCE);
            videoPlayer.getPlayer().prepare(createMediaSource(
                    mStartTime, mEndTime));
            videoPlayer.getPlayer().setPlayWhenReady(false);
        }
    }

    public String getVideoSrcFile(){
        return videoSrcFile;
    }

    private String getSaveVideoFilePath() {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        return "/sdcard/video_editor/" + format.format(new Date()) + ".mp4";
    }
    private void save(){
        if(mEndTime == 0 && mEndTime <= mStartTime){
            return;
        }

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("正在合成视频...");
        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()==0) {
                    return true;
                }

                return false;
            }
        });

        dialog.show();

        mEditManager.initMuxerManager(videoSrcFile, getSaveVideoFilePath());

        mEditManager.setMuxerProgressListener(new ProgressListener() {
            @Override
            public void onStart() {
                Log.e("MainActivity", "onStart: " + Thread.currentThread().getName());
            }

            @Override
            public void onProgress(int progress) {
                Log.e("MainActivity", "onProgress: " + progress);

            }

            @Override
            public void onCompleted() {
                Log.e("MainActivity", "onCompleted");
                dialog.dismiss();
                AndroidUtil.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AndroidUtil.getApplication(), "视频已经合成", Toast.LENGTH_SHORT).show();

                    }
                });

            }

            @Override
            public void onError() {
                Log.e("MainActivity", "onError");
                AndroidUtil.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AndroidUtil.getApplication(), "视频合并失败", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                    }
                });
            }
        });

        mEditManager.startMuxer(false);
    }
}
