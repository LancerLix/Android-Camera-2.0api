package com.lix.camera.presenters;

/**
 * Created by lix on 2016/9/2.
 *
 * Activity permission presenter interface
 */
public interface IActivityPermissionPresenter {

    boolean hasCameraPermission();

    boolean hasWriteExtStoragePermission();

    void requestWriteExtStoragePermission();

    void requestCameraPermission();

    void requestBothCWPermission();
}
