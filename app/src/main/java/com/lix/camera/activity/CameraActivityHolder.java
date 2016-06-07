package com.lix.camera.activity;

import com.lix.camera.R;
import com.lix.camera.ui.AutoFitTextureView;

import android.view.View;
import android.view.ViewStub;
import android.widget.Button;

public class CameraActivityHolder {
    
    private CameraActivity mCameraActivity;
    
    private AutoFitTextureView mTextureView;
    private ViewStub mCameraOpenFailedTip;
    private Button mShutterButton;
    
    public CameraActivityHolder(CameraActivity activity) {
        mCameraActivity = activity;
        
        if(null != mCameraActivity) {
            mTextureView = (AutoFitTextureView) mCameraActivity.findViewById(R.id.texture_view);
            mShutterButton = (Button) mCameraActivity.findViewById(R.id.btn_take_picture);
            mCameraOpenFailedTip = (ViewStub) mCameraActivity.findViewById(R.id.camera_open_failed_tip);
        }
    }
    
    public AutoFitTextureView getTextureView() {
        return mTextureView;
    }
    
    public Button getShutterButton() {
        return mShutterButton;
    }
    
    public void showCameraOpenFailedTip() {
        if(null != mCameraOpenFailedTip) {
            mCameraOpenFailedTip.setVisibility(View.VISIBLE);
        }
    }
}
