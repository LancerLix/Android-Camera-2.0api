package com.lix.camera.base;

import android.graphics.SurfaceTexture;
import android.util.Size;

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

    public interface CameraStatusListener {
        void onCameraOpened();
        void onCameraDisconnected();
        void onCameraError();
        void onCameraDisplaySizeChanged(int width, int height);
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
