package com.lix.camera.utils;

public final class Constants {
    
    public static final class CameraDeviceMsg {
        public static final int CAMERA_DEVICE_OPENED = 0;
        
        public static final int CAMERA_DEVICE_DISCONNECTED = 1;
        
        public static final int CAMERA_DEVICE_ERROR = 2;
    }
    
    public static final class PermissionRequestCode {
        public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0;
        
        public static final int PERMISSIONS_REQUEST_CAMERA = 1;
        
        public static final int PERMISSIONS_REQUEST_CAMERA_AND_STORAGE = 2;
    }
}
