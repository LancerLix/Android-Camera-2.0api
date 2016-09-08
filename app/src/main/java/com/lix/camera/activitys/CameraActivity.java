
package com.lix.camera.activitys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import com.lix.camera.R;
import com.lix.camera.utils.LogUtils;

public class CameraActivity extends Activity {

    private final String TAG = CameraActivity.class.getSimpleName();
    
    private CameraActivityListener mCameraActivityListener;

    public CameraActivityListener getListener() {
        return mCameraActivityListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
        getWindow().setAttributes(layout);

        mCameraActivityListener = new CameraActivityListener(this, new CameraActivityHolder(this));

        if(null != getListener()) {
            getListener().onActivityCreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(null != getListener()) {
            getListener().onActivityResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(null != getListener()) {
            getListener().onActivityPause();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if(null != getListener()) {
            getListener().onActivityStop();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(null != getListener()) {
            getListener().onActivityDestroy();
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        LogUtils.d(TAG, "onRequestPermissionsResult");

        if(null != getListener()) {
            getListener().onActivityRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return (null != getListener()) && getListener().onActivityKeyDown(keyCode, event);
    }
}
