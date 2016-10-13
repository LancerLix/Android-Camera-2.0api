package com.lix.camera.base;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import com.lix.camera.utils.ImageTools;
import com.lix.camera.utils.LogUtils;

import java.io.FileOutputStream;
import java.io.IOException;
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

    private WindowManager mWindowManager;

    private CameraManager mCameraManager;

    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCameraCharacteristics;
    private String mCurrentCameraId;

    private CaptureRequest.Builder mPreviewBuilder = null;
    private CaptureRequest.Builder mPictureBuilder = null;

    private CameraCaptureSession mCameraCaptureSession = null;

    private Surface mTextureSurface = null;
    private ImageReader mImageReader = null;

    private Size mPreviewSize;

    private HandlerThread mCameraBackgroundThread;
    private Handler mCameraBackgroundHandler;

    private boolean mCameraDeviceOpening = false;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private int OPEN_CLOSE_LOCK_TIMEOUT = 2500;

    public AndroidCamera2(Context context) {
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
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

    private final CameraCaptureSession.CaptureCallback mPreviewCaptureCallback = new CameraCaptureSession.CaptureCallback() {

    };

    private final CameraCaptureSession.CaptureCallback mPictureCaptureCallback = new CameraCaptureSession.CaptureCallback() {

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            if(null != mCameraBackgroundHandler) {
                mCameraBackgroundHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        LogUtils.d(TAG, "new still image available");
                        Image image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte [] bytes = new byte[buffer.remaining()];
                        LogUtils.d(TAG, "read " + bytes.length + " bytes");
                        buffer.get(bytes);
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(ImageTools.getOutputMediaFile());
                            fos.write(bytes);
                            fos.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            if(null != fos) {
                                try {
                                    fos.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        image.close();
                        LogUtils.d(TAG, "done onImageAvailable");
                    }
                });
            }
        }
    };

    @Override
    public void openCamera(int cameraId) {
        LogUtils.d(TAG, "try to open camera");

        if(null == mCameraManager) {
            return;
        }

        if(mCameraDeviceOpening) {
            return;
        }

        try {

            String[] cameraIdList = mCameraManager.getCameraIdList();
            if(cameraIdList.length > cameraId) {
                mCurrentCameraId = cameraIdList[cameraId];
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);

                if (!mCameraOpenCloseLock.tryAcquire(OPEN_CLOSE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening. Dead lock may appear!");
                }

                mCameraDeviceOpening = true;
                mCameraManager.openCamera(mCurrentCameraId, mDeviceStateCallback, mCameraBackgroundHandler);
            }

        } catch (CameraAccessException | SecurityException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    @Override
    public void startCameraBackgroundThread() {
        mCameraBackgroundThread = new HandlerThread("CameraBackground");
        mCameraBackgroundThread.start();
        mCameraBackgroundHandler = new Handler(mCameraBackgroundThread.getLooper());
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
            mPreviewBuilder.addTarget(mTextureSurface);

            mPictureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mPictureBuilder.addTarget(mImageReader.getSurface());

            mCameraDevice.createCaptureSession(Arrays.asList(mTextureSurface, mImageReader.getSurface()), mSessionStateCallback, mCameraBackgroundHandler);
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
    public void startPreview() {
        LogUtils.d(TAG, "startPreview");

        setRepeatingRequest();
    }

    @Override
    public void stopPreview() {
        LogUtils.d(TAG, "stopPreview");
    }

    private void setRepeatingRequest() {
        LogUtils.d(TAG, "setRepeatingRequest");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }
        try {
            // Auto focus should be continuous for camera preview.
            mPreviewBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // Flash is automatically enabled when necessary.
            mPreviewBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            mCameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), mPreviewCaptureCallback, mCameraBackgroundHandler);
        }
        catch(CameraAccessException e) {
            LogUtils.e(TAG, "failed to set repeating request");
            LogUtils.e(TAG, "reason: " + e.getReason());
            LogUtils.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
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
            if(null != mTextureSurface) {
                mTextureSurface.release();
                mTextureSurface = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    @Override
    public void stopCameraBackgroundThread() {
        if(null == mCameraBackgroundThread) {
            LogUtils.e(TAG, "stopCameraBackgroundThread when camera background thread is null !");
            return;
        }

        mCameraBackgroundThread.quitSafely();
        try {
            mCameraBackgroundThread.join();
            mCameraBackgroundThread = null;
            mCameraBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraBackgroundHandler);

        mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), pictureSize);

        surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        mTextureSurface = new Surface(surfaceTexture);

        createCaptureSession();
    }

    private Size chooseOptimalSize(Size[] choices, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();

        int aspectRatioWidth = aspectRatio.getWidth();
        int aspectRatioHeight = aspectRatio.getHeight();

        Point screenSize = new Point();

        mWindowManager.getDefaultDisplay().getSize(screenSize);

        int minSelectWidth = screenSize.x;
        int minSelectHeight = screenSize.y;

        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * aspectRatioHeight / aspectRatioWidth &&
                    option.getWidth() >= minSelectWidth && option.getHeight() >= minSelectHeight) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new SizeComparator());
        } else {
            LogUtils.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
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
    public void setWhiteBalance(String wbValue) {

    }

    @Override
    public void setISO(String isoValue) {

    }

    @Override
    public void setFlashValue(String flashValue) {

    }

    @Override
    public void setShutterSpeed(int shutterSpeed) {

    }

    @Override
    public void takePicture() {
        LogUtils.d(TAG, "capture");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            LogUtils.d(TAG, "no camera or capture session");
            return;
        }
        try {
            mCameraCaptureSession.capture(mPictureBuilder.build(), mPictureCaptureCallback, mCameraBackgroundHandler);
        }
        catch(CameraAccessException e) {
            LogUtils.e(TAG, "failed to capture");
            LogUtils.e(TAG, "reason: " + e.getReason());
            LogUtils.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
