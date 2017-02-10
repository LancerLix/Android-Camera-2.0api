package com.lix.camera.model;

import android.graphics.SurfaceTexture;
import android.util.Size;
import android.view.SurfaceHolder;

import java.io.File;

/**
 * Created by lix on 2016/12/27.
 *
 */
public interface ICameraModel {

    enum CameraId {
        CAMERA_FACING_BACK,
        CAMERA_FACING_FRONT
    }

    interface CameraModelCallback {
        void onCameraOpened();
        void onCameraDisconnected();
        void onCameraError();
        void onCameraClosed();
        void onCameraDisplaySizeChanged(final int width, final int height);

        void onAutoFocusStart(int envIso);
        void onAutoFocusReturn(boolean success, int distance, int expIso);
        void onCameraShutter();
        void onCameraRawPicDataSaved(final byte[] data, final File savedFile);
        void onCameraJpegPicDataSaved(final byte[] data, final File savedFile);

    }

    void setCameraModelCallback(CameraModelCallback cameraModelCallback);
    void setSurfaceHolder(SurfaceHolder holder);
    void setSurfaceTexture(SurfaceTexture texture);

    void openCamera(CameraId cameraId);
    void startPreview();
    void stopPreview();
    void closeCamera();
    void setPictureSize(Size pictureSize);
    void autoFocus(float focusPosX, float focusPosY);
    void setBurstMode(int times);
    void takePicture();

    void updateCameraViewSize(Size viewSize);
    void updateCameraViewOrientation(int orientation);
    void setFlashValue(String flashValue);
}
