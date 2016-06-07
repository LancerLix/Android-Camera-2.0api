package com.lix.camera.activity;

import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.location.Location;
import android.util.Log;

public class CameraSettings {
    private final String TAG = CameraSettings.class.getSimpleName();
    
    // keys that we need to store, to pass to the stillBuilder, but doesn't need to be passed to previewBuilder (should set sensible defaults)
    private int rotation = 0;
    private Location location = null;
    private byte jpeg_quality = 90;

    // keys that we have passed to the previewBuilder, that we need to store to also pass to the stillBuilder (should set sensible defaults, or use a has_ boolean if we don't want to set a default)
    private int scene_mode = CameraMetadata.CONTROL_SCENE_MODE_DISABLED;
    private int color_effect = CameraMetadata.CONTROL_EFFECT_MODE_OFF;
    private int white_balance = CameraMetadata.CONTROL_AWB_MODE_AUTO;
    private boolean has_iso = false;
    private int iso = 0;
    private Rect scalar_crop_region = null; // no need for has_scalar_crop_region, as we can set to null instead
    private boolean has_ae_exposure_compensation = false;
    private int ae_exposure_compensation = 0;
    private boolean has_af_mode = false;
    private int af_mode = CaptureRequest.CONTROL_AF_MODE_AUTO;
    private boolean ae_lock = false;
    private MeteringRectangle [] af_regions = null; // no need for has_scalar_crop_region, as we can set to null instead
    private MeteringRectangle [] ae_regions = null; // no need for has_scalar_crop_region, as we can set to null instead
    private boolean has_face_detect_mode = false;
    private int face_detect_mode = CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF;

    public void setupBuilder(CaptureRequest.Builder builder, CameraCharacteristics characteristics, boolean is_still) {
        setSceneMode(builder);
        setColorEffect(builder);
        setWhiteBalance(builder);
        setISO(builder);
        setCropRegion(builder);
        setExposureCompensation(builder);
        setFocusMode(builder);
        setAutoExposureLock(builder);
        setAFRegions(builder, characteristics);
        setAERegions(builder, characteristics);
        setFaceDetectMode(builder);

        builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
        //builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

        if( is_still ) {
            if( location != null ) {
                //builder.set(CaptureRequest.JPEG_GPS_LOCATION, location);
                // settings location messes up date on Nexus 7?!
            }
            builder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
            builder.set(CaptureRequest.JPEG_QUALITY, jpeg_quality);
        }
    }

    private boolean setSceneMode(CaptureRequest.Builder builder) {
        Log.d(TAG, "setSceneMode");
        Log.d(TAG, "builder: " + builder);
        
        if( builder.get(CaptureRequest.CONTROL_SCENE_MODE) == null && scene_mode == CameraMetadata.CONTROL_SCENE_MODE_DISABLED ) {
            // can leave off
        }
        else if( builder.get(CaptureRequest.CONTROL_SCENE_MODE) == null || builder.get(CaptureRequest.CONTROL_SCENE_MODE) != scene_mode ) {
            Log.d(TAG, "setting scene mode: " + scene_mode);
            
            if( scene_mode == CameraMetadata.CONTROL_SCENE_MODE_DISABLED ) {
                builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            }
            else {
                builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_USE_SCENE_MODE);
            }
            builder.set(CaptureRequest.CONTROL_SCENE_MODE, scene_mode);
            return true;
        }
        return false;
    }

    private boolean setColorEffect(CaptureRequest.Builder builder) {
        if( builder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null && color_effect == CameraMetadata.CONTROL_EFFECT_MODE_OFF ) {
            // can leave off
        }
        else if( builder.get(CaptureRequest.CONTROL_EFFECT_MODE) == null || builder.get(CaptureRequest.CONTROL_EFFECT_MODE) != color_effect ) {
            Log.d(TAG, "setting color effect: " + color_effect);
            builder.set(CaptureRequest.CONTROL_EFFECT_MODE, color_effect);
            return true;
        }
        return false;
    }

    private boolean setWhiteBalance(CaptureRequest.Builder builder) {
        if( builder.get(CaptureRequest.CONTROL_AWB_MODE) == null && white_balance == CameraMetadata.CONTROL_AWB_MODE_AUTO ) {
            // can leave off
        }
        else if( builder.get(CaptureRequest.CONTROL_AWB_MODE) == null || builder.get(CaptureRequest.CONTROL_AWB_MODE) != white_balance ) {
            Log.d(TAG, "setting white balance: " + white_balance);
            builder.set(CaptureRequest.CONTROL_AWB_MODE, white_balance);
            return true;
        }
        return false;
    }

    private boolean setISO(CaptureRequest.Builder builder) {
        Log.d(TAG, "setISO: " + has_iso + " : " + iso);
        if( has_iso ) {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
            builder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
        }
        else {
            builder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
        }
        return true;
    }

    private void setCropRegion(CaptureRequest.Builder builder) {
        if( scalar_crop_region != null ) {
            builder.set(CaptureRequest.SCALER_CROP_REGION, scalar_crop_region);
        }
    }

    private boolean setExposureCompensation(CaptureRequest.Builder builder) {
        if( !has_ae_exposure_compensation )
            return false;
        if( builder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) == null || ae_exposure_compensation != builder.get(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) ) {
            Log.d(TAG, "change exposure to " + ae_exposure_compensation);
            builder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ae_exposure_compensation);
            return true;
        }
        return false;
    }

    private void setFocusMode(CaptureRequest.Builder builder) {
        if( has_af_mode )
            builder.set(CaptureRequest.CONTROL_AF_MODE, af_mode);
    }

    private void setAutoExposureLock(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_AE_LOCK, ae_lock);
    }

    private void setAFRegions(CaptureRequest.Builder builder, CameraCharacteristics characteristics) {
        if( af_regions != null && characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) > 0 ) {
            builder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
        }
    }

    private void setAERegions(CaptureRequest.Builder builder, CameraCharacteristics characteristics) {
        if( ae_regions != null && characteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) > 0 ) {
            builder.set(CaptureRequest.CONTROL_AE_REGIONS, ae_regions);
        }
    }

    private void setFaceDetectMode(CaptureRequest.Builder builder) {
        if( has_face_detect_mode )
            builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, face_detect_mode);
    }
    
    // n.b., if we add more methods, remember to update setupBuilder() above!
}