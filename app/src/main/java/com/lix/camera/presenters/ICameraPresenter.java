package com.lix.camera.presenters;

import android.util.Size;

/**
 * Created by lix on 2016/8/11.
 *
 * Camera presenter interface
 */
public interface ICameraPresenter {

    void openCamera(int cameraId);
    void closeCamera();

    void startPreview();
    void stopPreview();

    void setPictureSize(Size pictureSize);

    void setAllowUseCamera(boolean allow);
    void setAllowSavePicture(boolean allow);

    void takePicture();

}
