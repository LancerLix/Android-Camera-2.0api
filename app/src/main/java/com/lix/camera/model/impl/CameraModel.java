package com.lix.camera.model.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.SurfaceHolder;

import com.lix.camera.base.AndroidCamera2;
import com.lix.camera.base.BaseCamera;
import com.lix.camera.model.ICameraModel;
import com.lix.camera.utils.FileUtils;
import com.lix.camera.utils.LogUtils;
import com.lix.camera.utils.MediaUtils;
import com.lix.camera.utils.PicSizeUtils;
import com.lix.camera.utils.SoundUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by lix on 2016/12/27.
 *
 */
public class CameraModel implements ICameraModel {

    private final String TAG = "CameraModel";

    private BaseCamera mCamera;
    @NonNull
    private BaseCamera getCamera() {
        return mCamera;
    }

    public CameraModel(Context context) {
        mCamera = new AndroidCamera2(context);
        mCamera.setCameraStatusListener(new Camera2StatusListener());

        startCameraThread();
    }

    private HandlerThread mCameraThread;
    @NonNull
    private HandlerThread getCameraThread() {
        return mCameraThread;
    }

    private Handler mCameraHandler;
    @NonNull
    private Handler getCameraHandler() {
        return mCameraHandler;
    }

    private void startCameraThread() {
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        getCamera().setCameraBackgroundHandler(mCameraHandler);
    }

//    private void stopCameraThread() {
//        if(null == getCameraThread()) {
//            LogUtils.e(TAG, "stopCameraBackgroundThread when camera background thread is null !");
//            return;
//        }
//
//        getCameraThread().quitSafely();
//        try {
//            getCameraThread().join();
//            mCameraThread = null;
//            mCameraHandler = null;
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }

    private boolean mCameraOpenSuccess = false;
    private boolean isCameraOpenSuccess() {
        return mCameraOpenSuccess && null != mCameraViewSize;
    }

    private boolean mBNeedTakePhotoWhenFocus = false;

    // environment iso that with no metering flash light working
    private int mEnvIso = -1;
    // exposure iso when flash light metering
    private int mExpIso = -1;

    private final int AUTO_FOCUS_TIMEOUT = 5000;
    private Runnable mAutoFocusTimeoutRun = new Runnable() {
        @Override
        public void run() {
            LogUtils.d(TAG, "auto focus timeout!");
            setPhase(PHASE_NORMAL);
            cancelAutoFocus();
        }
    };

    private BaseCamera.CameraFeatures mCameraFeatures = null;
    private int getSensorOrientation() {
        if(null != mCameraFeatures) {
            return mCameraFeatures.getSensorOrientation();
        }

        return 0;
    }
    private class Camera2StatusListener implements BaseCamera.CameraStatusListener {

        @Override
        public void onCameraFeaturesReach(BaseCamera.CameraFeatures cameraFeatures) {
            mCameraFeatures = cameraFeatures;
        }

        @Override
        public void onCameraOpened() {
            if(null != getCameraModelCallback()) {
                getCameraModelCallback().onCameraOpened();
            }

            setupCameraParameters();
            mCameraOpenSuccess = true;
        }

        @Override
        public void onCameraDisconnected() {
            mCameraOpenSuccess = false;
        }

        @Override
        public void onCameraClosed() {
            mCameraOpenSuccess = false;
        }

        @Override
        public void onCameraError() {
            mCameraOpenSuccess = false;
        }

        @Override
        public void onCameraDisplaySizeChanged(int width, int height) {
            if(null != getCameraModelCallback()) {
                getCameraModelCallback().onCameraDisplaySizeChanged(width, height);
            }
        }

        @Override
        public void onAutoFocus(boolean success, int distance, int iso) {
            LogUtils.d(TAG, "onAutoFocus: " + success);
            getCameraHandler().removeCallbacks(mAutoFocusTimeoutRun);

            if(mBNeedTakePhotoWhenFocus){
                setFocusState(success,System.currentTimeMillis(),success ? FOCUS_SUCCESS : FOCUS_FAILED);

                mExpIso = iso;
                if (null != getCameraModelCallback() && !isFrontCamera()) {
                    getCameraModelCallback().onAutoFocusReturn(success, distance, mExpIso);
                }

                try {
                    getCamera().cancelAutoFocus();
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }

                takePictureWhenFocused();
                mBNeedTakePhotoWhenFocus = false;
            }else{
                autoFocusCompleted(true,success, false, distance, iso);
            }
        }

        @Override
        public void onCameraShutter() {
            SoundUtils.getSingleton().play(SoundUtils.SOUND_SHUTTER);
        }

        @Override
        public void onCameraRawPicDataReach(byte[] data) {

        }

        @Override
        public void onCameraJpegPicDataReach(byte[] data) {
            LogUtils.d(TAG, "onCameraJpegPicDataReach");
            System.gc();


            setPhase(PHASE_NORMAL);
            handlePictureTaken(data);
        }

    }

    private void setupCameraParameters() {
        // Flash is automatically enabled when necessary.
        setFlashValue(BaseCamera.FlashValue.FLASH_AUTO);
        // Auto focus should be auto for camera preview.
        setFocusValue(BaseCamera.FocusValue.FOCUS_AUTO);
    }

    private int BURST_TO_NEXT_INTERVAL = 300;
    private void handlePictureTaken(byte[] data) {

        File picFile = savePictureDataToFile(data);

        if(null == picFile) {
            LogUtils.e(TAG, "handlePictureTaken error when save file error! ");
            return;
        }

        if(null != getCameraModelCallback()) {
            getCameraModelCallback().onCameraJpegPicDataSaved(data, picFile);
        }

        System.gc();
        // preview automatically stopped due to taking photo
        setPreviewStarted(false);
        //just for start preview
        setPhase(PHASE_NORMAL);
        startPreview();

        //lix
//        if(mActivity.isFromThirdApp()){
//            return;
//        }

        if (mRemainingBurstNum > 0) {
            setPhase(PHASE_TAKING_PHOTO);
            takePictureOnTimer(BURST_TO_NEXT_INTERVAL, false);

            LogUtils.d(TAG, "burst mode photos remaining:"+mRemainingBurstNum);
        }else {
            stopBurstPhotos();
            setPhase(PHASE_NORMAL);
        }
    }

    private File savePictureDataToFile(byte[] data) {
        File picFile = null;
        try {
            picFile = FileUtils.getOutputMediaFile(MediaUtils.MEDIA_FORMAT_IMAGE, mEnvIso, mExpIso);

            if(null == picFile){
                LogUtils.e(TAG, "failed to create directory");
                //lix
//                if (getUiHandler() != null) {
//                    Message msg = getUiHandler().obtainMessage();
//                    msg.what = Constants.MainMsg.PICFILE_DIRECTORY_ERROR;
//                    getUiHandler().sendMessage(msg);
//                }
                return null;
            }

            OutputStream outputStream;

            LogUtils.d(TAG, "onPictureTaken saved photo");
            outputStream = new FileOutputStream(picFile);
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();

            //lix
//                    if (getUiHandler() != null) {
//                        Message msg = getUiHandler().obtainMessage();
//                        msg.what = Constants.MainMsg.BROADCAST_FILE;
//                        Bundle b = new Bundle();
//                        b.putSerializable(Constants.MsgKey.FILE, picFile);
//                        if(mActivity.isFromThirdApp()){
//                            b.putByteArray(Constants.MsgKey.PICTURE_DATA, picData);
//                        }
//                        msg.setData(b);
//                        getUiHandler().sendMessage(msg);
//                    }


        }catch (IOException e) {
            e.getStackTrace();
        }

        return picFile;
    }

    private void autoFocusCompleted(boolean playSound, boolean success, boolean cancelled, int distance, int iso) {
        LogUtils.d(TAG, "autoFocusCompleted");
        LogUtils.d(TAG, "    playSound? " + playSound);
        LogUtils.d(TAG, "    success? " + success);
        LogUtils.d(TAG, "    cancelled? " + cancelled);

        mExpIso = iso;
        if (null != getCameraModelCallback()) {
            getCameraModelCallback().onAutoFocusReturn(success, distance, mExpIso);
        }
        if (cancelled) {
            setFocusState(false,-1,FOCUS_DONE);
        }else if(success){
            setFocusState(true,System.currentTimeMillis(),FOCUS_SUCCESS);
        }else{
            setFocusState(false,-1,FOCUS_FAILED);
        }

        if (!cancelled) {
            try {
                getCamera().cancelAutoFocus();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }

    }

    CameraModelCallback mCameraModelCallback;
    @Override
    public void setCameraModelCallback(CameraModelCallback cameraModelCallback) {
        mCameraModelCallback = cameraModelCallback;
    }

    public CameraModelCallback getCameraModelCallback() {
        return mCameraModelCallback;
    }

    @Override
    public void setSurfaceHolder(SurfaceHolder holder) {
        getCamera().setSurfaceHolder(holder);
    }

    @Override
    public void setSurfaceTexture(SurfaceTexture texture) {
        getCamera().setSurfaceTexture(texture);
    }

    private CameraId mCameraId;
    private CameraId getCameraId() {
        return mCameraId;
    }

    public final boolean isFrontCamera(){
        return CameraId.CAMERA_FACING_FRONT.equals(getCameraId());
    }

    @Override
    public void openCamera(CameraId cameraId) {
        mCameraId = cameraId;

        switch (getCameraId()) {
            case CAMERA_FACING_BACK:
                getCamera().openCamera(BaseCamera.CameraId.CAMERA_FACING_BACK);
                break;
            case CAMERA_FACING_FRONT:
                getCamera().openCamera(BaseCamera.CameraId.CAMERA_FACING_FRONT);
                break;
        }
    }

    private boolean mPreviewStarted = false;
    public final boolean hasPreviewStarted(){
        return mPreviewStarted;
    }

    protected void setPreviewStarted(boolean value){
        mPreviewStarted = value;
    }

    @Override
    public void startPreview() {
        if (!this.isTakingPhotoOrOnTimer() && !hasPreviewStarted()) {
            LogUtils.d(TAG, "starting the camera preview");

            setPhase(PHASE_NORMAL);
            try {
                getCamera().startPreview();
            } catch (RuntimeException e) {
                e.printStackTrace();
                return;
            }
            setPreviewStarted(true);

            //should call after start preview
            setFaceDetect();
        }
    }

    @Override
    public void stopPreview() {
        LogUtils.d(TAG, "pausePreview()");

        getCamera().stopPreview();
        setPhase(PHASE_PREVIEW_PAUSED);
        setPreviewStarted(false);
    }

    @Override
    public void closeCamera() {
        mCameraOpenSuccess = false;
        getCamera().closeCamera();
    }

    @Override
    public void setPictureSize(Size pictureSize) {
        getCamera().setPictureSize(pictureSize);
    }

    private static final int FOCUS_WAITING = 0;
    private static final int FOCUS_SUCCESS = 1;
    private static final int FOCUS_FAILED = 2;
    private static final int FOCUS_DONE = 3;

    private int mFocusState = FOCUS_DONE;
    private boolean mBSuccessfullyFocused = false;
    private final int FOCUS_STILL_ON_DURATION = 3500;
    private long mSuccessfullyFocusedTime = -1;

    public final boolean isSuccessFocusStillOn() {
        return mBSuccessfullyFocused
                && System.currentTimeMillis() < mSuccessfullyFocusedTime + FOCUS_STILL_ON_DURATION;
    }

    public void setFocusState(boolean success, long successTime,int state){
        mBSuccessfullyFocused = success;
        mSuccessfullyFocusedTime = successTime;
        mFocusState = state;
        LogUtils.d(TAG, "set mFocusState to " + mFocusState);
    }

    public boolean isFocusStateEq(int state){
        return mFocusState == state;
    }

    private float[] mAreaPos  = new float[2];
    @Override
    public void autoFocus(float focusPosX, float focusPosY) {
        if (!isCameraOpenSuccess()) {
            LogUtils.d(TAG, "try to reopen camera due to touch");
            return;
        }

        if (isTakingPhotoOrOnTimer() || isBursting() || !hasPreviewStarted()){
            return;
        }

        if(isFrontCamera()){
            //we return false here in order to dispatch onTouch Event
            //to activity to handle,
            return;
        }

        if(isFocusStateEq(FOCUS_WAITING)){
            return;
        }

        setFocusState(false,-1,FOCUS_WAITING);

        mAreaPos[0] = focusPosX;
        mAreaPos[1] = focusPosY;

        getCameraHandler().removeCallbacks(mTouchRun);
        getCameraHandler().post(mTouchRun);
    }

    private Runnable mTouchRun =  new Runnable() {
        public void run() {

            clearFocusAndMetering();
            startPreview();
            setFocusAndMeteringArea(mAreaPos[0], mAreaPos[1]);

            tryAutoFocus();
        }
    };

    private void tryAutoFocus(){
        LogUtils.d(TAG, "tryAutoFocus");
        if (!isCameraOpenSuccess()|| !hasPreviewStarted()) {
            LogUtils.d(TAG, "camera not opened, stop try auto focus");
            return;
        }

        if (isTakingPhotoOrOnTimer() || isFrontCamera()) {
            LogUtils.d(TAG, "currently taking a photo or isFrontFacing");
            return;
        }

        if (getCamera().supportAutoFocus()) {
            LogUtils.d(TAG, "try to start auto focus");
            setFocusState(false,-1,FOCUS_WAITING);

            try {
                if(getCamera().captureResultHasIso()) {
                    mEnvIso = getCamera().captureResultIso();
                }else {
                    mEnvIso = -1;
                }
                if (null != getCameraModelCallback()) {
                    getCameraModelCallback().onAutoFocusStart(mEnvIso);
                }

                getCameraHandler().removeCallbacks(mAutoFocusTimeoutRun);
                getCameraHandler().postDelayed(mAutoFocusTimeoutRun, AUTO_FOCUS_TIMEOUT);
                getCamera().autoFocus();
            } catch (RuntimeException e) {
                // just in case? We got a RuntimeException report here from 1 user on Google Play
                if(null != getCameraModelCallback()) {
                    getCameraModelCallback().onAutoFocusReturn(false, 0, -1);
                }
                e.printStackTrace();
            }
        }else {
            // do this so we get the focus box, for focus modes that support focus area, but don't support auto focus
            setFocusState(false,-1,FOCUS_DONE);
        }
    }

    private void setFocusAndMeteringArea(float mAreaPos1, float mAreaPos2) {

        if(null == mCameraViewSize) {
            LogUtils.e(TAG, "camera view size not reach when set focus metering area !");
            return;
        }

        ArrayList<BaseCamera.Area> areas = PicSizeUtils.getAreas(mAreaPos1, mAreaPos2,
                    isFrontCamera(), getCamera().getCameraDisplayOrientation(),
                    mCameraViewSize.getWidth(),
                    mCameraViewSize.getHeight());

        if (getCamera().setFocusAndMeteringArea(areas)) {
            LogUtils.d(TAG, "setFocusAndMeteringArea:" + "mAreaPos1--- " + mAreaPos2);
        }else {
            LogUtils.d(TAG, "didn't set focus area in this mode, may have set metering !");
        }
    }

    private void clearFocusAndMetering(){
        LogUtils.d(TAG, "clearFocusAreas()");
        getCamera().clearFocusAndMetering();
        setFocusState(false,-1,FOCUS_DONE);
    }

    private void cancelAutoFocus(){
        LogUtils.d(TAG, "cancelAutoFocus");

        try {
            getCamera().cancelAutoFocus();
        } catch (RuntimeException e) {
            // had a report of crash on some devices, see comment at https://sourceforge.net/p/opencamera/tickets/4/
            // made on 20140520
            LogUtils.d(TAG, "cancelAutoFocus() failed");
            e.printStackTrace();
        }

        autoFocusCompleted(false,true,true, 0, -1);
    }

    private final int PHASE_NORMAL = 0;
    private final int PHASE_TIMER = 1;
    private final int PHASE_TAKING_PHOTO = 2;
    private final int PHASE_PREVIEW_PAUSED = 3; // the paused state after taking a photo
    private int mPhase = PHASE_NORMAL;

    public void setPhase(int newPhase){
        mPhase = newPhase;
    }

    public final boolean isPhaseEq(int phase){
        return mPhase == phase;
    }

    public final boolean isTakingPhoto() {
        return mPhase == PHASE_TAKING_PHOTO;
    }

    public final boolean isTakingPhotoOrOnTimer() {
        return mPhase == PHASE_TAKING_PHOTO || mPhase == PHASE_TIMER;
    }

    public final boolean isOnTimer() {
        return mPhase == PHASE_TIMER;
    }

    @Override
    public void takePicture() {
        LogUtils.d(TAG, "takePicture called");

        if(stopTaking()){
            return;
        }

        setPhase(PHASE_TAKING_PHOTO);

        startPreview();

        mRemainingBurstNum = mBurstNum;

        if ( 0 == getDelayTimer()){
            takePictureNow();
        } else {
            takePictureOnTimer(getDelayTimer(), true);
        }
    }

    protected void takePictureNow() {
        if(!isCameraOpenSuccess()){
            setPhase(PHASE_NORMAL);
            return;
        }

        if (isSuccessFocusStillOn()) {
            LogUtils.d(TAG, "recently focused successfully, so no need to refocus, or isBursting");

            takePictureWhenFocused();
        }else if (getCamera().supportAutoFocus()) {

            LogUtils.d(TAG, "start auto focus to take picture");
            if(isFocusStateEq(FOCUS_WAITING)){
                LogUtils.d(TAG, "FOCUS_WAITING just waiting focus done and take picture");
                mBNeedTakePhotoWhenFocus = true;
                return;
            }

            // fix bug for auto focus areas error
            if (!isFocusStateEq(FOCUS_WAITING) && 0 == getDelayTimer()) {
                setFocusAndMeteringArea(PicSizeUtils.sScreenSize.x / 2,
                        PicSizeUtils.sScreenSize.y / 2);
            }

            try {
                if(getCamera().captureResultHasIso()) {
                    mEnvIso = getCamera().captureResultIso();
                }else {
                    mEnvIso = -1;
                }
                if (null != getCameraModelCallback() && !isFrontCamera()) {
                    getCameraModelCallback().onAutoFocusStart(mEnvIso);
                }

                getCameraHandler().removeCallbacks(mAutoFocusTimeoutRun);
                getCameraHandler().postDelayed(mAutoFocusTimeoutRun, AUTO_FOCUS_TIMEOUT);
                mBNeedTakePhotoWhenFocus = true;
                setFocusState(false, -1, FOCUS_WAITING);
                getCamera().autoFocus();
            } catch (RuntimeException e) {
                // just in case? We got a RuntimeException report here from 1 user on Google Play:
                // 21 Dec 2013, Xperia Go, Android 4.1
                if(null != getCameraModelCallback()) {
                    getCameraModelCallback().onAutoFocusReturn(false, 0, -1);
                }
                e.printStackTrace();
            }
        }else {
            LogUtils.d(TAG, "current focus mode not support auto focus! ");
            takePictureWhenFocused();
        }
    }

    protected void takePictureWhenFocused() {
        LogUtils.d(TAG, "takePictureWhenFocused, remaining_burst_photos " + mRemainingBurstNum);

        if(mRemainingBurstNum == 0){
            stopBurstPhotos();
            setPhase(PHASE_NORMAL);
            return;
        }

        mCameraRotation = mCurrentRotation;
        getCamera().setPictureRotation(mCameraRotation);

        getCameraHandler().postDelayed(mTakePicRun, 250);
    }

    private Runnable mTakePicRun = new Runnable() {

        @Override
        public void run() {
            takePhotoImmediately();
        }
    };

    protected void takePhotoImmediately() {
        LogUtils.d(TAG, "about to call takePicture");
        try {
            mRemainingBurstNum--;
            setFocusState(false, -1, FOCUS_DONE);
            getCamera().takePicture();

        } catch (RuntimeException e) {
            e.printStackTrace();
            setPhase(PHASE_NORMAL);
            startPreview();
        }
        LogUtils.d(TAG, "takePicture exit");
    }

    private long mTakePhotoTime = 0;
    private int mTakePhotoDelay = 0;
    private Timer mTakePictureTimer = new Timer();
    private TimerTask mTakePictureTimerTask = null;
    private Timer mBeepTimer = new Timer();
    private TimerTask mBeepTimerTask = null;

    public void setTakePhotoDelay(int value){
        mTakePhotoDelay = value;
    }

    public final int getDelayTimer(){
        return mTakePhotoDelay * 1000;
    }

    class TakePictureTimerTask extends TimerTask {
        public void run() {
            if (mBeepTimerTask != null) {
                mBeepTimerTask.cancel();
                mBeepTimerTask = null;
            }

            if (mTakePictureTimerTask != null){
                takePictureNow();
            }else {
                LogUtils.d(TAG, "takePictureTimerTask: don't take picture, as already cancelled");
            }
        }
    }

    class BeepTimerTask extends TimerTask {
        public void run() {
            try {
                SoundUtils.getSingleton().play(SoundUtils.SOUND_TIMER);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void takePictureOnTimer(long timerDelay, boolean repeated) {
        LogUtils.d(TAG, "takePictureOnTimer,timer_delay: " + timerDelay);

        if(repeated){
            setPhase(PHASE_TIMER);
            mTakePhotoTime = System.currentTimeMillis() + timerDelay;
        }

        LogUtils.d(TAG, "take photo at: " + mTakePhotoTime);

        mTakePictureTimer.schedule(mTakePictureTimerTask = new TakePictureTimerTask(), timerDelay);

        if (/*!mBMuteSound &&*/ repeated) {
            if (mBeepTimerTask != null) {
                mBeepTimerTask.cancel();
                mBeepTimerTask = null;
            }
            mBeepTimer.schedule(mBeepTimerTask = new BeepTimerTask(), 0, 1000);
        }
    }

    private void cancelTimer() {
        if (mTakePictureTimerTask != null) {
            mTakePictureTimerTask.cancel();
            mTakePictureTimerTask = null;
        }

        if (mBeepTimerTask != null) {
            mBeepTimerTask.cancel();
            mBeepTimerTask = null;
        }
    }

    private int mRemainingBurstNum;
    private int mBurstNum = 1;
    @Override
    public void setBurstMode(int num) {
        mBurstNum = num;
    }

    private void stopBurstPhotos() {
        mRemainingBurstNum = 0;
        mBNeedTakePhotoWhenFocus = false;
    }

    private boolean isFirstTaking(){
        return mRemainingBurstNum == mBurstNum;
    }

    private boolean isBursting() {
        return (mRemainingBurstNum > 0 && mBurstNum > 1);
    }

    private boolean stopTaking() {
        if (!isCameraOpenSuccess()) {
            LogUtils.d(TAG, "camera not opened!");
            setPhase(PHASE_NORMAL);
            return true;
        }

        if (isOnTimer()) {
            cancelTimer();
            stopBurstPhotos();
            setPhase(PHASE_NORMAL);
            return true;
        }

        if(isBursting() || isTakingPhoto()){
            stopBurstPhotos();
            setPhase(PHASE_NORMAL);
            return true;
        }

        return false;
    }

    private Size mCameraViewSize = null;
    @Override
    public void updateCameraViewSize(Size viewSize) {
        mCameraViewSize = viewSize;
    }

    // the camera rotation update by application rotation
    private int mCurrentRotation = 0;
    // the camera rotation saved by the moment of take picture
    private int mCameraRotation = 0;
    @Override
    public void updateCameraViewOrientation(int orientation) {

        // calculate view mOrientation to camera rotation
        orientation = (orientation + 45) / 90 * 90;
        int newRotation;
        int cameraOrientation = getSensorOrientation();
        if (isFrontCamera()) {
            newRotation = (cameraOrientation - orientation + 360) % 360;
        }
        else {
            newRotation = (cameraOrientation + orientation) % 360;
        }

        if (newRotation != mCurrentRotation) {
            LogUtils.d(TAG, " set Camera rotation from " +
                    mCurrentRotation + " to " + newRotation);
            mCurrentRotation = newRotation;
        }
    }

    @Override
    public void setFlashValue(String flashValue) {
        getCamera().setFlashValue(flashValue);
    }

    private void setFocusValue(String focusValue) {
        getCamera().setFocusValue(focusValue);
    }

    // front camera we use face if support, back camera we do not use
    private boolean mBSupportsFaceDetection = false;
    private boolean mBUseFaceDetection = false;
    private BaseCamera.Face[] mFacesDetected = null;
    //use face detect when CAMERA_FACING_FRONT if Supports
    public void setFaceDetect( ) {
        if (mBSupportsFaceDetection) {

            if (isFrontCamera()) {
                mBUseFaceDetection = true;
                try {
                    LogUtils.d(TAG, "start face detection...");
                    getCamera().startFaceDetection();
                } catch (RuntimeException e) {
                    // I didn't think this could happen, as we only call startFaceDetection() after we've called
                    // takePicture() or stopPreview(), which the Android docs say stops the face detection
                    // however I had a crash reported on Google Play for Open Camera v1.4
                    // 2 Jan 2014, "max_ax5", Android 4.0.3-4.0.4
                    // startCameraPreview() was called after taking photo in burst mode, but I tested with burst mode
                    // and face detection, and can't reproduce the crash on Galaxy Nexus
                    e.printStackTrace();
                }
                mFacesDetected = null;
            } else {
                if (mBUseFaceDetection) {
                    getCamera().stopFaceDetection();
                }
                mBUseFaceDetection = false;
                mFacesDetected = null;
            }
        }
    }
}
