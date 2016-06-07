
package com.lix.camera.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.lix.camera.R;
import com.lix.camera.utils.Constants;

public class CameraActivity extends Activity {
    
    private final String TAG = CameraActivity.class.getSimpleName();
    
    private CameraActivityHolder mCameraActivityHolder;
    
    private CameraActivityListener mCameraActivityListener;
    
    public CameraActivityHolder getHolder() {
        return mCameraActivityHolder;
    }
    
    public CameraActivityListener getListener() {
        return mCameraActivityListener;
    }
    
    private void checkApplicationPermission() {
        int cameraPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA);
        
        int writeStoragePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        
        if (PackageManager.PERMISSION_GRANTED != cameraPermission
                && PackageManager.PERMISSION_GRANTED != writeStoragePermission) {
            
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.PermissionRequestCode.PERMISSIONS_REQUEST_CAMERA_AND_STORAGE);
                
        } else if (PackageManager.PERMISSION_GRANTED != cameraPermission
                && PackageManager.PERMISSION_GRANTED == writeStoragePermission) {
            
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    Constants.PermissionRequestCode.PERMISSIONS_REQUEST_CAMERA);
                
        } else if (PackageManager.PERMISSION_GRANTED == cameraPermission
                && PackageManager.PERMISSION_GRANTED != writeStoragePermission) {
            
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.PermissionRequestCode.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            
            openCamera();
        } else {
            openCamera();
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(layout);
        
        mCameraActivityHolder = new CameraActivityHolder(this);
        
        mCameraActivityListener = new CameraActivityListener(mCameraActivityHolder);
    }

    @Override
    protected void onResume() {
        startCameraBackgroundThread();
        checkApplicationPermission();
        super.onResume();
    }

    @Override
    protected void onPause() {
        closeCamera();
        stopCameraBackgroundThread();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(Constants.DEBUG) {
            Log.d(TAG, "onRequestPermissionsResult requestCode:" + requestCode 
                    + " permissions length:" + permissions.length
                    + " grantResults length:" + grantResults.length);
        }
    
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
                    if(null != getHolder()) {
                        getHolder().showCameraOpenFailedTip();
                    }
                }
                break;
            }
            
            case Constants.PermissionRequestCode.PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(null != getListener()) {
                        getListener().setHasExternalStoragePermission(true);
                    }
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    if(null != getListener()) {
                        getListener().setHasExternalStoragePermission(false);
                    }
                }
                break;
            }
            
            case Constants.PermissionRequestCode.PERMISSIONS_REQUEST_CAMERA_AND_STORAGE : {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                    if(null != getListener()) {
                        getListener().setHasExternalStoragePermission(true);
                    }
                } else if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_DENIED) {
                    openCamera();
                    if(null != getListener()) {
                        getListener().setHasExternalStoragePermission(false);
                    }
                } else if (grantResults.length > 1
                        && grantResults[0] == PackageManager.PERMISSION_DENIED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    if(null != getHolder()) {
                        getHolder().showCameraOpenFailedTip();
                    }
                    if(null != getListener()) {
                        getListener().setHasExternalStoragePermission(true);
                    }
                } else {
                    if(null != getHolder()) {
                        getHolder().showCameraOpenFailedTip();
                    }
                    if(null != getListener()) {
                        getListener().setHasExternalStoragePermission(false);
                    }
                }
                break;
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
          if (keyCode == KeyEvent.KEYCODE_BACK)
          {
              AlertDialog isExit = new AlertDialog.Builder(this).create();
              isExit.setTitle("System");           
              isExit.setMessage("Exit?");         
              isExit.setButton("Yes", listenerButton);
              isExit.setButton2("No", listenerButton);
              isExit.show();
          }
          else if (keyCode == KeyEvent.KEYCODE_MENU) 
          {
              
          }
          else if (keyCode == KeyEvent.KEYCODE_HOME) 
          {
        
          }
          
          return super.onKeyDown(keyCode, event);
    }
    
    DialogInterface.OnClickListener listenerButton = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int which)
        {
            switch (which)
            {
                case AlertDialog.BUTTON_POSITIVE:
                    finish();
                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
                default:
                    break;
          
             }
         }
     };
     
     private void startCameraBackgroundThread() {
         if(null != getHolder() && null != getHolder().getTextureView()) {
             getHolder().getTextureView().startCameraBackgroundThread();
         }else if(Constants.DEBUG){
             Log.d(TAG, "startCameraBackgroundThread error occured");
         }
     }
     
     private void stopCameraBackgroundThread() {
         if(null != getHolder() && null != getHolder().getTextureView()) {
             getHolder().getTextureView().stopCameraBackgroundThread();
         }else if(Constants.DEBUG){
             Log.d(TAG, "stopCameraBackgroundThread error occured");
         }
     }
     
     private void openCamera() {
         if(null != getHolder() && null != getHolder().getTextureView()) {
             getHolder().getTextureView().openCamera(CameraCharacteristics.LENS_FACING_FRONT);
         }else if(Constants.DEBUG){
             Log.d(TAG, "openCamera error occured");
         }
     }
     
     private void closeCamera() {
         if(null != getHolder() && null != getHolder().getTextureView()) {
             getHolder().getTextureView().closeCamera();
         }else if(Constants.DEBUG){
             Log.d(TAG, "closeCamera error occured");
         }
     }
}
