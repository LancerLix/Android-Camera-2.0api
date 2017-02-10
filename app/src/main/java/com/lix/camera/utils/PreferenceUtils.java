
package com.lix.camera.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;

import java.io.File;

@SuppressLint("WorldReadableFiles")
@SuppressWarnings("deprecation")
public class PreferenceUtils {

    private final static String TAG = PreferenceUtils.class.getSimpleName();

    // the preference that will clean when application quit
    public final static String PREFERENCES_UTILS_MAIN = "preference_utils_main";
    // the preference will saved forever until the application uninstall
    public final static String PREFERENCES_UTILS_ADV = "preference_utils_adv";

    private static SharedPreferences sSharedPreferences;

    private static Editor sEditor;

    private static Context sCtx;

    public static void initPreferences(Context ctx) {
        sCtx = ctx;
    }

    /**
     * Set string value to the given preference
     *
     * @param preferenceName name from preferences, and must be PREFERENCES_UTILS_MAIN or PREFERENCES_UTILS_ADV
     * @param key the key whose value you want to modify
     * @param value the value will be set
     */

    public static void setStringValue(String preferenceName, String key, String value) {
        if (null == sCtx || null == key || null == value) {
            LogUtils.e(TAG, "you should init first");
            return;
        }
        sSharedPreferences = sCtx.getSharedPreferences(preferenceName,
                Context.MODE_WORLD_READABLE);
        sEditor = sSharedPreferences.edit();
        sEditor.putString(key, value);
        sEditor.apply();

    }

    /**
     * Get string value to the given preference
     *
     * @param preferenceName name from preferences, and must be PREFERENCES_UTILS_MAIN or PREFERENCES_UTILS_ADV
     * @param key the key whose value you want to get
     * @param defaultValue the default value will be get if the value is never set before
     */
    public static String getStringValue(String preferenceName, String key, String defaultValue) {
        if (null == sCtx || null == key) {
            LogUtils.e(TAG, "you should init first");
            return defaultValue;
        }
        sSharedPreferences = sCtx.getSharedPreferences(preferenceName,
                Context.MODE_WORLD_READABLE);
        return sSharedPreferences.getString(key, defaultValue);
    }

    /**
     * Set boolean value to the given preference
     *
     * @param preferenceName name from preferences, and must be PREFERENCES_UTILS_MAIN or PREFERENCES_UTILS_ADV
     * @param key the key whose value you want to modify
     * @param value the value will be set
     */
    public static void setBooleanValue(String preferenceName, String key, boolean value) {
        if (null == sCtx || null == key) {
            LogUtils.e(TAG, "you should init first");
            return;
        }
        sSharedPreferences = sCtx.getSharedPreferences(preferenceName,
                Context.MODE_WORLD_READABLE);
        sEditor = sSharedPreferences.edit();
        sEditor.putBoolean(key, value);
        sEditor.apply();
    }

    /**
     * Get boolean value to the given preference
     *
     * @param preferenceName name from preferences, and must be PREFERENCES_UTILS_MAIN or PREFERENCES_UTILS_ADV
     * @param key the key whose value you want to get
     * @param defaultValue the default value will be get if the value is never set before
     */
    public static boolean getBooleanValue(String preferenceName, String key, boolean defaultValue) {
        if (null == sCtx || null == key) {
            LogUtils.e(TAG, "you should init first");
            return defaultValue;
        }
        sSharedPreferences = sCtx.getSharedPreferences(preferenceName,
                Context.MODE_WORLD_READABLE);
        return sSharedPreferences.getBoolean(key, defaultValue);
    }

    /**
     * Set int value to the given preference
     *
     * @param preferenceName name from preferences, and must be PREFERENCES_UTILS_MAIN or PREFERENCES_UTILS_ADV
     * @param key the key whose value you want to modify
     * @param value the value will be set
     */
    public static void setIntValue(String preferenceName, String key, int value) {
        if (null == sCtx || null == key) {
            LogUtils.e(TAG, "you should init first");
            return;
        }
        sSharedPreferences = sCtx.getSharedPreferences(preferenceName,
                Context.MODE_WORLD_READABLE);
        sEditor = sSharedPreferences.edit();
        sEditor.putInt(key, value);
        sEditor.apply();
    }

    /**
     * Get int value to the given preference
     *
     * @param preferenceName name from preferences, and must be PREFERENCES_UTILS_MAIN or PREFERENCES_UTILS_ADV
     * @param key the key whose value you want to get
     * @param defaultValue the default value will be get if the value is never set before
     */
    public static int getIntValue(String preferenceName, String key, int defaultValue) {
        if (null == sCtx || null == key) {
            LogUtils.e(TAG, "you should init first");
            return defaultValue;
        }
        sSharedPreferences = sCtx.getSharedPreferences(preferenceName,
                Context.MODE_WORLD_READABLE);
        return sSharedPreferences.getInt(key, defaultValue);
    }

    public static boolean contains(String preferenceName, String key) {
        if (null == sCtx || null == key) {
            LogUtils.e(TAG, "you should init first");
            return false;
        }

        sSharedPreferences = sCtx.getSharedPreferences(preferenceName,
                Context.MODE_WORLD_READABLE);
        return sSharedPreferences.contains(key);
    }

    public static void clearPreferences(String preferenceName) {
        if (null == sCtx) {
            LogUtils.e(TAG, "you should init first");
            return;
        }
        sSharedPreferences = sCtx.getSharedPreferences(preferenceName, Context.MODE_WORLD_READABLE);
        sEditor = sSharedPreferences.edit();
        sEditor.clear();
        sEditor.apply();
    }
    
    public final static String DEFAULT_IMAGE_LOCATION = Environment.DIRECTORY_DCIM + File.separator + "lixCamera";
    public static String getSaveLocationPreferenceKey() {
        return "preference_save_location";
    }

}
