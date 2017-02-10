package com.lix.camera.utils;

public class PhoneUtils {

    public final static String PhoneModel = android.os.Build.MODEL;
    
    public final static String PhoneManufacturer = android.os.Build.MANUFACTURER;
    
    private final static String VENDOR_SAMSUNG = "samsung";
    public static boolean isSamsungPhone() {
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_SAMSUNG);
    }
    
    private final static String VENDOR_VIVO= "vivo";
    public static boolean isVivoPhone() {
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_VIVO);
    }
    
    private final static String VENDOR_OPPO= "oppo";
    public static boolean isOppoPhone() {
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_OPPO);
    }
    
    private final static String VENDOR_ASUS= "asus";
    public static boolean isAsusPhone() {
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_ASUS);
    }

    private final static String VENDOR_NUBIA = "nubia";
    public static boolean isNubiaPhone() {
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_NUBIA);
    }

    private final static String VENDOR_HUAWEI = "HUAWEI";
    public static boolean isHuaweiPhone() {
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_HUAWEI);
    }
    
    private final static String VENDOR_LENOVO = "LENOVO";
    public static boolean isLenovoPhone() {
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_LENOVO);
    }
    
    private final static String VENDOR_SONY = "sony";
    public static boolean isSonyPhone(){
        return PhoneManufacturer.equalsIgnoreCase(VENDOR_SONY);
    }

    private final static String VENDOR_MX2 = "M045";
    public static boolean isMx2(){
        return PhoneModel.equalsIgnoreCase(VENDOR_MX2);
    }

    public static boolean needCorrectExif() {
        return isMx2();
    }

    public static boolean useSpecialCamera2Setting() {
        return isHuaweiPhone();
    }
}
