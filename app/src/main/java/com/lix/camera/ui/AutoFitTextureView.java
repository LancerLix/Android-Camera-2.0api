/*
 * Copyright 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lix.camera.ui;

import java.io.FileNotFoundException;
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

import com.lix.camera.utils.Constants;
import com.lix.camera.utils.ImageTools;

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
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureView {
    
    private final String TAG = AutoFitTextureView.class.getSimpleName();
    
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
    
    private Handler mUIMessageHandler = null;
    
    private HandlerThread mCameraBackgroundThread;
    private Handler mCameraBackgroundHandler;
    
    private boolean mCameraDeviceOpening = false;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private int OPEN_CLOSE_LOCK_TIMEOUT = 2500;

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    
    private CameraDevice.StateCallback mDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "camera opened");
            mCameraDeviceOpening = false;
            mCameraOpenCloseLock.release();
            mCameraDevice = camera;
            
            if(isAvailable()) {
                configOutputTarget(null);
                // note, this won't start the preview yet, but we create the previewBuilder in order to start setting camera parameters
                startPreview();
                if(null != mUIMessageHandler) {
                    mUIMessageHandler.sendEmptyMessage(Constants.CameraDeviceMsg.CAMERA_DEVICE_OPENED);
                }
            }else {
                closeCamera();
                openCamera(CameraCharacteristics.LENS_FACING_FRONT);
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "camera disconnected");
            mCameraDeviceOpening = false;
            mCameraOpenCloseLock.release();
            // need to set the camera to null first, as closing the camera may take some time, and we don't want any other
            // operations to continue (if called from main thread)
            mCameraDevice = null;
            Log.d(TAG, "onDisconnected: camera is now set to null");
            camera.close();
            
            if(null != mUIMessageHandler) {
                mUIMessageHandler.sendEmptyMessage(Constants.CameraDeviceMsg.CAMERA_DEVICE_DISCONNECTED);
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.d(TAG, "camera error");
            mCameraDeviceOpening = false;
            mCameraOpenCloseLock.release();
            mCameraDevice = null;
            Log.d(TAG, "onDisconnected: camera is now set to null");
            camera.close();
            Log.d(TAG, "onDisconnected: camera is now closed");
            
            if(null != mUIMessageHandler) {
                mUIMessageHandler.sendEmptyMessage(Constants.CameraDeviceMsg.CAMERA_DEVICE_ERROR);
            }
        }
    };
    
    private CameraCaptureSession.StateCallback mSessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d(TAG, "onConfigured: " + session);
            Log.d(TAG, "captureSession was: " + mCameraCaptureSession);
            
            mCameraCaptureSession = session;
            
            if(mCameraCaptureSession != null) {
                startPreview();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.d(TAG, "onConfigureFailed: " + session);
            Log.d(TAG, "captureSession was: " + mCameraCaptureSession);
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
                        Log.d(TAG, "new still image available");
                        Image image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer(); 
                        byte [] bytes = new byte[buffer.remaining()]; 
                        Log.d(TAG, "read " + bytes.length + " bytes");
                        buffer.get(bytes);
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(ImageTools.getOutputMediaFile());
                            fos.write(bytes);
                            fos.flush();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
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
                        image = null;
                        Log.d(TAG, "done onImageAvailable");
                    }
                });
            }
        }
    };

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }
    
    public void setUIMessageHandler(Handler handler) {
        mUIMessageHandler = handler;
    }
    
    private void initView(Context context) {
        if( null == context) {
            return;
        }
        
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(final int width, final int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        
        this.post(new Runnable() {
            
            @Override
            public void run() {
                mRatioWidth = width;
                mRatioHeight = height;
                requestLayout();
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }
    
    public void openCamera(int cameraId) {
        if(Constants.DEBUG) {
            Log.d(TAG, "try to open camera");
        }
        
        if(null == mCameraManager) {
            return;
        }
        
        if(mCameraDeviceOpening) {
            return;
        }
        
        try {
            
            String[] cameraIdList = mCameraManager.getCameraIdList(); 
            if(null != cameraIdList && cameraIdList.length > cameraId) {
                mCurrentCameraId = cameraIdList[cameraId];
                mCameraCharacteristics = mCameraManager.getCameraCharacteristics(mCurrentCameraId);
                
                if (!mCameraOpenCloseLock.tryAcquire(OPEN_CLOSE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                    throw new RuntimeException("Time out waiting to lock camera opening. Dead lock may appear!");
                }
                
                mCameraDeviceOpening = true;
                mCameraManager.openCamera(mCurrentCameraId, mDeviceStateCallback, mCameraBackgroundHandler);
            }
            
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }
    
    public void closeCamera() {
        if(Constants.DEBUG) {
            Log.d(TAG, "try to close camera");
        }
        
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
    
    public void configOutputTarget(Size pictureSize) {
        if(null == mCameraManager) {
            return;
        }
        
        StreamConfigurationMap map = mCameraCharacteristics.get(
                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        
        if(null == pictureSize) {
            pictureSize = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new SizeComparator());
        }
        
        mImageReader = ImageReader.newInstance(pictureSize.getWidth(), pictureSize.getHeight(),
                ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraBackgroundHandler);
        
        mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), pictureSize);
        setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
        
        SurfaceTexture surfaceTexture = getSurfaceTexture();
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
            Log.e(TAG, "Couldn't find any suitable preview size");
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
    
    public void startCameraBackgroundThread() {
        mCameraBackgroundThread = new HandlerThread("CameraBackground");
        mCameraBackgroundThread.start();
        mCameraBackgroundHandler = new Handler(mCameraBackgroundThread.getLooper());
    }
    
    public void stopCameraBackgroundThread() {
        mCameraBackgroundThread.quitSafely();
        try {
            mCameraBackgroundThread.join();
            mCameraBackgroundThread = null;
            mCameraBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void createCaptureSession() {
        Log.d(TAG, "create capture session");

        if( mCameraCaptureSession != null ) {
            Log.d(TAG, "close old capture session");
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        
        if(mCameraDevice == null) {
            Log.d(TAG, "camera not available!");
            return;
        }

        Log.d(TAG, "camera: " + mCameraDevice);
        
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(mTextureSurface);
            
            mPictureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mPictureBuilder.addTarget(mImageReader.getSurface());
            
            mCameraDevice.createCaptureSession(Arrays.asList(mTextureSurface, mImageReader.getSurface()), mSessionStateCallback, mCameraBackgroundHandler);
        }
        catch(CameraAccessException e) {
            //captureSession = null;
            Log.e(TAG, "failed to create capture request");
            Log.e(TAG, "reason: " + e.getReason());
            Log.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
    
    public void startPreview() {
        Log.d(TAG, "startPreview");
        
        setRepeatingRequest();
    }
    
    private void setRepeatingRequest() {
        Log.d(TAG, "setRepeatingRequest");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            Log.d(TAG, "no camera or capture session");
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
            Log.e(TAG, "failed to set repeating request");
            Log.e(TAG, "reason: " + e.getReason());
            Log.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
    
    public void takePicture() {
        Log.d(TAG, "capture");
        if( mCameraDevice == null || mCameraCaptureSession == null ) {
            Log.d(TAG, "no camera or capture session");
            return;
        }
        try {
            mCameraCaptureSession.capture(mPictureBuilder.build(), mPictureCaptureCallback, mCameraBackgroundHandler);
        }
        catch(CameraAccessException e) {
            Log.e(TAG, "failed to capture");
            Log.e(TAG, "reason: " + e.getReason());
            Log.e(TAG, "message: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException();
        }
    }
}
