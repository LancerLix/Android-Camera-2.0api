package com.lix.camera.base;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.lix.camera.utils.CameraParametersConverter;
import com.lix.camera.utils.LogUtils;
import com.lix.camera.utils.PhoneUtils;
import com.lix.camera.utils.PicSizeUtils;
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by lix on 2016/8/10.
 *
 * Camera implements the functions defined in base camera use Camera API 2.0
 */
public class AndroidCamera2 extends BaseCamera {

    private final String TAG = AndroidCamera2.class.getSimpleName();

    private static enum RequestState {
        STATE_NORMAL,
        STATE_WAITING_AUTO_FOCUS,
        STATE_WAITING_PRE_CAPTURE_START,
        STATE_WAITING_PRE_CAPTURE_DONE,
        STATE_CAPTURE
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static class CaptureRequestEX {
        public static CaptureRequest.Key<Byte> HW_PROFESSIONAL_MODE;
        public static CaptureRequest.Key<Integer> HW_SENSOR_ISO_VALUE;
        public static CaptureRequest.Key<Integer> HW_SENSOR_EXPOSURE_VALUE;
        public static CaptureRequest.Key<Integer> HW_SENSOR_WB_VALUE;
        public static CaptureRequest.Key<Byte> HW_CAMERA_FLAG;
        public static CaptureRequest.Key<Byte> HW_PROFESSIONAL_FOCUS_MODE;
        public static CaptureRequest.Key<Byte> HW_MANUAL_FOCUS_MODE;
        public static CaptureRequest.Key<Integer> HW_MANUAL_FOCUS_VALUE;

        static {
            try {
                Class cls=Class.forName("android.hardware.camera2.CaptureRequest$Key");
                Constructor cst=cls.getConstructor(new Class[]{String.class, Class.class});

                HW_PROFESSIONAL_MODE =  (CaptureRequest.Key<Byte>) cst.newInstance("com.huawei.capture.metadata.professionalMode", Byte.TYPE);
                HW_SENSOR_ISO_VALUE =  (CaptureRequest.Key<Integer>) cst.newInstance("com.huawei.capture.metadata.sensorIso", Integer.TYPE);
                HW_SENSOR_EXPOSURE_VALUE =  (CaptureRequest.Key<Integer>) cst.newInstance("com.huawei.capture.metadata.sensorExposureTime", Integer.TYPE);
                HW_SENSOR_WB_VALUE = (CaptureRequest.Key<Integer>) cst.newInstance("com.huawei.capture.metadata.sensorWbValue", Integer.TYPE);
                HW_CAMERA_FLAG = (CaptureRequest.Key<Byte>) cst.newInstance("com.huawei.capture.metadata.hwCamera2Flag", Byte.TYPE);
                HW_PROFESSIONAL_FOCUS_MODE = (CaptureRequest.Key<Byte>) cst.newInstance("com.huawei.capture.metadata.professionalFocusMode", Byte.TYPE);
                HW_MANUAL_FOCUS_MODE = (CaptureRequest.Key<Byte>) cst.newInstance("com.huawei.capture.metadata.manualFocusMode", Byte.TYPE);
                HW_MANUAL_FOCUS_VALUE = (CaptureRequest.Key<Integer>) cst.newInstance("com.huawei.capture.metadata.manualFocusValue", Integer.TYPE);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * @author lix
     *
     * Store current camera builder settings
     */
    private class BuilderSettingStorage {

        private int mPictureRotation;

        private boolean mManualExposure = false;
        private int mIso = 0;
        private long mExposureTime = 0;

        private int mWhiteBalance = CameraMetadata.CONTROL_AWB_MODE_AUTO;
        private String mFlashValue = FlashValue.FLASH_AUTO;

        /**
         * Setup a CaptureRequest builder use current storage value
         *
         * @param builder which will be set
         */
        public void setupBuilder(CaptureRequest.Builder builder) {
            if(null == builder) {
                LogUtils.e(TAG, "builder is null when setup builder");
                return;
            }

            if(PhoneUtils.isHuaweiPhone()) {
                builder.set(CaptureRequestEX.HW_PROFESSIONAL_MODE, (byte)1);
                builder.set(CaptureRequestEX.HW_CAMERA_FLAG, (byte)1);
            }

            setIso(builder, mIso);
            setExposureTime(builder, mExposureTime);
            setWhiteBalance(builder, mWhiteBalance);
            setFlashValue(builder, mFlashValue);
        }

        public boolean isManualExposure() {
            return mManualExposure;
        }

        public void setIso(CaptureRequest.Builder builder, int isoValue) {
            if(null == builder) {
                LogUtils.e(TAG, "builder is null when set iso");
                return;
            }

            if(PhoneUtils.useSpecialCamera2Setting()) {
                setIsoSpecial(builder, isoValue);
                return;
            }

            mManualExposure = 0 != isoValue;

            Integer builderCurrentIso = builder.get(CaptureRequest.SENSOR_SENSITIVITY);
            if(null == builderCurrentIso || isoValue != builderCurrentIso) {
                if(mManualExposure) {
                    LogUtils.d(TAG, "manual exposure mode set iso");

                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, isoValue);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mExposureTime);

                    if(FlashValue.FLASH_OFF.equals(mFlashValue)) {
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_TORCH.equals(mFlashValue)) {
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

                    }else {
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    }
                }else {
                    LogUtils.d(TAG, "auto exposure mode set iso");

                    if(FlashValue.FLASH_OFF.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_AUTO.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_ON.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_TORCH.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    }
                }
            }

            if(isoValue != mIso) {
                mIso = isoValue;
            }
        }

        public int getIso() {
            return mIso;
        }

        public void setFlashValue(CaptureRequest.Builder builder, String flashValue) {
            if(mManualExposure) {
                LogUtils.d(TAG, "manual exposure mode set flash mode");

                builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                builder.set(CaptureRequest.SENSOR_SENSITIVITY, mIso);
                builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, mExposureTime);


                if(FlashValue.FLASH_OFF.equals(flashValue)) {
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                }else if(FlashValue.FLASH_TORCH.equals(flashValue)) {
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

                }else{
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                }
            }else {
                LogUtils.d(TAG, "auto exposure mode set flash mode");

                if(FlashValue.FLASH_OFF.equals(flashValue)) {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                }else if(FlashValue.FLASH_AUTO.equals(flashValue)) {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                }else if(FlashValue.FLASH_ON.equals(flashValue)) {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                }else if(FlashValue.FLASH_TORCH.equals(flashValue)) {
                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                    builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                }
            }

            if(!flashValue.equals(mFlashValue)) {
                mFlashValue = flashValue;
            }
        }

        public String getFlashValue() {
            return mFlashValue;
        }

        public void setWhiteBalance(CaptureRequest.Builder builder, int wbValue) {
            if(null == builder) {
                LogUtils.e(TAG, "builder is null when set white balance");
                return;
            }

            Integer builderCurrentWB = builder.get(CaptureRequest.CONTROL_AWB_MODE);
            if(null == builderCurrentWB || wbValue != builderCurrentWB) {
                builder.set(CaptureRequest.CONTROL_AWB_MODE, wbValue);
            }

            if(wbValue != mWhiteBalance) {
                mWhiteBalance = wbValue;
            }
        }

        public void setExposureTime(CaptureRequest.Builder builder, long exposureTime) {
            if(null == builder) {
                LogUtils.e(TAG, "builder is null when set exposure time");
                return;
            }

            if(PhoneUtils.useSpecialCamera2Setting()) {
                setExposureTimeSpecial(builder, exposureTime);
                return;
            }

            mManualExposure = 0 != exposureTime;

            Long builderCurrentExposureTime = builder.get(CaptureRequest.SENSOR_EXPOSURE_TIME);
            if(null == builderCurrentExposureTime || exposureTime != builderCurrentExposureTime) {
                if(mManualExposure) {
                    LogUtils.d(TAG, "manual exposure mode set exposure time");

                    builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                    builder.set(CaptureRequest.SENSOR_SENSITIVITY, mIso);
                    builder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);

                    if(FlashValue.FLASH_OFF.equals(mFlashValue)) {
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_TORCH.equals(mFlashValue)) {
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

                    }else{
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_SINGLE);
                    }
                }else {
                    LogUtils.d(TAG, "auto exposure mode set exposure time");

                    if(FlashValue.FLASH_OFF.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_AUTO.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_ON.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_OFF);

                    }else if(FlashValue.FLASH_TORCH.equals(mFlashValue)) {
                        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                    }
                }
            }

            if(exposureTime != mExposureTime) {
                mExposureTime = exposureTime;
            }
        }

        public void setPictureRotation(CaptureRequest.Builder builder, int rotation) {
            if(null == builder) {
                LogUtils.e(TAG, "builder is null when set picture rotation");
                return;
            }

            Integer jpegOrientation = builder.get(CaptureRequest.JPEG_ORIENTATION);

            if(null == jpegOrientation || rotation != jpegOrientation) {
                builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
            }

            if(rotation != mPictureRotation) {
                mPictureRotation = rotation;
            }
        }

        public void setIsoSpecial(@NonNull CaptureRequest.Builder builder, int isoValue) {
            if(PhoneUtils.isHuaweiPhone()) {
                builder.set(CaptureRequestEX.HW_SENSOR_ISO_VALUE, isoValue);
            }

            if(isoValue != mIso) {
                mIso = isoValue;
            }
        }

        public void setExposureTimeSpecial(CaptureRequest.Builder builder, long exposureTime) {
            if(PhoneUtils.isHuaweiPhone()) {
                builder.set(CaptureRequestEX.HW_SENSOR_EXPOSURE_VALUE, (int)exposureTime);
            }

            if(exposureTime != mExposureTime) {
                mExposureTime = exposureTime;
            }
        }
    }

    private RequestState mRequestState = RequestState.STATE_NORMAL;

    private CameraManager mCameraManager;

    private CameraDevice mCameraDevice = null;
    private CameraCharacteristics mCameraCharacteristics;
    private String mCurrentCameraId;

    private CaptureRequest.Builder mPreviewBuilder = null;
    private CaptureRequest.Builder mPictureBuilder = null;

    private BuilderSettingStorage mBuilderSettingStorage;

    private CameraCaptureSession mCameraCaptureSession = null;

    private ImageReader mImageReader = null;

    private Size mPreviewSize;

    private boolean mCameraDeviceOpening = false;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private int OPEN_CLOSE_LOCK_TIMEOUT = 2500;

    public AndroidCamera2(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            LogUtils.d(TAG, "camera opened");
            mCameraDeviceOpening = false;
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;

            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onCameraOpened();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            LogUtils.d(TAG, "camera disconnected");
            mCameraDeviceOpening = false;
            mCameraOpenCloseLock.release();
            // need to set the camera to null first, as closing the camera may take some time, and we don't want any other
            // operations to continue (if called from main thread)
            mCameraDevice = null;
            LogUtils.d(TAG, "onDisconnected: camera is now set to null");
            camera.close();

            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onCameraDisconnected();
            }
        }

        @Override
        public void onClosed(CameraDevice camera) {
            LogUtils.d(TAG, "camera closed");

            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onCameraClosed();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            LogUtils.d(TAG, "camera error");
            mCameraDeviceOpening = false;
            mCameraOpenCloseLock.release();
            mCameraDevice = null;
            LogUtils.d(TAG, "onDisconnected: camera is now set to null");
            camera.close();
            LogUtils.d(TAG, "onDisconnected: camera is now closed");

            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onCameraError();
            }
        }
    };

    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            LogUtils.d(TAG, "onConfigured: " + session);
            LogUtils.d(TAG, "captureSession was: " + mCameraCaptureSession);

            mCameraCaptureSession = session;

            startPreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            LogUtils.d(TAG, "onConfigureFailed: " + session);
            LogUtils.d(TAG, "captureSession was: " + mCameraCaptureSession);
        }

    };

    private long mPreCaptureStartedTime = -1;
    private long PRE_CAPTURE_TIMEOUT_LIMIT = 2000;

    private boolean mTotalCaptureResultHasIso = false;
    private int mTotalCaptureResultIso = -1;

    private boolean mTotalCaptureResultHasExpTime = false;
    private long mTotalCaptureResultExpTime = -1;

    private boolean mTotalCaptureResultHasFrameDur = false;
    private long mTotalCaptureResultFrameDur = -1;

    private boolean mTotalCaptureResultHasFocusDist = false;
    private float mTotalCaptureResultFocusDistMin = 0.0f;
    private float mTotalCaptureResultFocusDistMax = 0.0f;

    @Override
    public boolean captureResultHasIso() {
        return mTotalCaptureResultHasIso;
    }

    @Override
    public boolean captureResultHasExposureTime() {
        return mTotalCaptureResultHasExpTime;
    }

    @Override
    public boolean captureResultHasFrameDur() {
        return mTotalCaptureResultHasFrameDur;
    }

    @Override
    public boolean captureResultHasFocusDist() {
        return mTotalCaptureResultHasFocusDist;
    }

    @Override
    public int captureResultIso() {
        return mTotalCaptureResultIso;
    }

    @Override
    public long captureResultExposureTime() {
        return mTotalCaptureResultExpTime;
    }

    @Override
    public long captureResultFrameDur() {
        return mTotalCaptureResultFrameDur;
    }

    @Override
    public float captureResultFocusDistMin() {
        return mTotalCaptureResultFocusDistMin;
    }

    @Override
    public float captureResultFocusDistMax() {
        return mTotalCaptureResultFocusDistMax;
    }

    private final CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                     long timestamp, long frameNumber) {
            if(RequestState.STATE_CAPTURE == mRequestState) {
                LogUtils.d(TAG, "Capture picture onCaptureStarted");
            }
            super.onCaptureStarted(session, request, timestamp, frameNumber);
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            processResult(request, partialResult);
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
//            processAF(request,result);
            //  processFaceDetect(request,result);
            processResult(request, result);
            processTotalResult(request, result);
            super.onCaptureCompleted(session, request, result);
        }

        private long mLastProcessedFrameNumber = 0;
        private int mLastAfState = -1;
        private void processResult(CaptureRequest request, CaptureResult result) {
            /*if( MyDebug.LOG )
            Log.d(TAG, "process, state: " + state);*/
            if( result.getFrameNumber() < mLastProcessedFrameNumber ) {
                /*if( MyDebug.LOG )
                    Log.d(TAG, "processAF discarded outdated frame " + result.getFrameNumber() + " vs " + last_process_frame_number);*/
                return;
            }

            mLastProcessedFrameNumber = result.getFrameNumber();

            switch(mRequestState) {
                case STATE_NORMAL:
                    processNormalState();
                    break;
                case STATE_WAITING_AUTO_FOCUS:
                    processWaitingFocusState(result);
                    break;
                case STATE_WAITING_PRE_CAPTURE_START:
                    processWaitingPreCaptureStartState(result);
                    break;
                case STATE_WAITING_PRE_CAPTURE_DONE:
                    processWaitingPreCaptureDoneState(result);
                    break;
                default:
                    break;
            }

            // use Integer instead of int, so can compare to null: Google Play crashes confirmed that this can happen; Google Camera also ignores cases with null af state
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
            if( afState != null && afState != mLastAfState ) {
                LogUtils.d(TAG, "CONTROL_AF_STATE changed from " + mLastAfState + " to " + afState);
                mLastAfState = afState;
            }
        }

        private void processTotalResult(CaptureRequest request, CaptureResult totalResult) {
            /*if( MyDebug.LOG )
            Log.d(TAG, "processCompleted");*/

            if( totalResult.get(CaptureResult.SENSOR_SENSITIVITY) != null ) {
                mTotalCaptureResultHasIso = true;
                mTotalCaptureResultIso = totalResult.get(CaptureResult.SENSOR_SENSITIVITY);
//                LogUtils.d(TAG, "capture_result_iso: " + mTotalCaptureResultIso);

                if(getBuilderSettingStorage().isManualExposure()
                        && getBuilderSettingStorage().getIso() != mTotalCaptureResultIso ) {
                    // ugly hack: problem that when we start recording mVideo (video_recorder.start() call), this often causes the ISO setting to reset to the wrong value!
                    // seems to happen more often with shorter exposure time
                    // seems to happen on other camera apps with Camera2 API too
                    // this workaround still means a brief flash with incorrect ISO, but is best we can do for now!
                    LogUtils.d(TAG, "ISO " + mTotalCaptureResultIso + " different to requested ISO " + getBuilderSettingStorage().getIso());
                    LogUtils.d(TAG, "    requested ISO was: " + request.get(CaptureRequest.SENSOR_SENSITIVITY));
                    LogUtils.d(TAG, "    requested AE mode was: " + request.get(CaptureRequest.CONTROL_AE_MODE));

                    setRepeatingRequest(getPreviewBuilder().build());
                }

            }else {
                mTotalCaptureResultHasIso = false;
            }

            if( totalResult.get(CaptureResult.SENSOR_EXPOSURE_TIME) != null ) {
                mTotalCaptureResultHasExpTime = true;
                mTotalCaptureResultExpTime = totalResult.get(CaptureResult.SENSOR_EXPOSURE_TIME);

            }else {
                mTotalCaptureResultHasExpTime = false;
            }

            if( totalResult.get(CaptureResult.SENSOR_FRAME_DURATION) != null ) {
                mTotalCaptureResultHasFrameDur = true;
                mTotalCaptureResultFrameDur = totalResult.get(CaptureResult.SENSOR_FRAME_DURATION);

            }else {
                mTotalCaptureResultHasFrameDur = false;
            }
            /*if( MyDebug.LOG ) {
                if( result.get(CaptureResult.SENSOR_EXPOSURE_TIME) != null ) {
                    long capture_result_exposure_time = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
                    Log.d(TAG, "capture_result_exposure_time: " + capture_result_exposure_time);
                }
                if( result.get(CaptureResult.SENSOR_FRAME_DURATION) != null ) {
                    long capture_result_frame_duration = result.get(CaptureResult.SENSOR_FRAME_DURATION);
                    Log.d(TAG, "capture_result_frame_duration: " + capture_result_frame_duration);
                }
            }*/
            if( totalResult.get(CaptureResult.LENS_FOCUS_RANGE) != null ) {
                Pair<Float, Float> focus_range = totalResult.get(CaptureResult.LENS_FOCUS_RANGE);
                /*if( MyDebug.LOG ) {
                    Log.d(TAG, "capture result focus range: " + focus_range.first + " to " + focus_range.second);
                }*/
                mTotalCaptureResultHasFocusDist = true;
                mTotalCaptureResultFocusDistMin = focus_range.first;
                mTotalCaptureResultFocusDistMax = focus_range.second;

            }else {
                mTotalCaptureResultHasFocusDist = false;
            }

//            if( face_detection_listener != null && previewBuilder != null && previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) != null && previewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) == CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL ) {
//                Rect sensor_rect = getViewableRect();
//                android.hardware.camera2.params.Face [] camera_faces = result.get(CaptureResult.STATISTICS_FACES);
//                if( camera_faces != null ) {
//                    CameraController.Face [] faces = new CameraController.Face[camera_faces.length];
//                    for(int i=0;i<camera_faces.length;i++) {
//                        faces[i] = convertFromCameraFace(sensor_rect, camera_faces[i]);
//                    }
//                    face_detection_listener.onFaceDetection(faces);
//                }
//            }
//
//            if( push_repeating_request_when_torch_off && push_repeating_request_when_torch_off_id == request ) {
//                if( MyDebug.LOG )
//                    Log.d(TAG, "received push_repeating_request_when_torch_off");
//                Integer flash_state = result.get(CaptureResult.FLASH_STATE);
//                if( MyDebug.LOG ) {
//                    if( flash_state != null )
//                        Log.d(TAG, "flash_state: " + flash_state);
//                    else
//                        Log.d(TAG, "flash_state is null");
//                }
//                if( flash_state != null && flash_state == CaptureResult.FLASH_STATE_READY ) {
//                    push_repeating_request_when_torch_off = false;
//                    push_repeating_request_when_torch_off_id = null;
//                    try {
//                        setRepeatingRequest();
//                    }
//                    catch(CameraAccessException e) {
//                        if( MyDebug.LOG ) {
//                            Log.e(TAG, "failed to set flash [from torch/flash off hack]");
//                            Log.e(TAG, "reason: " + e.getReason());
//                            Log.e(TAG, "message: " + e.getMessage());
//                        }
//                        e.printStackTrace();
//                    }
//                }
//            }

            switch(mRequestState) {
                case STATE_CAPTURE:
                    processCaptureState();
                    break;
                default:
                    break;
            }

        }

//        private int lastAfState = -1;
//        private void processAF(CaptureRequest request, CaptureResult result) {
//            if(mAfState == STATE_NORMAL || result.get(CaptureResult.CONTROL_AF_STATE) == null){
//                return;
//            }
//
//            int afState = result.get(CaptureResult.CONTROL_AF_STATE);
//
//            if (afState != lastAfState) {
//                if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
//                        || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
//                        || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
//                        || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED) {
//                    mAfState = STATE_NORMAL;
//                    if (afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
//                            || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED) {
//                        mAFCallBack.onAutoFocus(true, Default_Focus_Distance);
//                    } else {
//                        mAFCallBack.onAutoFocus(false, Default_Focus_Distance);
//                    }
//                }
//                lastAfState = afState;
//            }
//        }

        //        private void processFaceDetect(CaptureRequest request, CaptureResult result) {
//            if (mFaceDetectionListener != null
//                    && mPreviewBuilder != null
//                    && mPreviewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) != null
//                    && mPreviewBuilder.get(CaptureRequest.STATISTICS_FACE_DETECT_MODE) == CaptureRequest.STATISTICS_FACE_DETECT_MODE_FULL) {
//                Rect sensor_rect = mCharacteristics
//                        .get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
//                android.hardware.camera2.params.Face[] camera_faces = result
//                        .get(CaptureResult.STATISTICS_FACES);
//                if (camera_faces != null) {
//                    CameraApi.Face[] faces = new CameraApi.Face[camera_faces.length];
//                    for (int i = 0; i < camera_faces.length; i++) {
//                        faces[i] = convertFromCameraFace(sensor_rect, camera_faces[i]);
//                    }
//                    mFaceDetectionListener.onFaceDetection(faces);
//                }
//            }
//        }
        private void processNormalState() {
            //do nothing
        }

        private void processWaitingFocusState(CaptureResult result) {
            LogUtils.d(TAG, "waiting for auto focus...");

            // use Integer instead of int, so can compare to null: Google Play crashes confirmed that this can happen; Google Camera also ignores cases with null af state
            Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);

            if( afState == null ) {
                // auto focus shouldn't really be requested if af not available, but still allow this rather than getting stuck waiting for auto focus to complete
                LogUtils.e(TAG, "waiting for auto focus but af_state is null");
                mRequestState = RequestState.STATE_NORMAL;
                mPreCaptureStartedTime = -1;
                if(null != getCameraStatusListener()) {
                    if(captureResultHasIso()) {
                        getCameraStatusListener().onAutoFocus(false, DEFAULT_FOCUS_DISTANCE, mTotalCaptureResultIso);
                    }else {
                        getCameraStatusListener().onAutoFocus(false, DEFAULT_FOCUS_DISTANCE, -1);
                    }
                }

            }else if( afState != mLastAfState ) {
                // check for auto focus completing
                // need to check that af_state != last_af_state, except for continuous focus mode where if we're already focused, should return immediately
                if( afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED ||
                        afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED ) {
                    boolean focusSuccess = afState == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED || afState == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED;
                    if( focusSuccess ) {
                        LogUtils.d(TAG, "onCaptureCompleted: auto focus success");
                    }else {
                        LogUtils.d(TAG, "onCaptureCompleted: auto focus failed");
                    }

                    LogUtils.d(TAG, "af_state: " + afState);
                    mRequestState = RequestState.STATE_NORMAL;
                    mPreCaptureStartedTime = -1;
                    if(null != getCameraStatusListener()) {
                        if(captureResultHasIso()) {
                            getCameraStatusListener().onAutoFocus(focusSuccess, DEFAULT_FOCUS_DISTANCE, mTotalCaptureResultIso);
                        }else {
                            getCameraStatusListener().onAutoFocus(focusSuccess, DEFAULT_FOCUS_DISTANCE, -1);
                        }
                    }
                }
            }
        }

        private void processWaitingPreCaptureStartState(CaptureResult result) {
            LogUtils.d(TAG, "waiting for pre capture start...");
            // CONTROL_AE_STATE can be null on some devices
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);

            if( aeState != null ) {
                LogUtils.d(TAG, "CONTROL_AE_STATE = " + aeState);
            }else {
                LogUtils.d(TAG, "CONTROL_AE_STATE is null");
            }

            if( aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE /*|| ae_state == CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED*/ ) {
                // we have to wait for CONTROL_AE_STATE_PRE_CAPTURE; if we allow CONTROL_AE_STATE_FLASH_REQUIRED, then on Nexus 6 at least we get poor quality results with flash:
                // varying levels of brightness, sometimes too bright or too dark, sometimes with blue tinge, sometimes even with green corruption
                LogUtils.d(TAG, "pre capture started after: " + (System.currentTimeMillis() - mPreCaptureStartedTime));
                mRequestState = RequestState.STATE_WAITING_PRE_CAPTURE_DONE;
                mPreCaptureStartedTime = -1;

            }else if( mPreCaptureStartedTime != -1 && System.currentTimeMillis() - mPreCaptureStartedTime > PRE_CAPTURE_TIMEOUT_LIMIT ) {
                // hack - give up waiting - sometimes we never get a CONTROL_AE_STATE_PRE_CAPTURE so would end up stuck
                LogUtils.e(TAG, "pre capture timeout");

                mRequestState = RequestState.STATE_WAITING_PRE_CAPTURE_DONE;
                mPreCaptureStartedTime = -1;
            }
        }

        private void processWaitingPreCaptureDoneState(CaptureResult result) {
            LogUtils.d(TAG, "waiting for pre capture done...");
            // CONTROL_AE_STATE can be null on some devices
            Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
            if( aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE ) {
                LogUtils.d(TAG, "pre capture completed");
                if( aeState != null ) {
                    LogUtils.d(TAG, "CONTROL_AE_STATE = " + aeState);
                }else {
                    LogUtils.d(TAG, "CONTROL_AE_STATE is null");
                }
                mRequestState = RequestState.STATE_NORMAL;
                mPreCaptureStartedTime = -1;
                takePictureAfterPreCapture();
            }
        }

        private void processCaptureState() {
            LogUtils.d(TAG, "capture request completed");
            mRequestState = RequestState.STATE_NORMAL;
            // actual parsing of image data is done in the imageReader's OnImageAvailableListener()
            // need to cancel the auto focus, and restart the preview after taking the photo
            // Camera2Basic does a capture then sets a repeating request - do the same here just to be safe
            getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
//            camera_settings.setAEMode(previewBuilder, false); // not sure if needed, but the AE mode is set again in Camera2Basic
            // n.b., if capture/setRepeatingRequest throw exception, we don't call the take_picture_error_cb.onError() callback, as the photo should have been taken by this point
            setCaptureRequest(getPreviewBuilder().build());
            getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE); // ensure set back to idle
            clearFocusAndMetering();
            setRepeatingRequest(getPreviewBuilder().build());
        }
    };

    private final CameraCaptureSession.CaptureCallback mPictureCaptureCallback = new CameraCaptureSession.CaptureCallback() {

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            if(null != getCameraBackgroundHandler()) {
                getCameraBackgroundHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.d(TAG, "new still image available");
                        Image image = reader.acquireNextImage();

                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte [] bytes = new byte[buffer.remaining()];
                        LogUtils.d(TAG, "read " + bytes.length + " bytes");
                        buffer.get(bytes);
                        if (null != getCameraStatusListener()) {
                            getCameraStatusListener().onCameraJpegPicDataReach(bytes);
                        }

                        image.close();
                        LogUtils.d(TAG, "done onImageAvailable");
                    }
                });
            }
        }
    };

    public CaptureRequest.Builder getPreviewBuilder() {
        return mPreviewBuilder;
    }

    public CaptureRequest.Builder getStillCaptureBuilder() {
        return mPictureBuilder;
    }

    @NonNull
    public BuilderSettingStorage getBuilderSettingStorage() {
        return mBuilderSettingStorage;
    }

    private SurfaceHolder mSurfaceHolder;
    protected  SurfaceHolder getSurfaceHolder() {
        return mSurfaceHolder;
    }
    @Override
    public void setSurfaceHolder(SurfaceHolder holder) {
        mSurfaceHolder = holder;
    }

    private SurfaceTexture mSurfaceTexture;
    protected SurfaceTexture getSurfaceTexture() {
        return mSurfaceTexture;
    }
    @Override
    public void setSurfaceTexture(SurfaceTexture texture) {
        mSurfaceTexture = texture;
    }

    private Surface mCameraSurface;
    private Surface getCameraSurface() {
        return mCameraSurface;
    }

    @Override
    public void openCamera(CameraId cameraId) {
        LogUtils.d(TAG, "try to open camera");

        if(null == mCameraManager) {
            return;
        }

        if(mCameraDeviceOpening) {
            return;
        }

        try {

            String camera2Id = CameraParametersConverter.commCameraIdToCamera2(mCameraManager, cameraId);

            if(null != camera2Id) {
                mCurrentCameraId = camera2Id;
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);

                if(null != getCameraStatusListener()) {
                    getCameraStatusListener().onCameraFeaturesReach(generateCameraFeatures());
                }

                if (!mCameraOpenCloseLock.tryAcquire(OPEN_CLOSE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening. Dead lock may appear!");
                }

                mCameraDeviceOpening = true;
                mBuilderSettingStorage = new BuilderSettingStorage();
                mCameraManager.openCamera(mCurrentCameraId, mDeviceStateCallback, getCameraBackgroundHandler());
            }

        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    private CameraFeatures generateCameraFeatures() {
        LogUtils.d(TAG, "generateCameraFeatures()");
        CameraFeatures cameraFeatures = new CameraFeatures();

        if(null == mCameraCharacteristics) {
            LogUtils.e(TAG, "can't get parameters when generate camera features !");
            return null;
        }

        Integer sensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        if(null != sensorOrientation) {
            cameraFeatures.mSensorOrientation = sensorOrientation;
        }

        Integer hardwareLevel = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
        if(null != hardwareLevel) {
            switch (hardwareLevel) {
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    LogUtils.d(TAG, "Hardware Level: LEGACY");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    LogUtils.d(TAG, "Hardware Level: LIMITED");
                    break;
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    LogUtils.d(TAG, "Hardware Level: FULL");
                    break;
                default:
                    LogUtils.e(TAG, "Unknown Hardware Level!");
                    break;
            }
        }

        Float maxZoom = mCameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        if(null != maxZoom) {
            cameraFeatures.mSupportZoom = maxZoom > 0.0f;
            LogUtils.d(TAG, "max zoom: " + maxZoom);
            if( cameraFeatures.mSupportZoom ) {
                // set 20 steps per 2x factor
                final int stepsPer2XFactor = 20;
                //final double scale_factor = Math.pow(2.0, 1.0/(double)steps_per_2x_factor);
                int nSteps =(int)( (stepsPer2XFactor * Math.log(maxZoom + 1.0e-11)) / Math.log(2.0));
                final double scaleFactor = Math.pow(maxZoom, 1.0 / (double)nSteps);
                LogUtils.d(TAG, "steps: " + nSteps);
                LogUtils.d(TAG, "scale factor: " + scaleFactor);
                cameraFeatures.mZoomRatios = new ArrayList<>();
                cameraFeatures.mZoomRatios.add(100);
                double zoom = 1.0;
                for(int i=0; i < nSteps - 1; i++) {
                    zoom *= scaleFactor;
                    cameraFeatures.mZoomRatios.add((int)(zoom * 100));
                }
                cameraFeatures.mZoomRatios.add((int)(maxZoom * 100));
                cameraFeatures.mMaxZoom = cameraFeatures.mZoomRatios.size() - 1;
            }
        }

        int [] faceModes = mCameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        if(null != faceModes) {
            for (int faceMode : faceModes) {
                LogUtils.d(TAG, "face detection mode: " + faceMode);
                // Although we currently only make use of the "SIMPLE" features, some devices (e.g., Nexus 6) support FULL and not SIMPLE.
                // We don't support SIMPLE yet, as I don't have any devices to test this.
                if (CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL == faceMode) {
                    cameraFeatures.mSupportFaceDetection = true;
                }
            }
            if (cameraFeatures.mSupportFaceDetection) {
                Integer faceCount = mCameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
                if (null == faceCount || faceCount <= 0) {
                    cameraFeatures.mSupportFaceDetection = false;
                }
            }
        }

        boolean capabilitiesRaw = false;
        int [] capabilities = mCameraCharacteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
        if(null != capabilities) {
            for (int capability : capabilities) {
                if (capability == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) {
                    capabilitiesRaw = true;
                }
            }
        }
        LogUtils.d(TAG, "capabilities raw: " + capabilitiesRaw);

        StreamConfigurationMap configs = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        if(null != configs) {
            Size[] cameraPictureSizes = configs.getOutputSizes(ImageFormat.JPEG);
            cameraFeatures.mPictureSizes = new ArrayList<>();
            for (Size cameraSize : cameraPictureSizes) {
                LogUtils.d(TAG, "picture size: " + cameraSize.getWidth() + " x " + cameraSize.getHeight());
                cameraFeatures.mPictureSizes.add(new Size(cameraSize.getWidth(), cameraSize.getHeight()));
            }

            Size[] cameraVideoSizes = configs.getOutputSizes(MediaRecorder.class);
            cameraFeatures.mVideoSizes = new ArrayList<>();
            for (Size cameraSize : cameraVideoSizes) {
                LogUtils.d(TAG, "mVideo size: " + cameraSize.getWidth() + " x " + cameraSize.getHeight());
                if (cameraSize.getWidth() > 4096 || cameraSize.getHeight() > 2160)
                    continue; // Nexus 6 returns these, even though not supported?!
                cameraFeatures.mVideoSizes.add(new Size(cameraSize.getWidth(), cameraSize.getHeight()));
            }

            Size[] cameraPreviewSizes = configs.getOutputSizes(SurfaceTexture.class);
            cameraFeatures.mPreviewSizes = new ArrayList<>();
            for (Size cameraSize : cameraPreviewSizes) {
                LogUtils.d(TAG, "preview size: " + cameraSize.getWidth() + " x " + cameraSize.getHeight());
                if (cameraSize.getWidth() > PicSizeUtils.sScreenSize.x || cameraSize.getHeight() > PicSizeUtils.sScreenSize.y) {
                    // Nexus 6 returns these, even though not supported?! (get green corruption lines if we allow these)
                    // Google Camera filters anything larger than height 1080, with a todo saying to use device's measurements
                    continue;
                }
                cameraFeatures.mPreviewSizes.add(new Size(cameraSize.getWidth(), cameraSize.getHeight()));
            }
        }

        Boolean flashValueInfo = mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        if( null != flashValueInfo && flashValueInfo) {
            cameraFeatures.mSupportedFlashValues = new ArrayList<>();
            cameraFeatures.mSupportedFlashValues.add(FlashValue.FLASH_OFF);
            cameraFeatures.mSupportedFlashValues.add(FlashValue.FLASH_AUTO);
            cameraFeatures.mSupportedFlashValues.add(FlashValue.FLASH_ON);
            cameraFeatures.mSupportedFlashValues.add(FlashValue.FLASH_TORCH);
        }

        int [] supportedFocusModes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES); // Android format
        if(null != supportedFocusModes) {
            cameraFeatures.mSupportedFocusValues = new ArrayList<>();
            for (int focusMode : supportedFocusModes) {
                cameraFeatures.mSupportedFocusValues.add(CameraParametersConverter.camera2FocusValueToComm(focusMode));
            }
        }
        Integer maxNumFocusArea = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        if(null != maxNumFocusArea) {
            cameraFeatures.mMaxNumFocusAreas = maxNumFocusArea;
        }

        int [] supportedWBModes = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES); // Android format
        if(null != supportedWBModes) {
            cameraFeatures.mSupportedWhiteBalanceValues = new ArrayList<>();
            for (int wbMode : supportedWBModes) {
                cameraFeatures.mSupportedWhiteBalanceValues.add(CameraParametersConverter.camera2WBValueToComm(wbMode));
            }
        }

        Range<Integer> isoRange = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        if( isoRange != null ) {
            cameraFeatures.mSupportIsoRange = true;
            cameraFeatures.mMinIso = isoRange.getLower();
            cameraFeatures.mMaxIso = isoRange.getUpper();
            // we only expose exposure_time if iso_range is supported
            Range<Long> exposureTimeRange = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
            if(null != exposureTimeRange) {
                cameraFeatures.mSupportExposureTime = true;
                cameraFeatures.mMinExposureTime = exposureTimeRange.getLower();
                cameraFeatures.mMaxExposureTime = exposureTimeRange.getUpper();
            }
        }

        cameraFeatures.mSupportExposureLock = true;
        Range<Integer> exposureRange = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        if(null != exposureRange) {
            cameraFeatures.mMinExposureCompensation = exposureRange.getLower();
            cameraFeatures.mMaxExposureCompensation = exposureRange.getUpper();
        }
        Rational exposureStep = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP);
        if(null != exposureStep) {
            cameraFeatures.mExposureCompensationStep = exposureStep.floatValue();
        }

        cameraFeatures.mSupportVideoStabilization = true;

        cameraFeatures.mCanDisableShutterSound = true;

        return cameraFeatures;
    }

    private void createCaptureSession() {
        LogUtils.d(TAG, "create capture session");

        if( mCameraCaptureSession != null ) {
            LogUtils.d(TAG, "close old capture session");
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if(mCameraDevice == null) {
            LogUtils.d(TAG, "camera not available!");
            return;
        }

        LogUtils.d(TAG, "camera: " + mCameraDevice);

        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(getCameraSurface());

            mPictureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mPictureBuilder.addTarget(mImageReader.getSurface());

            mCameraDevice.createCaptureSession(Arrays.asList(getCameraSurface(), mImageReader.getSurface()), mSessionStateCallback, getCameraBackgroundHandler());
        }
        catch(CameraAccessException e) {
            //captureSession = null;
            LogUtils.e(TAG, "failed to create capture request");
            LogUtils.e(TAG, "reason: " + e.getReason());
            LogUtils.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public int getCameraDisplayOrientation() {
        return 0;
    }

    @Override
    public void startPreview() {
        LogUtils.d(TAG, "startPreview");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }

        setRepeatingRequest(getPreviewBuilder().build());
    }

    @Override
    public void stopPreview() {
        LogUtils.d(TAG, "stopPreview");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }

        try {
            mCameraCaptureSession.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setRepeatingRequest(CaptureRequest request) {
        LogUtils.d(TAG, "setRepeatingRequest");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }

        try {
//            // Auto focus should be continuous for camera preview.
////            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
////                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
//            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
//                    CaptureRequest.CONTROL_AF_MODE_AUTO);
//            // Flash is automatically enabled when necessary.
//            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
//                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            mCameraCaptureSession.setRepeatingRequest(request, mPreviewCaptureCallback, getCameraBackgroundHandler());

        }catch(CameraAccessException e) {
            LogUtils.e(TAG, "failed to set repeating request");
            LogUtils.e(TAG, "reason: " + e.getReason());
            LogUtils.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private void setCaptureRequest(CaptureRequest request) {
        LogUtils.d(TAG, "setRepeatingRequest");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }

        try {
            mCameraCaptureSession.capture(request, mPreviewCaptureCallback, getCameraBackgroundHandler());

        }catch(CameraAccessException e) {
            LogUtils.e(TAG, "failed to set repeating request");
            LogUtils.e(TAG, "reason: " + e.getReason());
            LogUtils.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            // need to let preview work when capture request has problem
            setRepeatingRequest(getPreviewBuilder().build());
//            throw new RuntimeException();
        }
    }

    @Override
    public void closeCamera() {
        LogUtils.d(TAG, "try to close camera");

        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraCaptureSession) {
                mCameraCaptureSession.close();
                mCameraCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if(null != mCameraSurface) {
                mCameraSurface.release();
                mCameraSurface = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    @Override
    public void setPictureSize(Size pictureSize) {
        if(null == mCameraManager) {
            LogUtils.e(TAG, "camera manager is null when set picture size !");
            return;
        }

        SurfaceTexture surfaceTexture = getSurfaceTexture();
        if(null == surfaceTexture) {
            LogUtils.e(TAG, "surface texture from texture view is null when set picture size !");
            return;
        }

        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if(null == map) {
            LogUtils.e(TAG, "get not stream configuration map when set picture size !");
            return;
        }



        if(null == pictureSize) {
            pictureSize = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new SizeComparator());
        }

        if(null != getCameraStatusListener()) {
            getCameraStatusListener().onCameraDisplaySizeChanged(pictureSize.getWidth(), pictureSize.getHeight());
        }

        mImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(),
                ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, getCameraBackgroundHandler());

        mPreviewSize = PicSizeUtils.getOptimalPreviewSize(Arrays.asList(map.getOutputSizes(SurfaceTexture.class)), pictureSize);
        setPreviewSize(surfaceTexture, mPreviewSize);

        mCameraSurface = new Surface(surfaceTexture);

        createCaptureSession();
    }

    private void setPreviewSize(SurfaceTexture target, Size previewSize) {
        if(null == target) {
            LogUtils.e(TAG, "preview size will apply on a null target !");
            return;
        }

        if(null == previewSize) {
            LogUtils.e(TAG, "null preview size when set preview size !");
            return;
        }

        target.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
    }

    private class SizeComparator implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    @Override
    public boolean supportAutoFocus() {
        if(null == getPreviewBuilder().get(CaptureRequest.CONTROL_AF_MODE)) {
            return true;
        }

        Integer focus_mode = getPreviewBuilder().get(CaptureRequest.CONTROL_AF_MODE);

        return null != focus_mode
                && (focus_mode == CaptureRequest.CONTROL_AF_MODE_AUTO || focus_mode == CaptureRequest.CONTROL_AF_MODE_MACRO);
    }

    private final int DEFAULT_FOCUS_DISTANCE = 2;
    @Override
    public void autoFocus() {
        if(null == mCameraDevice || null == mCameraCaptureSession){
            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onAutoFocus(false, DEFAULT_FOCUS_DISTANCE, -1);
            }
        }

//        {
//            MeteringRectangle [] areas = getPreviewBuilder().get(CaptureRequest.CONTROL_AF_REGIONS);
//            for(int i=0;areas != null && i<areas.length;i++) {
//                Log.d(TAG, i + " focus area: " + areas[i].getX() + " , " + areas[i].getY() + " : " + areas[i].getWidth() + " x " + areas[i].getHeight() + " weight " + areas[i].getMeteringWeight());
//            }
//        }
//        {
//            MeteringRectangle [] areas = getPreviewBuilder().get(CaptureRequest.CONTROL_AE_REGIONS);
//            for(int i=0;areas != null && i<areas.length;i++) {
//                Log.d(TAG, i + " metering area: " + areas[i].getX() + " , " + areas[i].getY() + " : " + areas[i].getWidth() + " x " + areas[i].getHeight() + " weight " + areas[i].getMeteringWeight());
//            }
//        }

        mRequestState = RequestState.STATE_WAITING_AUTO_FOCUS;

        try{
            getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            setRepeatingRequest(getPreviewBuilder().build());
            getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            setCaptureRequest(getPreviewBuilder().build());
        }catch(Exception e){
            e.printStackTrace();
            mRequestState = RequestState.STATE_NORMAL;
            if(null != getCameraStatusListener()) {
                getCameraStatusListener().onAutoFocus(false, DEFAULT_FOCUS_DISTANCE, -1);
            }
        }

        // ensure set back to idle
        getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
    }

    @Override
    public void cancelAutoFocus() {
        if (null == mCameraDevice || null == mCameraCaptureSession) {
            return;
        }

        mRequestState = RequestState.STATE_NORMAL;

        try {
            getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setCaptureRequest(getPreviewBuilder().build());
            getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            setRepeatingRequest(getPreviewBuilder().build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean setFocusAndMeteringArea(List<Area> areas) {
        return false;
    }

    @Override
    public void clearFocusAndMetering() {
        if(null == mCameraCharacteristics) {
            return;
        }

        Rect sensorRect = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
        if(null == sensorRect) {
            return;
        }

        boolean hasFocus = false;
        boolean hasMetering = false;

        Integer maxRegionsAF = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
        if(null != maxRegionsAF && maxRegionsAF > 0) {
            hasFocus = true;
            MeteringRectangle[] afRegions = new MeteringRectangle[1];
            afRegions[0] = new MeteringRectangle(0, 0, sensorRect.width()-1, sensorRect.height()-1, 0);

            setBuilderValue(CaptureRequest.CONTROL_AF_REGIONS, afRegions);
        }


        Integer maxRegionsAE = mCameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
        if( null != maxRegionsAE && maxRegionsAE > 0 ) {
            hasMetering = true;
            MeteringRectangle[] aeRegions = new MeteringRectangle[1];
            aeRegions[0] = new MeteringRectangle(0, 0, sensorRect.width() - 1, sensorRect.height() - 1, 0);
            setBuilderValue(CaptureRequest.CONTROL_AE_REGIONS, aeRegions);
        }

        if( hasFocus || hasMetering ) {
            try {
                setRepeatingRequest(getPreviewBuilder().build());
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void takePicture() {
        LogUtils.d(TAG, "takePicture");
        preCaptureBeforeTakePicture();
    }

    private void preCaptureBeforeTakePicture() {
        LogUtils.d(TAG, "preCaptureBeforeTakePicture");

        if(null == mCameraDevice || null == mCameraCaptureSession) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }
        // first run pre capture sequence
//        getPreviewBuilder().set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
//        getPreviewBuilder().set(CaptureRequest.CONTROL_AE_PRE_CAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRE_CAPTURE_TRIGGER_IDLE);
//        setCaptureRequest();
//        setRepeatingRequest();
//
//        mRequestState = RequestState.STATE_WAITING_PRE_CAPTURE_START;
//        mPreCaptureStartedTime = System.currentTimeMillis();
//
//        getPreviewBuilder().set(CaptureRequest.CONTROL_AE_PRE_CAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRE_CAPTURE_TRIGGER_START);
//        setCaptureRequest();
        try {
            // use a separate builder for pre capture - otherwise have problem that if we take photo with flash auto/on of dark scene,
            // then point to a bright scene, the auto exposure isn't running until we auto focus again
            final CaptureRequest.Builder preCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            preCaptureBuilder.set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
            getBuilderSettingStorage().setupBuilder(preCaptureBuilder);

            preCaptureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_IDLE);
            preCaptureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);

            preCaptureBuilder.addTarget(getCameraSurface());

            mRequestState = RequestState.STATE_WAITING_PRE_CAPTURE_START;
            mPreCaptureStartedTime = System.currentTimeMillis();

            // first set pre capture to idle - this is needed, otherwise we hang in state STATE_WAITING_PRE_CAPTURE_START,
            // because pre capture already occurred whilst auto focusing, and it doesn't occur again unless we first set the pre capture trigger to idle
            mCameraCaptureSession.capture(preCaptureBuilder.build(), mPreviewCaptureCallback, getCameraBackgroundHandler());
            mCameraCaptureSession.setRepeatingRequest(preCaptureBuilder.build(), mPreviewCaptureCallback, getCameraBackgroundHandler());

            // now set pre capture
            preCaptureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mCameraCaptureSession.capture(preCaptureBuilder.build(), mPreviewCaptureCallback, getCameraBackgroundHandler());
        }
        catch(CameraAccessException e) {
            LogUtils.e(TAG, "failed to pre capture");
            LogUtils.e(TAG, "reason: " + e.getReason());
            LogUtils.e(TAG, "message: " + e.getMessage());

            e.printStackTrace();
        }
    }

    private void takePictureAfterPreCapture() {
        LogUtils.d(TAG, "takePictureAfterPreCapture");

        if(null == mCameraDevice || null == mCameraCaptureSession) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }

        getStillCaptureBuilder().set(CaptureRequest.CONTROL_CAPTURE_INTENT, CaptureRequest.CONTROL_CAPTURE_INTENT_STILL_CAPTURE);
        //stillBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        //stillBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        mRequestState = RequestState.STATE_CAPTURE;
        getStillCaptureBuilder().addTarget(getCameraSurface()); // Google Camera adds the preview surface as well as capture surface, for still capture
        getStillCaptureBuilder().addTarget(mImageReader.getSurface());

        stopPreview(); // need to stop preview before capture (as done in Camera2Basic; otherwise we get bugs such as flash remaining on after taking a photo with flash)
        setCaptureRequest(getStillCaptureBuilder().build());

        // call onCameraShutter here as soon as possible, otherwise maybe too slow to follow the take picture
        if(null != getCameraStatusListener()) {
            getCameraStatusListener().onCameraShutter();
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
        getBuilderSettingStorage().setPictureRotation(getStillCaptureBuilder(), rotation);
    }

    @Override
    public void setWhiteBalance(String wbValue) {
        if (wbValue != null) {
            LogUtils.d(TAG, "setWhiteBalance: " + wbValue);

            int convertWB = CameraParametersConverter.commWBValueToCamera2(wbValue);

            getBuilderSettingStorage().setWhiteBalance(getPreviewBuilder(), convertWB);
            getBuilderSettingStorage().setWhiteBalance(getStillCaptureBuilder(), convertWB);

            setCaptureRequest(getPreviewBuilder().build());
            setRepeatingRequest(getPreviewBuilder().build());
        }
    }

    @Override
    public void setISO(String isoValue) {
        if (null != isoValue) {
            LogUtils.d(TAG, "setISO: " + isoValue);

            getBuilderSettingStorage().setIso(getPreviewBuilder(), isoString2Int(isoValue));
            getBuilderSettingStorage().setIso(getStillCaptureBuilder(), isoString2Int(isoValue));

            setCaptureRequest(getPreviewBuilder().build());
            setRepeatingRequest(getPreviewBuilder().build());
        }
    }

    private int isoString2Int(String isoValue) {
        if("auto".equalsIgnoreCase(isoValue)){
            return 0;
        }else {
            return Integer.parseInt(isoValue);
        }
    }

    @Override
    public void setFlashValue(String flashValue) {
        if (flashValue != null) {
            LogUtils.d(TAG, "setFlashValue: " + flashValue);

            getBuilderSettingStorage().setFlashValue(getPreviewBuilder(), flashValue);
            getBuilderSettingStorage().setFlashValue(getStillCaptureBuilder(), flashValue);

            // if set capture request here the flash may not work normally
//            setCaptureRequest();
            setRepeatingRequest(getPreviewBuilder().build());
        }
    }

    @Override
    public void setFocusValue(String focusValue) {
        if (focusValue != null) {
            LogUtils.d(TAG, "setFocusValue: " + focusValue);

            int convertFocusValue = CameraParametersConverter.commFocusValueToCamera2(focusValue);

            getPreviewBuilder().set(CaptureRequest.CONTROL_AF_MODE, convertFocusValue);
            if(FocusValue.FOCUS_INFINITY.equalsIgnoreCase(focusValue)){
                getPreviewBuilder().set(CaptureRequest.LENS_FOCUS_DISTANCE,0.0f);
            }

            setRepeatingRequest(getPreviewBuilder().build());
        }
    }

    @Override
    public void setShutterSpeed(int shutterSpeed) {

    }

    private <T> void setBuilderValue(CaptureRequest.Key<T> key, T value){
        LogUtils.d(TAG, "setBuilderValue: key,"+key.getName()+"---value,"+value);
        getPreviewBuilder().set(key, value);
        getStillCaptureBuilder().set(key, value);
    }
}
