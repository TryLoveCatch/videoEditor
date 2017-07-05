package com.estrongs.android.pop.video.edit;

import com.estrongs.android.pop.video.BaseFragment;
import com.estrongs.android.pop.video.MainActivity;
import com.estrongs.android.pop.video.R;
import com.estrongs.android.pop.video.edit.cut.CutActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lipeng21 on 2017/6/27.
 */

public class EditFragment extends BaseFragment{

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState, R.layout.fragment_edit);
    }

    @Override
    public void initViewProperty() {
        mRoot.findViewById(R.id.fragment_edit_btn_cut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                openFragment(EditFragment.this, CutFragment.class, null);
                Intent intent = new Intent(getActivity(), CutActivity.class);
                intent.putExtra("video", ((MainActivity)getActivity()).getVideoSrcFile());
                startActivityForResult(intent, 1);
            }
        });
    }

    @Override
    public void initData() {

    }
}
