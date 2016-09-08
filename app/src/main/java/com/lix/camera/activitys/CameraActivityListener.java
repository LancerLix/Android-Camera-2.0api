package com.lix.camera.activitys;

import com.lix.camera.R;
import com.lix.camera.presenters.IActivityControlPresenter;
import com.lix.camera.presenters.IActivityPermissionPresenter;
import com.lix.camera.presenters.impl.ActivityControlPresent;
import com.lix.camera.presenters.impl.ActivityPermissionPresent;
import com.lix.camera.presenters.impl.CameraPresenter2;
import com.lix.camera.presenters.ICameraPresenter;
import com.lix.camera.utils.Constants;
import com.lix.camera.utils.LogUtils;

import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;

public class CameraActivityListener{
    
    private final String TAG = CameraActivityListener.class.getSimpleName();

    private ICameraPresenter mCameraPresenter;
    private IActivityPermissionPresenter mActivityPermissionPresenter;
    private IActivityControlPresenter mActivityControlPresenter;

    @NonNull
    public ICameraPresenter getCameraPresenter() {
        return mCameraPresenter;
    }

    @NonNull
    public IActivityPermissionPresenter getActivityPermissionPresenter() {
        return mActivityPermissionPresenter;
    }

    @NonNull
    public IActivityControlPresenter getActivityControlPresenter() {
        return mActivityControlPresenter;
    }

    private boolean mPermissionAlreadyChecked = false;
    private boolean permissionAlreadyChecked() {
        return mPermissionAlreadyChecked;
    }
    
    private OnClickListener mViewOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if(null == v) {
                return;
            }
            
            switch(v.getId()) {
                case R.id.btn_take_picture :
                    takePicture();
                    break;
            }
        }
    };

    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    public CameraActivityListener(@NonNull CameraActivity cameraActivity,
                                  @NonNull CameraActivityHolder cameraActivityHolder) {

        mCameraPresenter = new CameraPresenter2(cameraActivity, new CameraViewProxy(cameraActivityHolder));
        mActivityPermissionPresenter = new ActivityPermissionPresent(cameraActivity);
        mActivityControlPresenter = new ActivityControlPresent(new ActivityViewProxy(cameraActivityHolder));

        initListener(cameraActivityHolder);
    }
    
    private void initListener(CameraActivityHolder cameraActivityHolder) {
        if(null == cameraActivityHolder) {
            LogUtils.d(TAG, "error environment when init listener!");
            return;
        }

        if(null != cameraActivityHolder.getTextureView()) {
            cameraActivityHolder.getTextureView().setSurfaceTextureListener(mSurfaceTextureListener);
        }

        if(null != cameraActivityHolder.getShutterButton()) {
            cameraActivityHolder.getShutterButton().setOnClickListener(mViewOnClickListener);
        }
    }

    public void onActivityCreate() {
        if(permissionAlreadyChecked()) {
            LogUtils.d(TAG, "Permission has already checked!");
            openCamera();
        }

        checkApplicationPermission();
    }

    public void onActivityResume() {
        startCameraPreview();
    }

    public void onActivityPause() {
        stopCameraPreview();
    }

    public void onActivityStop() {
    }

    public void onActivityDestroy() {
        closeCamera();
        System.exit(0);
    }

    public void onActivityRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        LogUtils.d(TAG, "onRequestPermissionsResult requestCode:" + requestCode
                + " permissions length:" + permissions.length
                + " grantResults length:" + grantResults.length);

        mPermissionAlreadyChecked = true;

        switch (requestCode) {
            case Constants.PermissionRequestCode.PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    openCamera();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    setAllowUseCamera(false);
                }
                break;
            }

            case Constants.PermissionRequestCode.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setAllowSavePicture(true);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    setAllowSavePicture(false);
                }
                break;
            }

            case Constants.PermissionRequestCode.PERMISSIONS_REQUEST_CAMERA_AND_STORAGE : {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    openCamera();
                    setAllowSavePicture(true);

                } else if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_DENIED) {

                    openCamera();
                    setAllowSavePicture(false);

                } else if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_DENIED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                    setAllowUseCamera(false);
                    setAllowSavePicture(true);

                } else {
                    setAllowUseCamera(false);
                    setAllowSavePicture(false);
                }
                break;
            }
        }
    }

    public boolean onActivityKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            LogUtils.d(TAG, "on back key down");
            getActivityControlPresenter().exitActivity();

        } else if (keyCode == KeyEvent.KEYCODE_MENU) {

            LogUtils.d(TAG, "on menu key down");

        } else if (keyCode == KeyEvent.KEYCODE_HOME) {

            LogUtils.d(TAG, "on home key down");
        }

        return true;
    }

//=========================================================================================================================


    private void checkApplicationPermission() {

        boolean hasCameraPermission = hasCameraPermission();
        boolean hasWriteStoragePermission = hasWriteExtStoragePermission();

        if (!hasCameraPermission && !hasWriteStoragePermission) {

            requestBothCWPermission();

        } else if (!hasCameraPermission) {

            requestCameraPermission();

        } else if (!hasWriteStoragePermission) {

            requestWriteExtStoragePermission();
            openCamera();

        } else {
            openCamera();
        }
    }

//=================================================================================================================

    private boolean hasCameraPermission() {
        return getActivityPermissionPresenter().hasCameraPermission();
    }

    private boolean hasWriteExtStoragePermission() {
        return getActivityPermissionPresenter().hasWriteExtStoragePermission();
    }

    private void requestCameraPermission() {
        getActivityPermissionPresenter().requestCameraPermission();
    }

    private void requestWriteExtStoragePermission() {
        getActivityPermissionPresenter().requestWriteExtStoragePermission();
    }

    private void requestBothCWPermission() {
        getActivityPermissionPresenter().requestBothCWPermission();
    }

//=================================================================================================================

    private void openCamera() {
        getCameraPresenter().openCamera(CameraCharacteristics.LENS_FACING_FRONT);
    }

    private void closeCamera() {
        getCameraPresenter().closeCamera();
    }

    private void startCameraPreview() {
        getCameraPresenter().startPreview();
    }

    private void stopCameraPreview() {
        getCameraPresenter().stopPreview();
    }

    private void setAllowUseCamera(boolean allow) {
        getCameraPresenter().setAllowUseCamera(allow);
    }

    private void setAllowSavePicture(boolean allow) {
        getCameraPresenter().setAllowSavePicture(allow);
    }

    private void takePicture() {
        getCameraPresenter().takePicture();
    }
}
