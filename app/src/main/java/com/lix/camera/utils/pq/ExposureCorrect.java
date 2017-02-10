
package com.lix.camera.utils.pq;

import android.graphics.Bitmap;

import com.lix.camera.utils.LogUtils;

/**
 * @author hunter.zheng Java wrapper for over exposure correct
 */
public class ExposureCorrect {

    private static final String TAG = ExposureCorrect.class.getSimpleName();

    public static final float[] EXPOSURE_CORRECT_U = {1.0f, 0.9994f, 0.9976f, 0.9903f, 0.9659f, 0.9f, 0.9f, 0.8f, 0.76f};
    public static final float[] EXPOSURE_CORRECT_V = {1.0f, 0.0349f, 0.0698f, 0.1392f, 0.2588f, 1.1f, 1.2f, 1.3f, 1.4f};

    public static class CorrectLevel {
        public static final int LEVEL_NONE = 0;
        public static final int LEVEL_1 = 1;
        public static final int LEVEL_2 = 2;
        public static final int LEVEL_3 = 3;
        public static final int LEVEL_4 = 4;
        public static final int LEVEL_5 = 5;
        public static final int LEVEL_6 = 6;
        public static final int LEVEL_7 = 7;
        public static final int LEVEL_8 = 8;
    }

    static {
        try {
            LogUtils.d(TAG, "exposure_correct_jni");
            System.loadLibrary("exposure_correct_jni");
        } catch (UnsatisfiedLinkError e) {
            LogUtils.e(TAG, "exposure correct library not found!");
        }
    }

    /**
     * photo over exposure correct from bitmap with spline.
     * 
     * @param bitmap photo bitmap of ARGB format.
     * @param w photo bitmap width.
     * @param h photo bitmap height.
     * @param spline photo bitmap exposure correct spline, the float array size is 255.
     * @return >=0 is set successfully, false otherwise
     */
    public static int correct(Bitmap bitmap, int w, int h, float[] spline) {
        return _exposure_correct(bitmap, w, h, spline);
    }

    /**
     * photo over exposure correct from bitmap without spline,this is a auto mode.
     * 
     * @param bitmap photo bitmap of ARGB format.
     * @param w photo bitmap width.
     * @param h photo bitmap height.
     * @return >=0 is set successfully, false otherwise
     */
    public static int correct(Bitmap bitmap, int w, int h) {
        return _exposure_correct(bitmap, w, h);
    }

    /**
     * photo over exposure correct from bitmap without spline,this is a auto mode.
     * 
     * @param bitmap photo bitmap of ARGB format.
     * @param w photo bitmap width.
     * @param h photo bitmap height.
     * @param level color temperature level [1 ~ 16]
     * @param filter filter type value [1 ~ 4] median/mean/hybrid median/trimmed mean
     * @return >=0 is set successfully, false otherwise
     */
    public static int correct(Bitmap bitmap, int w, int h, int level, int filter) {
        return _exposure_correct(bitmap, w, h, level, filter);
    }

    /**
     * photo over exposure correct from bitmap without spline,this is a auto mode.
     * 
     * @param bitmap photo bitmap of ARGB format.
     * @param w photo bitmap width.
     * @param h photo bitmap height.
     * @param flashTimes external flash times [2,3]
     * @return >=0 is set successfully, false otherwise
     */
    public static int backgroundLightingCorrect(Bitmap bitmap, int w, int h, int flashTimes) {
        return _backlighting_correct(bitmap, w, h, flashTimes);
    }

    /**
     * get photo exposure level.
     * 
     * @param bitmap photo bitmap of ARGB format.
     * @param w photo bitmap width.
     * @param h photo bitmap height.
     * @return [0.0~1.0] = avg_lum / max_lum
     */
    public static float getExposureLevel(Bitmap bitmap, int w, int h) {
        return _check_exposure_level(bitmap, w, h);
    }

    /**
     * white balance correct.
     *
     * @param bitmap photo bitmap of ARGB format.
     * @param w photo bitmap width.
     * @param h photo bitmap height.
     * @param correctLevel correct level [0,8]
     * @return >=0 is set successfully, false otherwise
     */
    public static int whiteBalanceCorrect(Bitmap bitmap, int w, int h, int correctLevel) {
        return _whitebalance_correct(bitmap, w, h,
                EXPOSURE_CORRECT_U[correctLevel],
                EXPOSURE_CORRECT_V[correctLevel],
                correctLevel < EXPOSURE_CORRECT_U.length / 2 + 1  && correctLevel != CorrectLevel.LEVEL_NONE ? 2 : 0);
    }
    /**
     * white balance correct.
     * 
     * @param bitmap photo bitmap of ARGB format.
     * @param w photo bitmap width.
     * @param h photo bitmap height.
     * @param u U plan image data correct parameter.
     * @param v V plan image data correct parameter.
     * @param type the type of correct.default value is 0.[0,1]
     * @return >=0 is set successfully, false otherwise
     */
    private static int whiteBalanceCorrect(Bitmap bitmap, int w, int h, float u, float v, int type) {
        return _whitebalance_correct(bitmap, w, h, u, v, type);
    }
    
    private static native int _exposure_correct(Bitmap bitmap, int w, int h, float[] spline);

    private static native int _exposure_correct(Bitmap bitmap, int w, int h);

    private static native int _exposure_correct(Bitmap bitmap, int w, int h, int level, int filter);

    private static native int _backlighting_correct(Bitmap bitmap, int w, int h, int flashTimes);

    private static native float _check_exposure_level(Bitmap bitmap, int w, int h);
    
    private static native int _whitebalance_correct(Bitmap bitmap, int w, int h, float u, float v, int type);
}
