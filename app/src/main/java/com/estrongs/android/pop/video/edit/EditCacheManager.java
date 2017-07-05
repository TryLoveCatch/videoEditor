package com.estrongs.android.pop.video.edit;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lipeng21 on 2017/6/27.
 */

public class EditCacheManager {
    private static volatile EditCacheManager mInstance;
    private Map<String, EditManager> mMapCaches;
    private EditCacheManager(){
        mMapCaches = new HashMap<>();
    }
    public static EditCacheManager getInstance(){
        if(mInstance==null){
            synchronized (EditCacheManager.class){
                if(mInstance==null){
                    mInstance = new EditCacheManager();
                }
            }
        }
        return mInstance;
    }

    public void addManager(String pSrcPath, EditManager pManager){
        if(mMapCaches.containsKey(pSrcPath)){
            throw new RuntimeException("该路径已经存在");
        }

        mMapCaches.put(pSrcPath, pManager);
    }

    public EditManager getManager(String pSrcPath){
        return mMapCaches.get(pSrcPath);
    }

    public void removeManager(String pSrcPath){
        mMapCaches.remove(pSrcPath);
    }


}
