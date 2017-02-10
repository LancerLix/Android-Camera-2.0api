package com.lix.camera.presenters.impl;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Size;

import com.lix.camera.model.ICameraModel;
import com.lix.camera.model.impl.CameraModel;
import com.lix.camera.presenters.ICameraPresenter;
import com.lix.camera.ui.function.ICameraView;

import java.io.File;

import static com.lix.camera.presenters.ICameraPresenter.CameraId.CAMERA_FACING_BACK;

/**
 * Created by lix on 2016/8/11.
 *
 * Implements Camera presents interface used for camera api 2.0
 */
public class CameraPresenter implements ICameraPresenter {

    private ICameraModel mCameraModel;
    @NonNull
    private ICameraModel getCameraModel() {
        return mCameraModel;
    }

    private ICameraView mTextureView;
    private ICameraView getTextureView() {
        return mTextureView;
    }

    public CameraPresenter(Context context, ICameraView textureView) {
        mCameraModel = new CameraModel(context);
        mCameraModel.setCameraModelCallback(new CameraModelCallback());

        mTextureView = textureView;
    }

    private class CameraModelCallback implements ICameraModel.CameraModelCallback {

        @Override
        public void onCameraOpened() {
            if(null != getTextureView()) {
                // getCameraDisplayTexture() will never return null because the text view must be in available state here
                getCameraModel().setSurfaceTexture(getTextureView().getCameraDisplayTexture());
                getCameraModel().setPictureSize(null);
                // note, this won't start the preview yet, but we create the previewBuilder in order to start setting camera parameters
                getCameraModel().startPreview();
            }
        }

        @Override
        public void onCameraDisconnected() {
        }

        @Override
        public void onCameraClosed() {
        }

        @Override
        public void onCameraError() {
        }

        @Override
        public void onCameraDisplaySizeChanged(int width, int height) {
            if(null != getTextureView()) {
                getTextureView().setAspectRatio(width, height);
            }
        }

        @Override
        public void onAutoFocusStart(int envIso) {

        }

        @Override
        public void onAutoFocusReturn(boolean success, int distance, int expIso) {

        }

        @Override
        public void onCameraShutter() {

        }

        @Override
        public void onCameraRawPicDataSaved(byte[] data, File savedFile) {

        }

        @Override
        public void onCameraJpegPicDataSaved(byte[] data, File savedFile) {
            if(null != getTextureView()) {
                getTextureView().dealPictureResult(data, savedFile);
            }
        }
    }

    @Override
    public void openCamera(CameraId cameraId) {

        if(null == getTextureView() || !getTextureView().inAvailableState()) {
            return;
        }

        switch(cameraId) {
            case CAMERA_FACING_BACK:
                getCameraModel().openCamera(ICameraModel.CameraId.CAMERA_FACING_BACK);
                break;
            case CAMERA_FACING_FRONT:
                getCameraModel().openCamera(ICameraModel.CameraId.CAMERA_FACING_FRONT);
                break;
        }
    }

    @Override
    public void closeCamera() {
        getCameraModel().closeCamera();
    }

    @Override
    public void startPreview() {
        getCameraModel().startPreview();
    }

    @Override
    public void stopPreview() {
        getCameraModel().stopPreview();
    }

    @Override
    public void setPictureSize(Size pictureSize) {
        getCameraModel().setPictureSize(pictureSize);
    }

    @Override
    public void setAllowUseCamera(boolean allow) {
        if(!allow && null != getTextureView()) {
            getTextureView().forbidUseCamera();
        }
    }

    private boolean mAllowSavePicture;
    private boolean allowSavePicture() {
        return mAllowSavePicture;
    }
    @Override
    public void setAllowSavePicture(boolean allow) {
        mAllowSavePicture = allow;
    }

    @Override
    public void takePicture() {
        if(allowSavePicture()) {
            getCameraModel().takePicture();
        }else if(null != getTextureView()){
            getTextureView().forbidRecordPhoto();
        }
    }

    @Override
    public void updateCameraViewSize(Size viewSize) {
        getCameraModel().updateCameraViewSize(viewSize);
    }

    @Override
    public void updateCameraViewOrientation(int orientation) {
        getCameraModel().updateCameraViewOrientation(orientation);
    }

    @Override
    public void setFlashValue(String flashValue) {
        getCameraModel().setFlashValue(flashValue);
    }
}
