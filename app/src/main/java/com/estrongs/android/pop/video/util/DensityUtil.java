package com.estrongs.android.pop.video.util;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public final class DensityUtil {
	
    public static int screenPxWidth;
    public static int screenPxHeight;
    public static int screenDpHeight;
    public static int screenDpWidth;
    
    public static int realScreenPxWidth;
    public static int realScreenPxHeight;

	public static int dp1;
	public static int dp2;
	public static int dp3;
	public static int dp4;
	public static int dp5;
	public static int dp6;
	public static int dp7;
	public static int dp8;
	public static int dp9;
	public static int dp10;
	public static int dp11;
	public static int dp12;
	public static int dp13;
	public static int dp14;
	public static int dp15;
	public static int dp16;
	public static int dp17;
	public static int dp18;
	public static int dp19;
	public static int dp20;
	public static int dp21;
	public static int dp22;
	public static int dp23;
	public static int dp24;
	public static int dp25;
	public static int dp26;
	public static int dp27;
	public static int dp28;
	public static int dp29;

	private DensityUtil() {		
	}
	
	private static DisplayMetrics metrics = null;

	public static void init() {
		metrics = new DisplayMetrics();
		final WindowManager wm = (WindowManager) AndroidUtil.getApplication().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		display.getMetrics(metrics);
		DensityUtil.init(metrics, display);
	}

	@SuppressLint("NewApi")
	private static void init(final DisplayMetrics val, final Display display) {
		metrics = val;		
		int ver = android.os.Build.VERSION.SDK_INT;
		
		if (ver < 17) {
	        if (ver < 13) {
	        	realScreenPxWidth = metrics.widthPixels;
	    		realScreenPxHeight = metrics.heightPixels;
	        } else if (ver == 13) {
	            try {
	                Method method = display.getClass().getMethod("getRealWidth");
	                realScreenPxWidth = (Integer) method.invoke(display);
	            } catch (Exception e) {
	            	realScreenPxWidth = metrics.widthPixels;
	            } 
	            
	            try {
	                Method method = display.getClass().getMethod("getRealHeight");
	                realScreenPxHeight = (Integer) method.invoke(display);
	            } catch (Exception e) {
	            	realScreenPxHeight = metrics.heightPixels;
	            } 
	        } else if (ver > 13) {
	            try {
	                Method method = display.getClass().getMethod("getRawWidth");
	                realScreenPxWidth = (Integer) method.invoke(display);
	            } catch (Exception e) {
	            	realScreenPxWidth = metrics.widthPixels;
	            } 
	            
	            try {
	                Method method = display.getClass().getMethod("getRawHeight");
	                realScreenPxHeight = (Integer) method.invoke(display);
	            } catch (Exception e) {
	            	realScreenPxHeight = metrics.heightPixels;
	            } 
	        }
		} else {
			Point pt = new Point();
			display.getRealSize(pt);
			realScreenPxWidth = pt.x;
			realScreenPxHeight = pt.y;
		}
		
		screenPxWidth = metrics.widthPixels;
		screenPxHeight = metrics.heightPixels;
		screenDpWidth = px2dip (screenPxWidth);
		screenDpHeight = px2dip (screenPxHeight);

		dp1 = dip2px(1);
		dp2 = dip2px(2);
		dp3 = dip2px(3);
		dp4 = dip2px(4);
		dp5 = dip2px(5);
		dp6 = dip2px(6);
		dp7 = dip2px(7);
		dp8 = dip2px(8);
		dp9 = dip2px(9);
		dp10= dip2px(10);
		dp11= dip2px(11);
		dp12= dip2px(12);
		dp13= dip2px(13);
		dp14= dip2px(14);
		dp15= dip2px(15);
		dp16= dip2px(16);
		dp17= dip2px(17);
		dp18= dip2px(18);
		dp19= dip2px(19);
		dp20= dip2px(20);
		dp21= dip2px(21);
		dp22= dip2px(22);
		dp23= dip2px(23);
		dp24= dip2px(24);
		dp25= dip2px(25);
		dp26= dip2px(26);
		dp27= dip2px(27);
		dp28= dip2px(28);
		dp29= dip2px(29);
	}
  
    public static int dip2px(final float dpValue) {
    	if (metrics == null) {
    		init();
    	}
    	
        return (int) (dpValue * metrics.density + 0.5f);  
    }  
  
    public static int px2dip(final float pxValue) {
    	if (metrics == null) {
    		init();
    	}
    	
        return (int) (pxValue / metrics.density + 0.5f);  
    }

	public static int spToPx(float sp) {
		return (int) (sp * metrics.density + 0.5f);
	}
}