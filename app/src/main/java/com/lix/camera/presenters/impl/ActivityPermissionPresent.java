package com.lix.camera.presenters.impl;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.lix.camera.presenters.IActivityPermissionPresenter;
import com.lix.camera.utils.Constants;

/**
 * Created by lix on 2016/9/2.
 *
 * Implements activity present interface
 */
public class ActivityPermissionPresent implements IActivityPermissionPresenter {

    private Activity mActivity;

    public ActivityPermissionPresent(Activity activity) {
        mActivity = activity;
    }

    @Override
    public boolean hasCameraPermission() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.CAMERA);
    }

    @Override
    public boolean hasWriteExtStoragePermission() {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void requestWriteExtStoragePermission() {
        ActivityCompat.requestPermissions(mActivity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                Constants.PermissionRequestCode.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void requestCameraPermission() {
        ActivityCompat.requestPermissions(mActivity,
                new String[]{Manifest.permission.CAMERA},
                Constants.PermissionRequestCode.PERMISSIONS_REQUEST_CAMERA);
    }

    @Override
    public void requestBothCWPermission() {
        ActivityCompat.requestPermissions(mActivity,
                new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                Constants.PermissionRequestCode.PERMISSIONS_REQUEST_CAMERA_AND_STORAGE);
    }


}
