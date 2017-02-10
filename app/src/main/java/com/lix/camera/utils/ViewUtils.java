package com.lix.camera.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ViewUtils {
 // Orientation hysteresis amount used in rounding, in degrees
    public static final int ORIENTATION_HYSTERESIS = 5;

    /**
     * Rounds the mOrientation so that the UI doesn't rotate if the user holds the device towards the floor or the sky
     * 
     * @param orientation New mOrientation
     * @param orientationHistory Previous mOrientation
     * @return Rounded mOrientation
     */
    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            dist = Math.min(dist, 360 - dist);
            changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
        }
        if (changeOrientation) {
            return ((orientation + 45) / 90 * 90) % 360;
        }
        return orientationHistory;
    }
    
    /**
     * convert the mOrientation come from application to the degrees that view will actually rotate
     * 
     * @param orientation the mOrientation come from application
     * @return true if convert success and the degrees we saved have been already updated
     *
     */
    public static int calculateUIRotationCompensation(Activity uiParent, int orientation, int currentUIRotation) {
        if (OrientationEventListener.ORIENTATION_UNKNOWN == orientation) {
            return currentUIRotation;
        }

        // Adjust orientationCompensation for the native mOrientation of the device
        int displayRotation = uiParent.getWindowManager().getDefaultDisplay().getRotation();

        int degrees = 0;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int orientationCompensation = (orientation + degrees) >= 360 ? (orientation + degrees - 360)
                : (orientation + degrees);
        if (orientationCompensation == 90) {
            orientationCompensation += 180;
        } else if (orientationCompensation == 270) {
            orientationCompensation -= 180;
        }

        // Avoid turning all around
        float angleDelta = orientationCompensation - currentUIRotation;
        if (angleDelta >= 270) {
            orientationCompensation -= 360;
        }

        return orientationCompensation;
    }
    
    /**
     * rotate an view with the given rotation
     * 
     * @param v which view be rotated
     * @param rotation the rotation degrees
     *
     */
    public static void rotateView(View v, float rotation) {
        if(null == v) {
            return;
        }
        
        v.animate().rotation(rotation).setDuration(50)
                .setInterpolator(new DecelerateInterpolator()).start();
    }

    /**
     * Converts the specified DP to PIXELS according to current screen density
     * 
     * @param context the application context
     * @param dp the size which based on dp as a unit
     * @return the size which based on pixel as a unit converted from dp parameter
     */
    public static float dpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int) ((dp * displayMetrics.density) + 0.5);
    }
}
