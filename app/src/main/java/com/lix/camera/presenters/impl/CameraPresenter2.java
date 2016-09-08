package com.lix.camera.presenters.impl;

import android.content.Context;
import android.hardware.camera2.CameraCharacteristics;
import android.util.Size;

import com.lix.camera.base.AndroidCamera2;
import com.lix.camera.base.BaseCamera;
import com.lix.camera.presenters.ICameraPresenter;
import com.lix.camera.ui.function.ITextureView;

/**
 * Created by lix on 2016/8/11.
 *
 * Implements Camera presents interface used for camera api 2.0
 */
public class CameraPresenter2 implements ICameraPresenter {

    private BaseCamera mCamera;

    private ITextureView mTextView;

    private boolean mAllowSavePicture;
    private boolean allowSavePicture() {
        return mAllowSavePicture;
    }

    public CameraPresenter2(Context context, ITextureView textureView) {
        mCamera = new AndroidCamera2(context);
        mCamera.setCameraStatusListener(new Camera2StatusListener());

        mTextView = textureView;
    }

    private class Camera2StatusListener implements BaseCamera.CameraStatusListener {

        @Override
        public void onCameraOpened() {

            if(mTextView.inAvailableState()) {
                // getCameraDisplayTexture() will return null is the text view is not in available state
                mCamera.setDisplayTexture(mTextView.getCameraDisplayTexture());
                mCamera.setPictureSize(null);
                // note, this won't start the preview yet, but we create the previewBuilder in order to start setting camera parameters
                mCamera.startPreview();
            }else {
                mCamera.closeCamera();
                mCamera.openCamera(CameraCharacteristics.LENS_FACING_FRONT);
            }

        }

        @Override
        public void onCameraDisconnected() {
        }

        @Override
        public void onCameraError() {
        }

        @Override
        public void onCameraDisplaySizeChanged(int width, int height) {
            mTextView.setAspectRatio(width, height);
        }
    }

    @Override
    public void openCamera(int cameraId) {
        if(null != mCamera) {
            mCamera.startCameraBackgroundThread();
            mCamera.openCamera(cameraId);
        }
    }

    @Override
    public void closeCamera() {
        if(null != mCamera) {
            mCamera.closeCamera();
            mCamera.stopCameraBackgroundThread();
        }
    }

    @Override
    public void startPreview() {
        if(null != mCamera) {
            mCamera.startPreview();
        }
    }

    @Override
    public void stopPreview() {
        if(null != mCamera) {
            mCamera.stopPreview();
        }
    }

    @Override
    public void setPictureSize(Size pictureSize) {
        if(null != mCamera) {
            mCamera.setPictureSize(pictureSize);
        }
    }

    @Override
    public void setAllowUseCamera(boolean allow) {
        if(!allow && null != mTextView) {
            mTextView.forbidUseCamera();
        }
    }

    @Override
    public void setAllowSavePicture(boolean allow) {
        mAllowSavePicture = allow;
    }

    @Override
    public void takePicture() {
        if(null != mCamera) {
            if(allowSavePicture()) {
                mCamera.takePicture();
            }else if(null != mTextView){
                mTextView.forbidRecordPhoto();
            }
        }
    }
}
