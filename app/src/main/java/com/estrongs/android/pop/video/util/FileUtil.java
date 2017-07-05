package com.estrongs.android.pop.video.util;

import java.io.File;

public class FileUtil {

    public static boolean makeSureFolderOfFileExist(String path) {
        int pos = path.lastIndexOf("/");
        String folder = path.substring(0, pos);
        return makeSureFolderExist(folder);
    }

    public static boolean makeSureFolderExist(String path) {
        File dir = new File(path);

        if (!dir.exists()) {
            return dir.mkdirs();
        } else {
            return dir.isDirectory();
        }

    }
}
