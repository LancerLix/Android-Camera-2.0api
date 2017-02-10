package com.lix.camera.base;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.lix.camera.utils.CameraParametersConverter;
import com.lix.camera.utils.LogUtils;
import com.lix.camera.utils.PicSizeUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lix on 2016/8/8.
 *
 */
@SuppressWarnings("deprecation")
public class AndroidCamera1 extends BaseCamera {

    private final String TAG = AndroidCamera1.class.getSimpleName();

    private class CameraPreviewCallback implements Camera.PreviewCallback {

        private final String TAG = "PreviewCallback";

        // Luminance threshold from experience value
        private static final int LUM_THRESHOLD_VERY_DARK = 5;

        private static final int LUM_THRESHOLD_DARK = 50;

        private static final int LUM_THRESHOLD_MIDDLE = 80;

        private static final int LUM_THRESHOLD_BRIGHT = 128;

        // 9 blocks
        private static final int COL_BLOCKS = 3;

        private static final int ROW_BLOCKS = 3;

        // 4 down sample
        private static final int COL_DOWNSAMPLE = 2;

        private static final int ROW_DOWNSAMPLE = 2;

        private int preview_width = 0;

        private int preview_height = 0;

        private int preview_luminance_level = 0;

        private long preview_min_luminance[] = new long[COL_BLOCKS * ROW_BLOCKS];

        private long preview_max_luminance[] = new long[COL_BLOCKS * ROW_BLOCKS];

        private long preview_total_luminance[] = new long[COL_BLOCKS * ROW_BLOCKS];

        private long preview_u_total = 0;

        private long preview_v_total = 0;

        private long current_preview_luminance = 0;

        private int preview_luminance_count = 0;

        private Boolean enable_luminance_detect = false;

        private int preview_col_size = 0;

        private int preview_row_size = 0;

        private final Object mLock = new Object();

        private int block_size = 1;

        private final float preview_lum_table[][] = new float[][] {
                {// LUM_THRESHOLD_VERY_DARK
                        180, 150, 100, 70, 0
                },
                {// LUM_THRESHOLD_DARK
                        150, 120, 80, 40, 0
                },
                {// LUM_THRESHOLD_MIDDLE
                        120, 80, 20, 2, 0
                },
                {// LUM_THRESHOLD_BRIGHT
                        50, 20, 10, 5, 0
                },
                {// LUM_THRESHOLD_VERY_BRIGHT
                        30, 20, 10, 5, 0
                },
        };

        private final float preview_contrast_table[][] = new float[][] {
                {// LUM_THRESHOLD_VERY_DARK
                        3.0f, 2.0f, 1.7f, 0xff, 0xff
                },
                {// LUM_THRESHOLD_DARK
                        2.0f, 1.7f, 1.5f, 0xff, 0xff
                },
                {// LUM_THRESHOLD_MIDDLE
                        2.0f, 0xff, 0xff, 0xff, 0xff
                },
                {// LUM_THRESHOLD_BRIGHT
                        2.0f, 0xff, 0xff, 0xff, 0xff
                },
                {// LUM_THRESHOLD_VERY_BRIGHT
                        0xff, 0xff, 0xff, 0xff, 0xff
                },
        };

        public CameraPreviewCallback(int width, int height) {
            preview_width = width;
            preview_height = height;
            preview_col_size = preview_height / COL_BLOCKS + 1;
            preview_row_size = preview_width / ROW_BLOCKS + 1;

            block_size = (preview_col_size * preview_row_size) / (COL_DOWNSAMPLE * ROW_DOWNSAMPLE);
        }

        public void enableDetect() {
            LogUtils.d(TAG, "enable detect...");
            synchronized (mLock) {
                enable_luminance_detect = true;
                for (int i = 0; i < COL_BLOCKS * ROW_BLOCKS; i++) {
                    preview_min_luminance[i] = Integer.MAX_VALUE;
                    preview_max_luminance[i] = 0;
                    preview_total_luminance[i] = 0;
                }
                preview_luminance_level = 0;
                preview_luminance_count = 0;
                preview_u_total = 0;
                preview_v_total = 0;
                current_preview_luminance = 0;
            }
        }

        public void disableDetect() {
            synchronized (mLock) {
                enable_luminance_detect = false;
            }
        }

        public boolean IsEnableDetect() {
            return enable_luminance_detect;
        }

        public int getLuminanceLevel() {
            return preview_luminance_level;
        }

        public int getBrightness() {
            LogUtils.d(TAG, "getBrightness:"+current_preview_luminance);
            return (int) (current_preview_luminance);
        }

        public int getFocusDistance() {
            int distance = 5;
            long bright_max = 0;
            long bright_total = 0;
            long dark_max = 0;
            long dark_total = 1;
            long avg_total = 0;
            long min_diff = 255, max_diff = 0;
            for (int i = 0; i < COL_BLOCKS * ROW_BLOCKS; i++) {
                long min = preview_min_luminance[i] / block_size;
                long max = preview_max_luminance[i] / block_size;
                long max_min = max - min;
                avg_total += max_min;

                if (max_min > max_diff) {
                    max_diff = max_min;
                }

                if (max_min < min_diff) {
                    min_diff = max_min;
                }

                if (bright_max < max) {
                    bright_max = max;
                }
                bright_total += max;

                if (dark_max < min) {
                    dark_max = min;
                }
                dark_total += min;
                LogUtils.d(TAG, "[" + i + "]" + " max:" + max + "  min:" + min + "  diff:" + max_min);
            }

            if (avg_total == 0 || max_diff < 10) {
                LogUtils.d(TAG, "brightness not changed!");
                return distance;
            }

            // adjustment contrast threshold
            float contrast_changed = (max_diff * (COL_BLOCKS * ROW_BLOCKS - 1.0f)) / (avg_total - max_diff);

            // calculate luminance level
            long level = dark_total / (COL_BLOCKS * ROW_BLOCKS);
            if (level <= LUM_THRESHOLD_VERY_DARK) {
                preview_luminance_level = 1;
            } else if (level <= LUM_THRESHOLD_DARK) {
                preview_luminance_level = 2;
            } else if (level <= LUM_THRESHOLD_MIDDLE) {
                preview_luminance_level = 3;
            } else if (level <= LUM_THRESHOLD_BRIGHT) {
                preview_luminance_level = 4;
            } else {
                preview_luminance_level = 5;
            }

            // bright case use whole frame data
            if (preview_luminance_level > 3) {
                max_diff = (bright_total - dark_total) / (COL_BLOCKS * ROW_BLOCKS);
            }
            float v_color_level = (preview_v_total * 8.0f) / (preview_width * preview_height * preview_luminance_count);
            float u_color_level = (preview_u_total * 8.0f) / (preview_width * preview_height * preview_luminance_count);
            LogUtils.d(TAG, "base:" + level + " max diff:" + max_diff + " avg diff:" + (avg_total / (COL_BLOCKS * ROW_BLOCKS))
                    + " contrast:" + contrast_changed
                    + " v:" + v_color_level
                    + " u:" + u_color_level);

            // diff_y compensation by color level
            if (v_color_level > 0.1 && max_diff < 130 && u_color_level < 0.7 && preview_luminance_level <= 3) {
                max_diff += v_color_level * max_diff;
                LogUtils.d(TAG, "compensation by color level " + max_diff);
            }

            float lut[] = preview_lum_table[preview_luminance_level - 1];
            float ctt[] = preview_contrast_table[preview_luminance_level - 1];
            for (int i = 0; i < 5; i++) {
                if (max_diff >= lut[i] || Math.abs(contrast_changed) >= ctt[i]) {
                    distance = i + 1;
                    break;
                }
            }
            LogUtils.d(TAG, "calculate distance:" + distance + "  level:" + preview_luminance_level);
            return distance;
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            boolean enable;
            synchronized (mLock) {
                enable = enable_luminance_detect;
            }
            if (data != null && data.length > 0 && enable) {
                if (preview_width > 0 && preview_height > 0) {
                    long lum[] = new long[COL_BLOCKS * ROW_BLOCKS];
                    for (int i = 0; i < preview_height; i = i + COL_DOWNSAMPLE) {
                        int col_index = i / preview_col_size;
                        for (int j = 0; j < preview_width; j = j + ROW_DOWNSAMPLE) {
                            int row_index = j / preview_row_size;
                            int index = i * preview_width + j;
                            lum[col_index * ROW_BLOCKS + row_index] += (0xff & (data[index]));
                        }
                    }

                    int v_count = 1;
                    int u_count = 1;
                    int length = Math.min(preview_width * preview_height * 3 / 2, data.length);
                    for (int i = preview_width * preview_height; i < length; i = i + 4) {
                        int v = (0xff & (data[i]));
                        if (v > 144 || v < 112)
                        {// filter dark/white color
                            v_count++;
                        }
                        int u = (0xff & (data[i + 1]));
                        if (u > 144 || u < 112)
                        {// filter dark/white color
                            u_count++;
                        }
                    }
                    preview_v_total += v_count;
                    preview_u_total += u_count;
                    // u_avg/v_avg > 1.1 blue color
                    // u_avg/v_avg < 0.9 red color

                    // sort max/min luminance during auto focus
                    for (int i = 0; i < lum.length; i++) {
                        preview_total_luminance[i] += lum[i];

                        if (lum[i] > preview_max_luminance[i]) {
                            preview_max_luminance[i] = lum[i];
                        }
                        if (lum[i] < preview_min_luminance[i]) {
                            preview_min_luminance[i] = lum[i];
                        }
                    }
                    current_preview_luminance = (lum[0] + lum[1] + lum[2] + lum[3] + lum[4] + lum[5] + lum[6] + lum[7] + lum[8])
                            / block_size / 9;
                    LogUtils.d(TAG, "Y: " + current_preview_luminance);
                    // Log.d(TAG, " " + lum[0] + " " + lum[1] + " " + lum[2] + " "
                    // + lum[3] + " " + lum[4] / block_size + " " + lum[5]
                    // + " " + lum[6] + " " + lum[7] + " " + lum[8]);
                    preview_luminance_count++;
                }
            }
            // else if (preview_width > 0 && data != null && data.length > 0 && base_iso_level <= 0) {
            // // detect noise level
            // for (int i = 0; i < detect_window_height; i++) {
            // for (int j = 0; j < detect_window_width; j++) {
            // current_detect_window[i * detect_window_width + j] = (0xff & (data[i * preview_width + j]));
            // }
            // }
            //
            // // get current frame noise level
            // int noise = 0;
            // for (int i = 0; i < detect_window_width * detect_window_height; i++) {
            // if (Math.abs(current_detect_window[i] - last_detect_window[i]) > 3) {
            // noise++;
            // }
            // last_detect_window[i] = current_detect_window[i];
            // }
            // float level = (noise * 1.0f) / (detect_window_width * detect_window_height);
            // detect_noise_total += level;
            //
            // // move quickly need drop this frame
            // if (level >= 0.8) {
            // detect_noise_count = 0;
            // detect_noise_total = 0;
            // }
            //
            // // get average noise level
            // if (detect_noise_count++ >= 15) {
            // detect_noise_level = detect_noise_total / detect_noise_count;
            // detect_noise_count = 0;
            // detect_noise_total = 0;
            // // Log.d(TAG, "Noise ratio:" + detect_noise_level);
            // }
            // }
        }
    }

    private Camera mCamera = null;

    private String mIsoKey = null;

    private CameraPreviewCallback mCameraPreviewCallback = null;
    private CameraPreviewCallback getCameraPreviewCallback() {
        return mCameraPreviewCallback;
    }

    private int mPreviewWidth = 0;
    private int mPreviewHeight = 0;

    private Camera.ShutterCallback mCameraShutterCallback = new Camera.ShutterCallback() {
        @Override
        public void onShutter() {
            // n.b., this is automatically run in a different thread
            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onCameraShutter();
            }
        }
    };

    private Camera.PictureCallback mCameraRawPicCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera cam) {
            // n.b., this is automatically run in a different thread
            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onCameraRawPicDataReach(data);
            }
        }
    };

    private Camera.PictureCallback mCameraJpegPicCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera cam) {
            // n.b., this is automatically run in a different thread
            if (null != getCameraStatusListener()) {
                getCameraStatusListener().onCameraJpegPicDataReach(data);
            }
        }
    };

    @Override
    public boolean captureResultHasIso() {
        return false;
    }

    @Override
    public boolean captureResultHasExposureTime() {
        return false;
    }

    @Override
    public boolean captureResultHasFrameDur() {
        return false;
    }

    @Override
    public boolean captureResultHasFocusDist() {
        return false;
    }

    @Override
    public int captureResultIso() {
        return -1;
    }

    @Override
    public long captureResultExposureTime() {
        return -1;
    }

    @Override
    public long captureResultFrameDur() {
        return -1;
    }

    @Override
    public float captureResultFocusDistMin() {
        return -1;
    }

    @Override
    public float captureResultFocusDistMax() {
        return -1;
    }

    @Override
    public void setSurfaceHolder(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setSurfaceTexture(SurfaceTexture texture) {
        try {
            mCamera.setPreviewTexture(texture);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
    @Override
    public void openCamera(CameraId cameraId) {

        int camera1Id = CameraParametersConverter.commCameraIdToCamera1(cameraId);

        mCamera = Camera.open(camera1Id);
        Camera.getCameraInfo(camera1Id, mCameraInfo);

        setupCameraDisplayOrientation();

        if(null != getCameraStatusListener()) {
            getCameraStatusListener().onCameraOpened();
        }

        if(null != getCameraStatusListener()) {
            getCameraStatusListener().onCameraFeaturesReach(generateCameraFeatures());
        }
    }

    private int mCameraDisplayOrientation;
    private void setupCameraDisplayOrientation() {
        int degrees = 0;
        switch (PicSizeUtils.sRotation) {
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

        int result = 0;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }

        LogUtils.d(TAG, "    info mOrientation is " + mCameraInfo.orientation);
        LogUtils.d(TAG, "    setDisplayOrientation to " + result);

        mCamera.setDisplayOrientation(result);
        mCameraDisplayOrientation = result;
    }

    private CameraFeatures generateCameraFeatures() {
        LogUtils.d(TAG, "generateCameraFeatures()");
        Camera.Parameters parameters = this.getParameters();
        if(null == parameters) {
            LogUtils.e(TAG, "can't get parameters when generate camera features !");
            return null;
        }

        if(null == mCameraInfo) {
            LogUtils.e(TAG, "camera information is null when generate camera features !");
            return null;
        }

        CameraFeatures cameraFeatures = new CameraFeatures();

        cameraFeatures.mSensorOrientation = mCameraInfo.orientation;

        cameraFeatures.mSupportZoom = parameters.isZoomSupported();
        if( cameraFeatures.mSupportZoom ) {
            cameraFeatures.mMaxZoom = parameters.getMaxZoom();
            try {
                cameraFeatures.mZoomRatios = parameters.getZoomRatios();
            }
            catch(NumberFormatException e) {
                // crash java.lang.NumberFormatException: Invalid int: " 500" reported in v1.4 on device "es209ra", Android 4.1, 3 Jan 2014
                // this is from java.lang.Integer.invalidInt(Integer.java:138) - unclear if this is a bug in Open Camera, all we can do for now is catch it
                e.printStackTrace();
                cameraFeatures.mSupportZoom = false;
                cameraFeatures.mMaxZoom = 0;
                cameraFeatures.mZoomRatios = null;
            }
        }

        cameraFeatures.mSupportFaceDetection = parameters.getMaxNumDetectedFaces() > 0;

        // get available sizes
        List<Camera.Size> cameraPictureSizes = parameters.getSupportedPictureSizes();
        cameraFeatures.mPictureSizes = new ArrayList<>();
        for(Camera.Size cameraSize : cameraPictureSizes) {
            cameraFeatures.mPictureSizes.add(new Size(cameraSize.width, cameraSize.height));
        }

        List<Camera.Size> cameraVideoSizes = parameters.getSupportedVideoSizes();
        if( cameraVideoSizes == null ) {
            // if null, we should use the preview sizes - see http://stackoverflow.com/questions/14263521/android-getsupportedvideosizes-always-returns-null
            LogUtils.d(TAG, "take video_sizes from preview sizes");
            cameraVideoSizes = parameters.getSupportedPreviewSizes();
        }
        cameraFeatures.mVideoSizes = new ArrayList<>();
        for(Camera.Size cameraSize : cameraVideoSizes) {
            cameraFeatures.mVideoSizes.add(new Size(cameraSize.width, cameraSize.height));
        }

        List<Camera.Size> cameraPreviewSizes = parameters.getSupportedPreviewSizes();
        cameraFeatures.mPreviewSizes = new ArrayList<>();
        for(Camera.Size cameraSize : cameraPreviewSizes) {
            cameraFeatures.mPreviewSizes.add(new Size(cameraSize.width, cameraSize.height));
        }

        List<String> supportedFlashModes = parameters.getSupportedFlashModes(); // Android format
        cameraFeatures.mSupportedFlashValues = new ArrayList<>();
        for(String flashMode : supportedFlashModes) {
            cameraFeatures.mSupportedFlashValues.add(CameraParametersConverter.camera1FlashValueToComm(flashMode));// convert to our format (also resorts)
        }

        List<String> supportedFocusModes = parameters.getSupportedFocusModes(); // Android format
        cameraFeatures.mSupportedFocusValues = new ArrayList<>();
        for(String focusMode : supportedFocusModes) {
            cameraFeatures.mSupportedFocusValues.add(CameraParametersConverter.camera1FocusValueToComm(focusMode));
        }

        cameraFeatures.mMaxNumFocusAreas = parameters.getMaxNumFocusAreas();

        List<String> supportedWBValues = parameters.getSupportedWhiteBalance(); // Android format
        cameraFeatures.mSupportedWhiteBalanceValues = new ArrayList<>();
        for(String wbValue : supportedWBValues) {
            cameraFeatures.mSupportedWhiteBalanceValues.add(CameraParametersConverter.camera1WBValueToComm(wbValue));

        }

        getSupportedIsoValues(parameters, cameraFeatures);

        cameraFeatures.mSupportedAntibandingValues = parameters.getSupportedAntibanding();
        if(null != cameraFeatures.mSupportedAntibandingValues
                && cameraFeatures.mSupportedAntibandingValues.size() > 0) {
            cameraFeatures.mSupportAntibanding = true;
        }

        cameraFeatures.mSupportExposureLock = parameters.isAutoExposureLockSupported();
        cameraFeatures.mMinExposureCompensation = parameters.getMinExposureCompensation();
        cameraFeatures.mMaxExposureCompensation = parameters.getMaxExposureCompensation();
        cameraFeatures.mExposureCompensationStep = parameters.getExposureCompensationStep();

        cameraFeatures.mSupportVideoStabilization = parameters.isVideoStabilizationSupported();

        try {
            parameters.getPreviewFpsRange(cameraFeatures.mCurrentFpsRange);
            if(-1 != cameraFeatures.mCurrentFpsRange[0] || -1 != cameraFeatures.mCurrentFpsRange[1] ) {
                cameraFeatures.mHasCurrentFpsRange = true;
            }
        }catch(StringIndexOutOfBoundsException e) {
            /* Have had reports of StringIndexOutOfBoundsException on Google Play on Sony Xperia M devices
                at android.hardware.Camera$Parameters.splitRange(Camera.java:4098)
                at android.hardware.Camera$Parameters.getSupportedPreviewFpsRange(Camera.java:2799)
                */
            e.printStackTrace();
        }

        LogUtils.d(TAG, "camera parameters: " + parameters.flatten());

            // Camera.canDisableShutterSound requires JELLY_BEAN_MR1 or greater
        cameraFeatures.mCanDisableShutterSound = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && mCameraInfo.canDisableShutterSound;

        return cameraFeatures;
    }

    void getSupportedIsoValues(Camera.Parameters parameters, CameraFeatures cameraFeatures){
        String isoValues = parameters.get("iso-values");
        if (isoValues == null) {
            isoValues = parameters.get("iso-mode-values"); // Galaxy Nexus
            if (isoValues == null) {
                isoValues = parameters.get("iso-speed-values"); // Micro max A101
                if (isoValues == null)
                    isoValues = parameters.get("nv-picture-iso-values"); // LG dual P990
            }
        }

        if ( null == isoValues) {
            cameraFeatures.mSupportIso = false;
            return;
        }

        if (isoValues.length() > 0) {
            LogUtils.d(TAG, "iso values: " + isoValues);
            String[] isoArray = isoValues.split(",");
            if (isoArray.length > 0) {
                cameraFeatures.mSupportedIsoValues = new ArrayList<>();
                for (String iso : isoArray) {
                    if (!iso.equals("ISO_HJR")) {// skip ISO_HJR
                        cameraFeatures.mSupportedIsoValues.add(iso);
                    }
                }
            }
        }

        mIsoKey = "iso";
        if (parameters.get(mIsoKey) == null) {
            mIsoKey = "iso-speed"; // Micro max A101
            if (parameters.get(mIsoKey) == null) {
                mIsoKey = "nv-picture-iso"; // LG dual P990
                if (parameters.get(mIsoKey) == null)
                    mIsoKey = null; // not supported
            }
        }

        if (mIsoKey == null) {
            mIsoKey = "iso";
        }

        cameraFeatures.mSupportIso = null != cameraFeatures.mSupportedIsoValues
                && cameraFeatures.mSupportedIsoValues.size() > 0 && mIsoKey != null;
    }

    @Override
    public int getCameraDisplayOrientation() {
        return mCameraDisplayOrientation;
    }

    @Override
    public void startPreview() {
        Camera.Parameters parameters = this.getParameters();
        if(parameters == null){
            return ;
        }
        Camera.Size size = parameters.getPreviewSize();
        if (mPreviewWidth != size.width || mPreviewHeight != size.height) {
            mPreviewWidth = size.width;
            mPreviewHeight = size.height;
            mCameraPreviewCallback = null;
        }

        if(null == mCameraPreviewCallback) {
            mCameraPreviewCallback = new CameraPreviewCallback(mPreviewWidth, mPreviewHeight);
        }

        mCameraPreviewCallback.disableDetect();

        mCamera.setPreviewCallback(mCameraPreviewCallback);
        mCamera.startPreview();
    }

    @Override
    public void stopPreview() {
        if(null != mCamera) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
        }
    }

    @Override
    public void closeCamera() {
        if(null != mCamera) {
            mCamera.release();
        }
        mCamera = null;

        if(null != getCameraStatusListener()) {
            getCameraStatusListener().onCameraClosed();
        }
    }

    @Override
    public void setPictureSize(Size pictureSize) {
        Camera.Parameters parameters = this.getParameters();
        if(parameters == null) {
            LogUtils.e(TAG, "failed to get camera parameters when set picture size");
            return ;
        }

        if(null != pictureSize && pictureSize.getWidth() > 0 && pictureSize.getHeight() > 0
                && (pictureSize.getWidth() != parameters.getPictureSize().width
                || pictureSize.getHeight() != parameters.getPictureSize().height)) {
            parameters.setPictureSize(pictureSize.getWidth(), pictureSize.getHeight());
            LogUtils.d(TAG, "set picture size: " + parameters.getPictureSize().width + ", "
                    + parameters.getPictureSize().height);
            setCameraParameters(parameters);
        }
    }

    private void setPreviewSize(Size previewSize) {
        Camera.Parameters parameters = this.getParameters();
        if(parameters == null){
            return ;
        }
        LogUtils.d(TAG, "current preview size: "
                + parameters.getPreviewSize().width + ", "+ parameters.getPreviewSize().height);
        parameters.setPreviewSize(previewSize.getWidth(), previewSize.getHeight());
        LogUtils.d(TAG, "new preview size: "
                + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
        setCameraParameters(parameters);
    }

    @Override
    public boolean supportAutoFocus() {
        Camera.Parameters parameters = this.getParameters();
        if(null == parameters) {
            return false;
        }

        String focus_mode = parameters.getFocusMode();
        // getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play from the below line (v1.7),
        // on Galaxy Tab 10.1 (GT-P7500), Android 4.0.3 - 4.0.4; HTC EVO 3D X515m (shooteru), Android 4.0.3 - 4.0.4
        return focus_mode != null
                && (focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO));
    }

    @Override
    public void autoFocus() {
        Camera.AutoFocusCallback camera_cb = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                // int level = 0;
                int distance = 0;
                if (null != getCameraPreviewCallback()){
                    getCameraPreviewCallback().disableDetect();
                    //    level = mPreviewCallbackListener.getLuminanceLevel();
                    distance = getCameraPreviewCallback().getFocusDistance();
                }

                if(null != getCameraStatusListener()) {
                    getCameraStatusListener().onAutoFocus(success, distance, -1);
                }
            }
        };
        if (null != getCameraPreviewCallback()){
            getCameraPreviewCallback().enableDetect();
        }

        if (mPreviewWidth > 0 && mPreviewHeight >0){
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try{
            mCamera.autoFocus(camera_cb);
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if(null != getCameraPreviewCallback()) {
                getCameraPreviewCallback().disableDetect();
            }
        }
    }

    @Override
    public void cancelAutoFocus() {
        try{
            mCamera.cancelAutoFocus();
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if(null != getCameraPreviewCallback()) {
                getCameraPreviewCallback().disableDetect();
            }
        }
    }

    @Override
    public boolean setFocusAndMeteringArea(List<Area> areas) {
        return false;
    }

    @Override
    public void clearFocusAndMetering() {
        Camera.Parameters parameters = this.getParameters();
        if(parameters == null){
            return ;
        }
        boolean update_parameters = false;
        if( parameters.getMaxNumFocusAreas() > 0 ) {
            parameters.setFocusAreas(null);
            update_parameters = true;
        }
        if( parameters.getMaxNumMeteringAreas() > 0 ) {
            parameters.setMeteringAreas(null);
            update_parameters = true;
        }
        cancelAutoFocus();
        if( update_parameters ) {
            setCameraParameters(parameters);
        }
    }

    @Override
    public void takePicture() {
        if(null != mCamera) {
            mCamera.takePicture(mCameraShutterCallback, mCameraRawPicCallback, mCameraJpegPicCallback);
        }
    }

    @Override
    public void startFaceDetection() {

    }

    @Override
    public void stopFaceDetection() {

    }

    @Override
    public void setPictureRotation(int rotation) {
        Camera.Parameters parameters = this.getParameters();
        if(parameters == null){
            return ;
        }
        parameters.setRotation(rotation);
        setCameraParameters(parameters);
    }

    @Override
    public void setWhiteBalance(String wbValue) {
        Camera.Parameters parameters = this.getParameters();
        if(null == parameters) {
            LogUtils.e(TAG, "failed to get camera parameters when set white balance value");
            return;
        }

        wbValue = CameraParametersConverter.commWBValueToCamera1(wbValue);

        if(null != wbValue && !wbValue.equals(parameters.getWhiteBalance())) {
            LogUtils.d(TAG, "set white balance to: " + wbValue);
            parameters.setWhiteBalance(wbValue);
            setCameraParameters(parameters);
        }
    }

    @Override
    public void setISO(String isoValue) {
        Camera.Parameters parameters = this.getParameters();

        if(null == parameters) {
            LogUtils.e(TAG, "failed to get camera parameters when set iso value");
            return;
        }

        if (null != isoValue && null != mIsoKey && !isoValue.equals(parameters.get(mIsoKey))) {
            LogUtils.d(TAG, "set " + mIsoKey + " to: " +isoValue);
            parameters.set(mIsoKey, isoValue);
            setCameraParameters(parameters);
        }
    }

    @Override
    public void setFlashValue(String flashValue) {
        Camera.Parameters parameters = this.getParameters();
        if(null == parameters || null == parameters.getFlashMode()) {
            LogUtils.e(TAG, "failed to get camera parameters when set flash value");
            return ;
        }

        flashValue = CameraParametersConverter.commFlashValueToCamera1(flashValue);

        if (null != flashValue && !flashValue.equals(parameters.getFlashMode())) {
            if (parameters.getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)
                    && !flashValue.equals(Camera.Parameters.FLASH_MODE_OFF)) {
                // workaround for bug on Nexus 5 where torch doesn't switch off
                // until we set FLASH_MODE_OFF
                LogUtils.d(TAG, "first turn torch off");
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                setCameraParameters(parameters);
            }

            LogUtils.d(TAG, "set flash to: " + flashValue);
            parameters.setFlashMode(flashValue);
            setCameraParameters(parameters);
        }
    }

    @Override
    public void setFocusValue(String focusValue) {
        Camera.Parameters parameters = this.getParameters();
        if(null == parameters || null == focusValue) {
            LogUtils.e(TAG, "failed to get camera parameters when set focus value");
            return ;
        }

        focusValue = CameraParametersConverter.commFocusValueToCamera1(focusValue);

        if(null != focusValue && !focusValue.equalsIgnoreCase(parameters.getFocusMode())) {
            LogUtils.d(TAG, "setFocusValue: " + focusValue);
            parameters.setFocusMode(focusValue);
            setCameraParameters(parameters);
        }
    }

    @Override
    public void setShutterSpeed(int shutterSpeed) {

    }

    private Camera.Parameters getParameters() {
        try{
            return mCamera.getParameters();
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private void setCameraParameters(Camera.Parameters parameters) {
        LogUtils.d(TAG, "setCameraParameters");
        try {
            mCamera.setParameters(parameters);
            LogUtils.d(TAG, "done");
        }catch(RuntimeException e) {
            LogUtils.d(TAG, "setCameraParameters failed!");
            e.printStackTrace();
        }
    }
}
