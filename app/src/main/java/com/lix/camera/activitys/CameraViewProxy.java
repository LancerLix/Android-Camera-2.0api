package com.lix.camera.activitys;

import android.graphics.SurfaceTexture;
import android.view.View;
import android.widget.Toast;

import com.lix.camera.R;
import com.lix.camera.ui.function.ITextureView;

/**
 * Created by lix on 2016/9/1.
 *
 * All camera view control and status gaining.
 */
public class CameraViewProxy implements ITextureView {

    private CameraActivityHolder mCameraActivityHolder;

    public CameraViewProxy(CameraActivityHolder holder) {
        mCameraActivityHolder = holder;
    }

    public CameraActivityHolder getHolder() {
        return mCameraActivityHolder;
    }

//==================================================================================================================

    @Override
    public boolean inAvailableState() {
        return !(null == getHolder() || null == getHolder().getTextureView())
                && getHolder().getTextureView().isAvailable();
    }

    @Override
    public void setAspectRatio(int width, int height) {
        if(null == getHolder() || null == getHolder().getTextureView()) {
            return;
        }

        getHolder().getTextureView().setAspectRatio(width, height);
    }

    @Override
    public SurfaceTexture getCameraDisplayTexture() {
        if(null == getHolder() || null == getHolder().getTextureView()) {
            return null;
        }

        return getHolder().getTextureView().getSurfaceTexture();
    }

    @Override
    public void forbidUseCamera() {
        if(null == getHolder() || null == getHolder().getCameraOpenFailedTip()) {
            return;
        }

        getHolder().getCameraOpenFailedTip().setVisibility(View.VISIBLE);
    }

    @Override
    public void forbidRecordPhoto() {
        if(null == getHolder() || null == getHolder().getActivity()) {
            return;
        }

        Toast.makeText(getHolder().getActivity(), R.string.camera_take_picture_failed, Toast.LENGTH_SHORT).show();
    }

//==================================================================================================================

}
