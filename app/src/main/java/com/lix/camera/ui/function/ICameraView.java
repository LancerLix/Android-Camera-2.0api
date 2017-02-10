package com.lix.camera.ui.function;

import android.graphics.SurfaceTexture;

import java.io.File;

/**
 * Created by lix on 2016/8/11.
 *
 */
public interface ICameraView {

    boolean inAvailableState();

    void setAspectRatio(final int width, final int height);

    SurfaceTexture getCameraDisplayTexture();

    void forbidUseCamera();
    void forbidRecordPhoto();

    // this function may not call on main UI thread
    void dealPictureResult(final byte[] picData, final File resultFile);

}
