
package com.lix.camera.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class CommonUtils {
    
    private static String TAG = CommonUtils.class.getSimpleName();
    /**
     * 格式化时间日期
     * 
     * @param time
     * @return
     */
    @SuppressLint("SimpleDateFormat")
    public static String formatDateTime(long time) {
        Date currentdate = new Date(time);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(currentdate);
    }
    
    /**
     * 判断网络是否连接
     * 
     * @return
     */
    public static boolean isNetworkConnected(
            ConnectivityManager connectivityManager) {
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        if (connectivityManager != null) {

            // 获取网络连接管理的对象
            NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            if (info != null) {
                // 判断当前网络是否已经连接
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }

        return false;
    }

    
    static public void cleanMemory(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);  
        List<RunningAppProcessInfo> infoList = am.getRunningAppProcesses();  
    
        if (infoList != null) {  
            for (int i = 0; i < infoList.size(); ++i) {  
                RunningAppProcessInfo appProcessInfo = infoList.get(i);  
//                Log.d(TAG, "process name : " + appProcessInfo.processName);  
//                //importance 该进程的重要程度  分为几个级别，数值越低就越重要。  
//                Log.d(TAG, "importance : " + appProcessInfo.importance);  

                // 一般数值大于RunningAppProcessInfo.IMPORTANCE_SERVICE的进程都长时间没用或者空进程了  
                // 一般数值大于RunningAppProcessInfo.IMPORTANCE_VISIBLE的进程都是非可见进程，也就是在后台运行着  
               // if (appProcessInfo.importance > RunningAppProcessInfo.IMPORTANCE_VISIBLE) {  
                if (appProcessInfo.processName.equals(context.getPackageName()) &&
                        appProcessInfo.pid != android.os.Process.myPid()) {  
                    
                    LogUtils.d(TAG, "process name : " + appProcessInfo.processName);
                    //importance 该进程的重要程度  分为几个级别，数值越低就越重要。  
                    LogUtils.d(TAG, "appProcessInfo.pid : " + appProcessInfo.pid);
                    LogUtils.d(TAG, "android.os.Process.myPid() : " + android.os.Process.myPid());
//                   String[] pkgList = appProcessInfo.pkgList;  
//                    for (int j = 0; j < pkgList.length; ++j) {//pkgList 得到该进程下运行的包名  
//                        Log.d(TAG, "It will be killed, package name : " + pkgList[j]);  
//                        am.killBackgroundProcesses(pkgList[j]);  
//                    } 
                    android.os.Process.killProcess(appProcessInfo.pid);
                }  

            }  
        }  
    }
    
    public final static String LANGUAGE_CN = "zh-CN";
    public final static String LANGUAGE_TW = "zh-TW";
    public final static String LANGUAGE_EN = "en";
    public static String getLanguageEnv() {
        Locale l = Locale.getDefault();
        String language = l.getLanguage();
        String country = l.getCountry().toLowerCase();
        if ("zh".equals(language)) {
            if ("tw".equals(country)) {
                language = "zh-TW";
            }else {
                language = "zh-CN";
            }
        } else {
            language = "en";
        }
        return language;
    }

    @SuppressLint("SimpleDateFormat")
    public static String longParseDate(String submitTime){
        long pushTime;
        try{
           pushTime = Long.parseLong(submitTime); 
        }catch(Exception e){
            e.printStackTrace();
            return "2015-01-01 13:59:23";
        }        
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date= new Date(pushTime*1000);
        return df.format(date);
    }

    public static byte intChangeByte(int data) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data & 0xff00) >> 8);
        bytes[2] = (byte) ((data & 0xff0000) >> 16);
        bytes[3] = (byte) ((data & 0xff000000) >> 24);
        return bytes[0];
    }
}
