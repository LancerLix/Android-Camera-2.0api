package com.lix.camera.utils;

import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;

import com.lix.camera.base.BaseCamera;

/**
 * Created by lix on 2016/12/26.
 *
 */
public class CameraParametersConverter {

//=================================================================================================================

    public static int commCameraIdToCamera1(BaseCamera.CameraId commCameraId) {
        int cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

        switch(commCameraId) {
            case CAMERA_FACING_BACK:
                cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                break;
            case CAMERA_FACING_FRONT:
                cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                break;
        }

        return cameraId;
    }

    public static String commFlashValueToCamera1(String commFlashValue) {
        String flashValue = null;

        switch (commFlashValue) {
            case BaseCamera.FlashValue.FLASH_OFF:
                flashValue = Camera.Parameters.FLASH_MODE_OFF;
                break;
            case BaseCamera.FlashValue.FLASH_AUTO:
                flashValue = Camera.Parameters.FLASH_MODE_AUTO;
                break;
            case BaseCamera.FlashValue.FLASH_ON:
                flashValue = Camera.Parameters.FLASH_MODE_ON;
                break;
            case BaseCamera.FlashValue.FLASH_TORCH:
                flashValue = Camera.Parameters.FLASH_MODE_TORCH;
                break;
        }

        return flashValue;
    }

    public static String camera1FlashValueToComm(String flashValue) {
        String commFlashValue = null;

        switch (flashValue) {
            case Camera.Parameters.FLASH_MODE_OFF:
                commFlashValue = BaseCamera.FlashValue.FLASH_OFF;
                break;
            case Camera.Parameters.FLASH_MODE_AUTO:
                commFlashValue = BaseCamera.FlashValue.FLASH_AUTO;
                break;
            case Camera.Parameters.FLASH_MODE_ON:
                commFlashValue = BaseCamera.FlashValue.FLASH_ON;
                break;
            case Camera.Parameters.FLASH_MODE_TORCH:
                commFlashValue = BaseCamera.FlashValue.FLASH_TORCH;
                break;
        }

        return commFlashValue;
    }

    public static String commWBValueToCamera1(String commWBValue) {
        String wbValue = null;

        switch (commWBValue) {
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_AUTO:
                wbValue = Camera.Parameters.WHITE_BALANCE_AUTO;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_CLOUDY_DAYLIGHT:
                wbValue = Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_DAYLIGHT:
                wbValue = Camera.Parameters.WHITE_BALANCE_DAYLIGHT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_FLUORESCENT:
                wbValue = Camera.Parameters.WHITE_BALANCE_FLUORESCENT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_INCANDESCENT:
                wbValue = Camera.Parameters.WHITE_BALANCE_INCANDESCENT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_SHADE:
                wbValue = Camera.Parameters.WHITE_BALANCE_SHADE;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_TWILIGHT:
                wbValue = Camera.Parameters.WHITE_BALANCE_TWILIGHT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_WARM_FLUORESCENT:
                wbValue = Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT;
                break;
        }

        return wbValue;
    }

    public static String camera1WBValueToComm(String wbValue) {
        String commWBValue = null;

        switch (wbValue) {
            case Camera.Parameters.WHITE_BALANCE_AUTO:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_AUTO;
                break;
            case Camera.Parameters.WHITE_BALANCE_CLOUDY_DAYLIGHT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_CLOUDY_DAYLIGHT;
                break;
            case Camera.Parameters.WHITE_BALANCE_DAYLIGHT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_DAYLIGHT;
                break;
            case Camera.Parameters.WHITE_BALANCE_FLUORESCENT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_FLUORESCENT;
                break;
            case Camera.Parameters.WHITE_BALANCE_INCANDESCENT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_INCANDESCENT;
                break;
            case Camera.Parameters.WHITE_BALANCE_SHADE:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_SHADE;
                break;
            case Camera.Parameters.WHITE_BALANCE_TWILIGHT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_TWILIGHT;
                break;
            case Camera.Parameters.WHITE_BALANCE_WARM_FLUORESCENT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_WARM_FLUORESCENT;
                break;
        }

        return commWBValue;
    }

    public static String commFocusValueToCamera1(String commFocusValue) {
        String focusValue = null;

        switch (commFocusValue) {
            case BaseCamera.FocusValue.FOCUS_AUTO:
                focusValue = Camera.Parameters.FOCUS_MODE_AUTO;
                break;
            case BaseCamera.FocusValue.FOCUS_INFINITY:
                focusValue = Camera.Parameters.FOCUS_MODE_INFINITY;
                break;
            case BaseCamera.FocusValue.FOCUS_MACRO:
                focusValue = Camera.Parameters.FOCUS_MODE_MACRO;
                break;
            case BaseCamera.FocusValue.FOCUS_FIXED:
                focusValue = Camera.Parameters.FOCUS_MODE_FIXED;
                break;
            case BaseCamera.FocusValue.FOCUS_EDOF:
                focusValue = Camera.Parameters.FOCUS_MODE_EDOF;
                break;
            case BaseCamera.FocusValue.FOCUS_CONTINUOUS_VIDEO:
                focusValue = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO;
                break;
            case BaseCamera.FocusValue.FOCUS_CONTINUOUS_PICTURE:
                focusValue = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE;
                break;
        }

        return focusValue;
    }

    public static String camera1FocusValueToComm(String focusValue) {
        String commFocusValue = null;

        switch (focusValue) {
            case Camera.Parameters.FOCUS_MODE_AUTO:
                commFocusValue = BaseCamera.FocusValue.FOCUS_AUTO;
                break;
            case Camera.Parameters.FOCUS_MODE_INFINITY:
                commFocusValue = BaseCamera.FocusValue.FOCUS_INFINITY;
                break;
            case Camera.Parameters.FOCUS_MODE_MACRO:
                commFocusValue = BaseCamera.FocusValue.FOCUS_MACRO;
                break;
            case Camera.Parameters.FOCUS_MODE_FIXED:
                commFocusValue = BaseCamera.FocusValue.FOCUS_FIXED;
                break;
            case Camera.Parameters.FOCUS_MODE_EDOF:
                commFocusValue = BaseCamera.FocusValue.FOCUS_EDOF;
                break;
            case Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO:
                commFocusValue = BaseCamera.FocusValue.FOCUS_CONTINUOUS_VIDEO;
                break;
            case Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE:
                commFocusValue = BaseCamera.FocusValue.FOCUS_CONTINUOUS_PICTURE;
                break;
        }

        return commFocusValue;
    }

//=================================================================================================================

    public static String commCameraIdToCamera2(CameraManager cameraManager, BaseCamera.CameraId commCameraId) {

        int cameraIntId = CameraMetadata.LENS_FACING_BACK;

        switch(commCameraId) {
            case CAMERA_FACING_BACK:
                cameraIntId = CameraMetadata.LENS_FACING_BACK;
                break;
            case CAMERA_FACING_FRONT:
                cameraIntId = CameraMetadata.LENS_FACING_FRONT;
                break;
        }

        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String cameraStringId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager
                        .getCameraCharacteristics(cameraStringId);

                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (null != lensFacing && lensFacing == cameraIntId) {
                    return cameraStringId;
                }
            }
        } catch (CameraAccessException ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static int commWBValueToCamera2(String commWBValue) {
        int wbValue = -1;

        switch (commWBValue) {
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_AUTO:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_AUTO;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_CLOUDY_DAYLIGHT:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_DAYLIGHT:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_FLUORESCENT:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_INCANDESCENT:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_SHADE:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_SHADE;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_TWILIGHT:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_TWILIGHT;
                break;
            case BaseCamera.WhiteBalanceValue.WHITE_BALANCE_WARM_FLUORESCENT:
                wbValue = CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT;
                break;
        }

        return wbValue;
    }

    public static String camera2WBValueToComm(int wbValue) {
        String commWBValue = null;

        switch (wbValue) {
            case CameraMetadata.CONTROL_AWB_MODE_AUTO:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_AUTO;
                break;
            case CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_CLOUDY_DAYLIGHT;
                break;
            case CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_DAYLIGHT;
                break;
            case CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_FLUORESCENT;
                break;
            case CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_INCANDESCENT;
                break;
            case CameraMetadata.CONTROL_AWB_MODE_SHADE:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_SHADE;
                break;
            case CameraMetadata.CONTROL_AWB_MODE_TWILIGHT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_TWILIGHT;
                break;
            case CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT:
                commWBValue = BaseCamera.WhiteBalanceValue.WHITE_BALANCE_WARM_FLUORESCENT;
                break;
        }

        return commWBValue;
    }

    public static int commFocusValueToCamera2(String commFocusValue) {
        int focusValue = -1;

        switch (commFocusValue) {
            case BaseCamera.FocusValue.FOCUS_AUTO:
                focusValue = CameraMetadata.CONTROL_AF_MODE_AUTO;
                break;
            case BaseCamera.FocusValue.FOCUS_INFINITY:
                focusValue = CameraMetadata.CONTROL_AF_MODE_OFF;
                break;
            case BaseCamera.FocusValue.FOCUS_MACRO:
                focusValue = CameraMetadata.CONTROL_AF_MODE_MACRO;
                break;
            case BaseCamera.FocusValue.FOCUS_EDOF:
                focusValue = CameraMetadata.CONTROL_AF_MODE_EDOF;
                break;
            case BaseCamera.FocusValue.FOCUS_CONTINUOUS_VIDEO:
                focusValue = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO;
                break;
            case BaseCamera.FocusValue.FOCUS_CONTINUOUS_PICTURE:
                focusValue = CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE;
                break;
        }

        return focusValue;
    }

    public static String camera2FocusValueToComm(int focusValue) {
        String commFocusValue = null;

        switch (focusValue) {
            case CameraMetadata.CONTROL_AF_MODE_AUTO:
                commFocusValue = BaseCamera.FocusValue.FOCUS_AUTO;
                break;
            case CameraMetadata.CONTROL_AF_MODE_OFF:
                commFocusValue = BaseCamera.FocusValue.FOCUS_INFINITY;
                break;
            case CameraMetadata.CONTROL_AF_MODE_MACRO:
                commFocusValue = BaseCamera.FocusValue.FOCUS_MACRO;
                break;
            case CameraMetadata.CONTROL_AF_MODE_EDOF:
                commFocusValue = BaseCamera.FocusValue.FOCUS_EDOF;
                break;
            case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO:
                commFocusValue = BaseCamera.FocusValue.FOCUS_CONTINUOUS_VIDEO;
                break;
            case CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE:
                commFocusValue = BaseCamera.FocusValue.FOCUS_CONTINUOUS_PICTURE;
                break;
        }

        return commFocusValue;
    }
}
