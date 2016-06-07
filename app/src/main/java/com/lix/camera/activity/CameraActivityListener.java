package com.lix.camera.activity;

import com.lix.camera.R;
import com.lix.camera.utils.Constants;

import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.View.OnClickListener;

public class CameraActivityListener {
    
    private final String TAG = CameraActivityListener.class.getSimpleName();
    
    private CameraActivityHolder mCameraActivityHolder;
    
    public CameraActivityHolder getHolder() {
        return mCameraActivityHolder;
    }
    
    private boolean mHasExternalStoragePermission = false;
    public void setHasExternalStoragePermission(boolean permission) {
        mHasExternalStoragePermission = permission;
    }
    
    public boolean hasExternalStoragePermission() {
        return mHasExternalStoragePermission;
    }
    
    private Handler mCameraForegroundHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if(null == msg) {
                return;
            }
            
            switch(msg.what) {
            case Constants.CameraDeviceMsg.CAMERA_DEVICE_OPENED :
                break;  
            case Constants.CameraDeviceMsg.CAMERA_DEVICE_DISCONNECTED :
                break;
            case Constants.CameraDeviceMsg.CAMERA_DEVICE_ERROR :
                break;
            }
            super.handleMessage(msg);
        }
        
    };
    
    private OnClickListener mViewOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if(null == v) {
                return;
            }
            
            switch(v.getId()) {
                case R.id.btn_take_picture :
                    if(hasExternalStoragePermission()) {
                        takePicture();
                    }else {
                    }
                    break;
            }
        }
    };
    
    private SurfaceTextureListener mSurfaceTextureListener = new SurfaceTextureListener(){

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
    
    public CameraActivityListener(CameraActivityHolder cameraActivityHolder) {
        mCameraActivityHolder = cameraActivityHolder;
        initListener();
    }
    
    private void initListener() {
        if(null == getHolder()) {
            if(Constants.DEBUG) {
                Log.d(TAG, "initListener holder is null");
            }
            return;
        }
        
        if(null != getHolder().getTextureView()) {
            getHolder().getTextureView().setUIMessageHandler(mCameraForegroundHandler);
            getHolder().getTextureView().setSurfaceTextureListener(mSurfaceTextureListener);
        }
        
        if(null != getHolder().getShutterButton()) {
            getHolder().getShutterButton().setOnClickListener(mViewOnClickListener);
        }
    }
    
    private void takePicture() {
        if(null != getHolder() && null != getHolder().getTextureView()) {
            getHolder().getTextureView().takePicture();
        }else if(Constants.DEBUG){
            Log.d(TAG, "takePicture error occured");
        }
    }

}
