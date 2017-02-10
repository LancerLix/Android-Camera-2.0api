package com.lix.camera.base;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.util.Size;
import android.view.SurfaceHolder;

import java.util.List;

/**
 * Created by lix on 2016/8/8.
 *
 * Base Camera defines a camera's functions used by others.
 */
public abstract class BaseCamera {

    public abstract boolean captureResultHasIso();
    public abstract boolean captureResultHasExposureTime();
    public abstract boolean captureResultHasFrameDur();
    public abstract boolean captureResultHasFocusDist();
    public abstract int captureResultIso();
    public abstract long captureResultExposureTime();
    public abstract long captureResultFrameDur();
    public abstract float captureResultFocusDistMin();
    public abstract float captureResultFocusDistMax();

    public abstract void setSurfaceHolder(SurfaceHolder holder);
    public abstract void setSurfaceTexture(SurfaceTexture texture);

    public abstract void openCamera(CameraId cameraId);
    public abstract int getCameraDisplayOrientation();
    public abstract void startPreview();
    public abstract void stopPreview();
    public abstract void closeCamera();
    public abstract void setPictureSize(Size pictureSize);
    public abstract boolean supportAutoFocus();
    public abstract void autoFocus();
    public abstract void cancelAutoFocus();
    public abstract boolean setFocusAndMeteringArea(List<BaseCamera.Area> areas);
    public abstract void clearFocusAndMetering();
    public abstract void takePicture();

    public abstract void startFaceDetection();
    public abstract void stopFaceDetection();

    public abstract void setPictureRotation(int rotation);
    public abstract void setWhiteBalance(String wbValue);
    public abstract void setISO(String isoValue);
    public abstract void setFlashValue(String flashValue);
    public abstract void setFocusValue(String focusValue);
    public abstract void setShutterSpeed(int shutterSpeed);

    public interface CameraStatusListener {
        void onCameraFeaturesReach(final CameraFeatures cameraFeatures);
        void onCameraOpened();
        void onCameraDisconnected();
        void onCameraError();
        void onCameraClosed();
        void onCameraDisplaySizeChanged(final int width, final int height);
        void onAutoFocus(boolean success, int distance, int iso);
        void onCameraShutter();
        void onCameraRawPicDataReach(final byte[] data);
        void onCameraJpegPicDataReach(final byte[] data);
    }

    public enum CameraId {
        CAMERA_FACING_BACK,
        CAMERA_FACING_FRONT
    }

    public static class Area {
        public Rect mRect = null;
        public int mWeight = 0;

        public Area(Rect rect, int weight) {
            this.mRect = rect;
            this.mWeight = weight;
        }
    }

    public static class Face {
        public int mScore = 0;
        public Rect mRect = null;

        Face(int score, Rect rect) {
            this.mScore = score;
            this.mRect = rect;
        }
    }

    public static class CameraFeatures {
        int mSensorOrientation = 0;

        boolean mSupportZoom = false;
        int mMaxZoom = 0;
        List<Integer> mZoomRatios = null;

        boolean mSupportFaceDetection = false;

        List<Size> mPictureSizes = null;
        List<Size> mVideoSizes = null;
        List<Size> mPreviewSizes = null;

        List<String> mSupportedFlashValues = null;

        List<String> mSupportedFocusValues = null;
        int mMaxNumFocusAreas = 0;

        List<String> mSupportedWhiteBalanceValues = null;

        boolean mSupportIso = false;
        List<String> mSupportedIsoValues = null;
        boolean mSupportIsoRange = false;
        int mMinIso = 0;
        int mMaxIso = 0;

        boolean mSupportExposureTime = false;
        long mMinExposureTime = 0;
        long mMaxExposureTime = 0;

        boolean mSupportAntibanding = false;
        List<String> mSupportedAntibandingValues = null;

        boolean mSupportExposureLock = false;
        int mMinExposureCompensation = 0;
        int mMaxExposureCompensation = 0;
        float mExposureCompensationStep = 0.0f;

        boolean mSupportVideoStabilization = false;

        boolean mHasCurrentFpsRange = false;
        int[] mCurrentFpsRange = new int[]{-1, -1};

        boolean mCanDisableShutterSound = false;

        public int getSensorOrientation() {
            return mSensorOrientation;
        }
    }

    public static class FlashValue {
        public static final String FLASH_ON = "flash_on";
        public static final String FLASH_OFF = "flash_off";
        public static final String FLASH_AUTO = "flash_auto";
        public static final String FLASH_TORCH = "flash_torch";
    }

    public static class WhiteBalanceValue {
        public static final String WHITE_BALANCE_AUTO = "wb_auto";
        public static final String WHITE_BALANCE_CLOUDY_DAYLIGHT = "wb_cloudy_daylight";
        public static final String WHITE_BALANCE_DAYLIGHT = "wb_daylight";
        public static final String WHITE_BALANCE_FLUORESCENT = "wb_fluorescent";
        public static final String WHITE_BALANCE_INCANDESCENT = "wb_incandescent";
        public static final String WHITE_BALANCE_SHADE = "wb_shade";
        public static final String WHITE_BALANCE_TWILIGHT = "wb_twilight";
        public static final String WHITE_BALANCE_WARM_FLUORESCENT = "wb_warm";
    }

    public static class FocusValue {
        public static final String FOCUS_AUTO = "focus_auto";
        public static final String FOCUS_INFINITY = "focus_infinity";
        public static final String FOCUS_MACRO = "focus_macro";
        public static final String FOCUS_FIXED = "focus_fixed";
        public static final String FOCUS_EDOF = "focus_edof";
        public static final String FOCUS_CONTINUOUS_VIDEO = "focus_continuous_video";
        public static final String FOCUS_CONTINUOUS_PICTURE = "focus_continuous_picture";
    }

    private CameraStatusListener mCameraStatusListener = null;

    protected CameraStatusListener getCameraStatusListener() {
        return mCameraStatusListener;
    }

    public void setCameraStatusListener(CameraStatusListener cameraStatusListener) {
        mCameraStatusListener = cameraStatusListener;
    }

    private Handler mCameraBackgroundHandler = null;

    protected Handler getCameraBackgroundHandler() {
        return mCameraBackgroundHandler;
    }

    public void setCameraBackgroundHandler(Handler backgroundHandler) {
        mCameraBackgroundHandler = backgroundHandler;
    }
}
