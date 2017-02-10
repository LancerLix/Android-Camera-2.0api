package com.lix.camera.activities;

import com.lix.camera.R;
import com.lix.camera.presenters.IActivityControlPresenter;
import com.lix.camera.presenters.IActivityPermissionPresenter;
import com.lix.camera.presenters.impl.ActivityControlPresent;
import com.lix.camera.presenters.impl.ActivityPermissionPresent;
import com.lix.camera.presenters.impl.CameraPresenter;
import com.lix.camera.presenters.ICameraPresenter;
import com.lix.camera.ui.top.flashmenu.FlashSettingMenu;
import com.lix.camera.utils.Constants;
import com.lix.camera.utils.LogUtils;
import com.lix.camera.utils.ViewUtils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.support.annotation.NonNull;
import android.util.Size;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.SurfaceHolder;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;

public class CameraActivityListener {
    
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

    private SurfaceHolder.Callback mSurfaceHolderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            updateCameraViewSize(new Size(width, height));
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
    };

    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            if(permissionAlreadyChecked()) {
                LogUtils.d(TAG, "Permission has already checked!");
                updateCameraViewSize(new Size(width, height));
                openCamera();
                return;
            }

            checkApplicationPermission();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            updateCameraViewSize(new Size(width, height));
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private FlashSettingMenu.OnFlashMenuOptionSelectListener mOnFlashMenuOptionSelectListener =
            new FlashSettingMenu.OnFlashMenuOptionSelectListener() {
        @Override
        public void onOptionSelected(String tag) {
            setFlashValue(tag);
        }
    };

    private int mCurrentOrientation = 0;
    private int mUIRotation = 0;
    private CameraOrientationEventListener mCameraOrientationEventListener = null;
    private class CameraOrientationEventListener extends OrientationEventListener {

        private Activity mActivity;

        public CameraOrientationEventListener(Activity activity) {
            super(activity);
            mActivity = activity;
        }

        @Override
        public void onOrientationChanged(int orientation) {

            if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                LogUtils.e(TAG, "unknown mOrientation call back !");
                return;
            }

            updateCameraViewOrientation(orientation);

            mCurrentOrientation = ViewUtils.roundOrientation(orientation, mCurrentOrientation);
            int tmpUIRotation = ViewUtils.calculateUIRotationCompensation(mActivity, mCurrentOrientation, mUIRotation);

            if(mUIRotation == tmpUIRotation){
                return;
            }

            mUIRotation = tmpUIRotation;
        }
    }


    public CameraActivityListener(@NonNull CameraActivity cameraActivity,
                                  @NonNull CameraActivityHolder cameraActivityHolder) {

        mCameraPresenter = new CameraPresenter(cameraActivity, new CameraViewProxy(cameraActivityHolder));
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

        if(null != cameraActivityHolder.getTopOptionMenu()) {
            cameraActivityHolder.getTopOptionMenu().setOnFlashMenuOptionSelectListener(mOnFlashMenuOptionSelectListener);
        }

        if(null != cameraActivityHolder.getActivity()) {
            mCameraOrientationEventListener = new CameraOrientationEventListener(cameraActivityHolder.getActivity());
            mCameraOrientationEventListener.enable();
        }
    }

    public void onActivityCreate() {
    }

    public void onActivityResume() {
        openCamera();
    }

    public void onActivityPause() {
        closeCamera();
    }

    public void onActivityStop() {
    }

    public void onActivityDestroy() {
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

//=================================================================================================================//

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
            setAllowSavePicture(true);
            openCamera();
        }
    }

//=================================================================================================================//

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

//=================================================================================================================//

    private void updateCameraViewSize(Size viewSize) {
        getCameraPresenter().updateCameraViewSize(viewSize);
    }

    private void updateCameraViewOrientation(int orientation) {
        getCameraPresenter().updateCameraViewOrientation(orientation);
    }

    private void openCamera() {
        getCameraPresenter().openCamera(ICameraPresenter.CameraId.CAMERA_FACING_BACK);
    }

    private void closeCamera() {
        getCameraPresenter().closeCamera();
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

    private void setFlashValue(String flashValue) {
        getCameraPresenter().setFlashValue(flashValue);
    }
}
