package com.lix.camera.presenters;

import android.util.Size;

/**
 * Created by lix on 2016/8/11.
 *
 * Camera presenter interface
 */
public interface ICameraPresenter {

    enum CameraId {
        CAMERA_FACING_BACK,
        CAMERA_FACING_FRONT
    }

    void openCamera(CameraId cameraId);
    void closeCamera();

    void startPreview();
    void stopPreview();

    void setPictureSize(Size pictureSize);

    void setAllowUseCamera(boolean allow);
    void setAllowSavePicture(boolean allow);

    void takePicture();

    void updateCameraViewSize(Size viewSize);
    void updateCameraViewOrientation(int orientation);
    void setFlashValue(String flashValue);

}
