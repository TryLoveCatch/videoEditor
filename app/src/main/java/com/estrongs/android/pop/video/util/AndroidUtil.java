package com.estrongs.android.pop.video.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;

public class AndroidUtil {

    private static Application application;
    private static Handler handler = null;

    public static void init(Application app) {
        application = app;
        handler = new Handler(Looper.getMainLooper());
        application.registerActivityLifecycleCallbacks(activityLifecycleCallback);
    }

    public static Application getApplication() {
        return application;
    }

    public static String getRawString( int resId) {
        Context context = application;

        InputStream inputStream = context.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

    public static int getColor(int color) {
        return ContextCompat.getColor(AndroidUtil.getApplication(), color);
    }

    public static Handler handler() {
        return handler;
    }

    public static boolean isUIThread() {
        return Looper.getMainLooper().equals(Looper.myLooper());
    }

    public static void runOnUiThread(Runnable action) {
        handler.post(action);
    }

    public static void post(Runnable action) {
        handler.post(action);
    }

    public static void postDelayed(Runnable action, long delayed) {
        handler.postDelayed(action, delayed);
    }

    public static Drawable getDrawable(int drawable) {
        return application.getResources().getDrawable(drawable);
    }

    public static Activity currentActivity() {
        return currentActivity == null ? null : currentActivity.get();
    }

    private static SoftReference<Activity> currentActivity;
    private static Application.ActivityLifecycleCallbacks activityLifecycleCallback = new Application.ActivityLifecycleCallbacks() {

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            DensityUtil.init();
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            currentActivity = new SoftReference<>(activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }
    };
}
