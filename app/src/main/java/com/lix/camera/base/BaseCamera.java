package com.lix.camera.base;

import android.graphics.SurfaceTexture;
import android.util.Size;

import java.util.List;

/**
 * Created by lix on 2016/8/8.
 *
 * Base Camera defines a camera's functions used by others.
 */
public abstract class BaseCamera {

    public abstract void openCamera(int cameraId);
    public abstract void startCameraBackgroundThread();
    public abstract void startPreview();
    public abstract void stopPreview();
    public abstract void closeCamera();
    public abstract void stopCameraBackgroundThread();
    public abstract void setPictureSize(Size pictureSize);
    public abstract void takePicture();

    public abstract void setWhiteBalance(String wbValue);
    public abstract void setISO(String isoValue);
    public abstract void setFlashValue(String flashValue);
    public abstract void setShutterSpeed(int shutterSpeed);

    public interface CameraStatusListener {
        void onCameraFeaturesReach(CameraFeatures cameraFeatures);
        void onCameraOpened();
        void onCameraDisconnected();
        void onCameraError();
        void onCameraDisplaySizeChanged(int width, int height);
    }

    public static class CameraFeatures {
        boolean supportZoom = false;
        int maxZoom = 0;
        List<Integer> zoomRatios = null;

        boolean supportFaceDetection = false;

        List<Size> pictureSizes = null;
        List<Size> videoSizes = null;
        List<Size> previewSizes = null;

        List<String> supportedFlashValues = null;
        List<String> supportedFocusValues = null;
        int maxNumFocusAreas = 0;

        List<String> supportedWhiteBalanceValues = null;

        boolean supportIso = false;
        List<String> supportedIsoValues = null;

        boolean supportAntibanding = false;

        List<String> supportedAntibandingValues = null;

        boolean supportExposureLock = false;
        boolean supportVideoStabilization = false;
        int minExposure = 0;
        int maxExposure = 0;
        float exposureStep = 0.0f;

        boolean hasCurrentFpsRange = false;

        int[] currentFpsRange = new int[2];

        boolean canDisableShutterSound = false;
    }

    public static class FlashValue {
        public static final String FLASH_ON = "flash_on";
        public static final String FLASH_OFF = "flash_off";
        public static final String FLASH_AUTO = "flash_auto";
        public static final String FLASH_TORCH = "flash_torch";
    }

    private CameraStatusListener mCameraStatusListener;

    protected CameraStatusListener getCameraStatusListener() {
        return mCameraStatusListener;
    }

    public void setCameraStatusListener(CameraStatusListener cameraStatusListener) {
        mCameraStatusListener = cameraStatusListener;
    }

    private SurfaceTexture mSurfaceTexture;

    protected SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }

    public void setDisplayTexture(SurfaceTexture texture) {
        mSurfaceTexture = texture;
    }
}
