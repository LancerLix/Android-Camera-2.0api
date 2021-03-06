package com.lix.camera.activities;

import com.lix.camera.R;
import com.lix.camera.ui.main.CameraTextureView;
import com.lix.camera.ui.top.TopOptionMenu;

import android.app.Activity;
import android.view.ViewStub;
import android.widget.ImageButton;

public class CameraActivityHolder {
    
    private CameraActivity mCameraActivity;
    
    private CameraTextureView mTextureView;

    private TopOptionMenu mTopOptionMenu;

    private ViewStub mCameraOpenFailedTip;
    private ImageButton mShutterButton;
    
    public CameraActivityHolder(CameraActivity activity) {
        mCameraActivity = activity;
        
        if(null != mCameraActivity) {
            mTopOptionMenu = (TopOptionMenu) mCameraActivity.findViewById(R.id.top_option_menu);
            mTextureView = (CameraTextureView) mCameraActivity.findViewById(R.id.texture_view);
            mShutterButton = (ImageButton) mCameraActivity.findViewById(R.id.btn_take_picture);
            mCameraOpenFailedTip = (ViewStub) mCameraActivity.findViewById(R.id.camera_open_failed_tip);
        }

        getTopOptionMenu().prepareContentView(TopOptionMenu.TYPE_NORMAL, true);
    }

    public Activity getActivity() {
        return mCameraActivity;
    }

    public CameraTextureView getTextureView() {
        return mTextureView;
    }
    
    public ImageButton getShutterButton() {
        return mShutterButton;
    }

    public ViewStub getCameraOpenFailedTip() {
        return mCameraOpenFailedTip;
    }

    public TopOptionMenu getTopOptionMenu() {
        return mTopOptionMenu;
    }
}
