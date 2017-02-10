package com.lix.camera.utils;

import android.util.Log;

/**
 * Created by lix on 2016/9/1.
 *
 * A warp for system log.
*/
public class LogUtils {

    private static boolean DEBUG = true;

    public static void d(String tag, String declare) {
        if(DEBUG) {
            Log.d(tag, declare);
        }
    }

    public static void e(String tag, String declare) {
        if(DEBUG) {
            Log.e(tag, declare);
        }
    }

    public static void w(String tag, String declare) {
        if(DEBUG) {
            Log.w(tag, declare);
        }
    }
}
