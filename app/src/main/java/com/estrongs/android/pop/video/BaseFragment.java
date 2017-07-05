package com.estrongs.android.pop.video;

import java.util.List;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Fragment基类<br>
 * @Package com.jiaoyang.video.framework
 * @ClassName BaseFragment
 * @author TryLoveCatch
 * @date 2014年5月21日 下午10:24:10
 */
public abstract class BaseFragment extends Fragment implements IUI {

//    private ArrayList<Subscription> mSubscriptions = new ArrayList<>();

    protected View mRoot;

    private boolean mIsResumed;

    /**
     * * 做了4件事:<br>
     * 1、生成rootView<br>
     * 2、初始化Views<br>
     * 3、调用initViewProperty<br>
     * 4、调用initData
     *
     * @Title: onCreateView
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @param layoutResId
     * @return View
     * @date Apr 18, 2014 11:24:57 AM
     */
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, int layoutResId) {
        super.onCreateView(inflater, container, savedInstanceState);

        mRoot = inflater.inflate(layoutResId, container, false);
//        rootView.setBackgroundResource(R.color.bg);
//        if(UtilDust.isNight()){
//        	mRoot.setBackgroundResource(R.drawable.bg_night);
//        }else{
//        	mRoot.setBackgroundResource(R.drawable.bg_day);
//        }
        mRoot.setBackgroundColor(Color.WHITE);
        initData();
        initViewProperty();

        return mRoot;
    }

    public void removeBackground(){
        mRoot.setBackgroundColor(Color.TRANSPARENT);
    }

    public void setBackgroud(int pId){
    	mRoot.setBackgroundResource(pId);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!mIsResumed) {
            mIsResumed = true;
            onFirstResume();
        }
    }

    protected void onFirstResume(){
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void replaceFragment(Class<?> fregmentClass, Bundle arguments) {

        Fragment fragment = Fragment.instantiate(getActivity(), fregmentClass.getName(), arguments);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.content_frame, fragment);
        transaction.commit();
    }

    protected void openFragment(Fragment fromFragment, Class<?> fregmentClass, Bundle arguments) {

        Fragment fragment = Fragment.instantiate(getActivity(), fregmentClass.getName(), arguments);
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
//        transaction.setCustomAnimations(R.anim.right_in, R.anim.left_out, R.anim.left_in, R.anim.right_out);
        transaction.hide(fromFragment);
        transaction.add(R.id.content_frame, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    protected void replaceFragment(Fragment fromFragment, Class<?> fregmentClass, int contentId, Bundle arguments) {

        Fragment fragment = Fragment.instantiate(getActivity(), fregmentClass.getName(), arguments);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
//        transaction.setCustomAnimations(R.anim.right_in, R.anim.left_out, R.anim.left_in, R.anim.right_out);
        transaction.replace(contentId, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    protected void openFragment(Fragment fromFragment, Class<?> fregmentClass, int contentId, Bundle arguments) {

        Fragment fragment = Fragment.instantiate(getActivity(), fregmentClass.getName(), arguments);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
//        transaction.setCustomAnimations(R.anim.right_in, R.anim.left_out, R.anim.left_in, R.anim.right_out);
        transaction.hide(fromFragment);
        transaction.add(contentId, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

//    protected void addSubscription(Subscription pSub) {
//        mSubscriptions.add(pSub);
//    }

    protected void setTitle(int resId) {
        getActivity().setTitle(resId);
    }

    protected void setTitle(CharSequence title) {
        getActivity().setTitle(title);
    }


    /**
     * 接收返回键按下事件
     *
     * @Title: onBackKeyDown
     * @return boolean false:back键事件未处理，向下传递。 true：消费掉该事件。
     * @date 2014-3-10 上午11:15:33
     */
    public boolean onBackPressed() {
        return false;
    }




    protected boolean isListValid(List<?> plist){
    	return plist!=null && plist.size() > 0;
    }
}
