package com.lix.camera.ui.function;

import android.graphics.SurfaceTexture;

/**
 * Created by lix on 2016/8/11.
 *
 */
public interface ITextureView {

    boolean inAvailableState();

    void setAspectRatio(final int width, final int height);

    SurfaceTexture getCameraDisplayTexture();

    void forbidUseCamera();
    void forbidRecordPhoto();

}
