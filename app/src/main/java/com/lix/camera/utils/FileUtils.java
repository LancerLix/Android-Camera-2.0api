
package com.lix.camera.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Xml;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    public static File getOutputMediaFile(int mediaFormat, int envIso, int expIso) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = getImageFolder();
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String index = "";
        File mediaFile = null;
        for (int count = 1; count <= 100; count++) {
            if (mediaFormat == MediaUtils.MEDIA_FORMAT_IMAGE) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "IMG_" + envIso + "_" + expIso + "_" + timeStamp + index + ".jpg");
            }else if (mediaFormat == MediaUtils.MEDIA_FORMAT_VIDEO) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_" + timeStamp + index + ".mp4");
            }else {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_" + timeStamp + index + ".tmp");
            }
            if (!mediaFile.exists()) {
                break;
            }
            index = "_" + count; // try to find a unique filename
        }

        LogUtils.d(TAG, "getOutputMediaFile returns: " + mediaFile);
        return mediaFile;
    }

    private static File getImageFolder() {
        String folderName = getSaveLocation();
        return getImageFolder(folderName);
    }

    public static String getSaveLocation() {
        return PreferenceUtils.getStringValue(PreferenceUtils.PREFERENCES_UTILS_ADV,
                PreferenceUtils.getSaveLocationPreferenceKey(), PreferenceUtils.DEFAULT_IMAGE_LOCATION);
    }

    private static File getImageFolder(String folderName) {
        File file;
        if (folderName.length() > 0 && folderName.lastIndexOf(File.separator) == folderName.length() - 1) {
            // ignore final '/' character
            folderName = folderName.substring(0, folderName.length() - 1);
        }
        // if( folder_name.contains("/") ) {
        if (folderName.startsWith(File.separator)) {
            file = new File(folderName);
        }
        else {
            file = new File(getBaseFolder(), folderName);
        }
        return file;
    }
    
    private static File getBaseFolder() {
        return Environment.getExternalStorageDirectory(); 
    }

    public static boolean isCustomPathFile(String fileName){
        String customPath = getImageFolder().getAbsolutePath();
        if(fileName != null){
            String path = fileName.substring(0,fileName.lastIndexOf("/"));
            LogUtils.d(TAG, "fileName====>" + fileName);
            LogUtils.d(TAG, "CustomPath====>" + customPath);

            return path.length() == customPath.length() && fileName.contains(customPath);
        }
        return false;
    }

    /** 
     * Create a file on the device
     *
     * @param fileName the created file name
     * @return true if create is success, false when the file is already exist or create failed
     */  
    public static boolean createFile(File fileName){
        boolean flag = false;

        try {
            if(fileName != null ){
                if(fileName.exists()) {
                    flag = fileName.delete();
                }

                flag = fileName.createNewFile();
            }
        } catch (Exception e) {
            LogUtils.d(TAG, "createFile fail --- " + fileName);
            LogUtils.d(TAG, "createFile fail --- " + e);
            e.printStackTrace();
        }

        return flag;
    }
    
    /** 
     * Delete a file on the device
     *
     * @param fileName the deleted file name
     * @return true if delete is success, false when the file is not exist or delete failed
     */  
    public static boolean deleteFile(File fileName){
        boolean flag = false;
        try {
            if(fileName != null ){
                if(fileName.exists()) {
                    flag = fileName.delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }
   
    public static boolean writeTxtFile(String content, File fileName){
        boolean flag = false;
        FileOutputStream fOut = null;
        try {
            fOut = new FileOutputStream(fileName);
            fOut.write(content.getBytes("GBK"));
            fOut.close();
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fOut != null) {
                try {
                    fOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return flag;
    }
    
    public static String zipFolder(Context context,String srcFiles) throws IOException {
        if(context == null){
            return null;
        }        
        String zipPath =  context.getFilesDir().toString();
        String zipName = "tmp.zip";
        srcFiles = ImageUtils.compressImage(context,srcFiles);
        if (srcFiles == null || zipPath.equals("")) {
            LogUtils.e(TAG, "src or zip file cannot be null !");
            return null;
        }

        LogUtils.d(TAG, "Source File:" + srcFiles);
        LogUtils.d(TAG, "Zip File:" + zipPath);

        File zipLog = new File(zipPath,zipName);
        if (zipLog.exists()) {
            zipLog.delete();
        }
        
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipLog));
            byte[] buffer = new byte[1024];

            File file = new File(srcFiles);

            String fileName = null;
            fileName = file.getName();

            zos.putNextEntry(new ZipEntry(fileName));

            FileInputStream is = new FileInputStream(srcFiles);
            int len = 0;
            while ((len = is.read(buffer)) != -1)
                zos.write(buffer, 0, len);
            is.close();
            return zipLog.getPath();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zos != null){
                zos.close();
            }
        }
        
        return null;
    }
    
 // 用来存储设备信息和异常信息
    private static Map<String, String> infos = new HashMap<String, String>();

    // 用于格式化日期,作为日志文件名的一部分
    private static DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    
    public static final String LOG_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)+"/crash/";

    private static final String mLogcatCmd = "/system/bin/logcat -v time -df ";
    /**
     * 将设备信息保存到本地文件
     * 
     * @param param
     * @param ctx
     * @return file path
     * @throws IOException
     */
    public static String getDeviceInfo(HashMap<String, String> param,
            Context ctx) throws IOException {
        if (param == null || param.isEmpty()) {
            LogUtils.d(TAG, "param can't be null !");
            return null;
        }

        File log_dir = new File(LOG_DIR);
        if (!log_dir.exists()) {
            log_dir.mkdirs();
        }

        boolean flag = true;
        StringBuffer sb = new StringBuffer();
        long timestamp = System.currentTimeMillis();
        String path = LOG_DIR + "Deviceinfo-" + timestamp + ".txt";
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        long rtc = SystemClock.elapsedRealtime() / 1000;
        String time = formatter.format(new Date());

        String fsStat = MyExec("df /data /system /cache /mnt/sdcard/ /mnt/external_sd/");

        sb.append("Timestamp=" + timestamp + "\n");
        sb.append("Time=" + time + "\n");
        sb.append("PowerOnTime=" + rtc + "s\n\n");

        for (Map.Entry<String, String> entry : param.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        if (fsStat != null && !fsStat.isEmpty()) {
            sb.append("\nFileSystem State:\n" + fsStat);
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(sb.toString().getBytes());
        } catch (IOException e) {
            LogUtils.d(TAG, e.getMessage());
            flag = false;
        } finally {
            if (fos != null)
                fos.close();
        }
        return flag ? path : null;
    }
    
    /**
     * @since API 2.1
     * @return /data/anr/trace.txt path to the caller if failed, than return null;
     */
    public static String getTracesFile() {
        String traceFile = "/data/anr/traces.txt";
        File logFile = new File(traceFile);
        if (logFile.exists() && logFile.canRead()) {
            LogUtils.d(TAG, "Get traces file successful !");
            return traceFile;
        } else {
            LogUtils.d(TAG, "Can't open " + traceFile);
            return null;
        }
    }
    
    public static String captureLogInfoWriteXML(Context context,String zipLogFile,String title,String type){
        String xmlFilePath = null;
        try
        {          
            xmlFilePath = context.getFilesDir() + "/captureLog_"+System.currentTimeMillis()+".xml";     
            zipLogFile = zipLogFile==null?"":zipLogFile;
            title = title==null?"":title;
            type = type==null?"":type;
            File file = new File(xmlFilePath); 
            if(!file.exists()){
                if (file.createNewFile()) {                
                } else {
                    return null;
                }
            }
            LogUtils.d(TAG,file.toString());
            FileOutputStream  osw = new FileOutputStream (file);
            osw.write(writeXml(xmlFilePath,zipLogFile,title,type).getBytes());
            osw.close();
            osw.close();
        } catch (Exception e){
            e.printStackTrace();
            LogUtils.d(TAG,"Exception : " + e.toString());
            return null;
        }
        return xmlFilePath;
    }
    
    public static ArrayList<HashMap<String, String>> captureLogInfoReadXML(String filepath){
        LogUtils.d(TAG,"filepath = " + filepath);
        if(filepath == null){
            return null;
        }
        File file = new File(filepath); 
        if(!file.exists()){
            LogUtils.d(TAG,filepath + " is not exit!");
            return null;
        }
        ArrayList<HashMap<String, String>> infoList = new ArrayList<HashMap<String, String>>();
        infoList.clear();
        try {
            FileInputStream is = new FileInputStream(file);
            XmlPullParser parser = Xml.newPullParser(); //由android.util.Xml创建一个XmlPullParser实例  
            parser.setInput(is, "UTF-8");               //设置输入流 并指明编码方式  
      
            int eventType = parser.getEventType();  
            while (eventType != XmlPullParser.END_DOCUMENT) {  
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.START_TAG:
                        if(parser.getName().equals("XMLFilePath")){
                            eventType = parser.next();
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("XMLFilePath", parser.getText());
                            infoList.add(map);
                        }else if(parser.getName().equals("ZIPFilePath")){
                            eventType = parser.next();
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("ZIPFilePath", parser.getText());
                            infoList.add(map);
                        }else if(parser.getName().equals("TYPE")){
                            eventType = parser.next();
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("TYPE", parser.getText());
                            infoList.add(map);
                        }else if(parser.getName().equals("TITLE")){
                            eventType = parser.next();
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("TITLE", parser.getText());
                            infoList.add(map);
                        }                            
                        break;
                    case XmlPullParser.END_TAG:                
                        break;
                }  
                eventType = parser.next();  
            } 
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return infoList;
    }
    
    private static String writeXml(String xmlFilePath,String zipLogFile,String title,String type) {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();
        try {
            serializer.setOutput(writer);
            // <?xml version="1.0″ encoding="UTF-8″ standalone="yes"?>
            serializer.startDocument("UTF-8", true);                  
            {
                serializer.startTag(null, "DATA");               
                {
                    {
                        serializer.startTag(null, "XMLFilePath");            
                        serializer.text(xmlFilePath);
                        serializer.endTag(null, "XMLFilePath");
                        
                        serializer.startTag(null, "ZIPFilePath");            
                        serializer.text(zipLogFile);
                        serializer.endTag(null, "ZIPFilePath");
                        
                        serializer.startTag(null, "TITLE");            
                        serializer.text(title);
                        serializer.endTag(null, "TITLE");  
                        
                        serializer.startTag(null, "TYPE");            
                        serializer.text(type);
                        serializer.endTag(null, "TYPE");      
                    } 
                }
                serializer.endTag(null, "DATA");
            }
            serializer.endDocument();
            return writer.toString();
        } catch (Exception e)
        {
            return null;
        }
    }  
    
    public static ArrayList<String> getAllCaptureLogXML(Context context){
        String Path = context.getFilesDir().toString();
        String Extension = ".xml";
        //try{}
        File[] files =new File(Path).listFiles();
        ArrayList<String> filepathList = new ArrayList<String>();
        for (int i =0; i < files.length; i++)
        {
            File f = files[i];
            if (f.isFile())
            {
                if (f.getPath().contains("captureLog_") && f.getPath().endsWith(Extension)){
                    filepathList.add(f.getPath());
                    LogUtils.d(TAG,"f.getPath() = " + f.getPath());
                }                    
            }            
        }
        return filepathList;
    }
    
    /**
     * @return System log file path to the caller if failed, than return null;
     */
    public static String getSystemLog() {

        File log_dir = new File(LOG_DIR);
        if (!log_dir.exists()) {
            log_dir.mkdirs();
        }
        long timestamp = System.currentTimeMillis();
        String logPath = LOG_DIR + "system_log-" + timestamp + ".txt";
        String cmd = mLogcatCmd + logPath;
        if (redirectLog(cmd, logPath)) {
            LogUtils.d(TAG, "Pack system log sucessful !");
            return logPath;
        } else {
            LogUtils.d(TAG, "Pack system log failed !");
            return null;
        }
    }
    
    private static boolean redirectLog(String cmd, String logPath) {
        if (cmd == null || cmd.length() == 0) {
            LogUtils.e(TAG, "Cmd cannot be null !");
            return false;
        }
        LogUtils.d(TAG, "packingCmd = " + cmd);

        try {
            File logFile = new File(logPath);
            if (logFile.exists()) {
                logFile.delete();
            }
            Process p = Runtime.getRuntime().exec(cmd);
            p.waitFor();

            File log = new File(logPath);
            if (log.exists()) {
                LogUtils.d(TAG, "logfile lenght:" + log.length() + " Byte(s)");
            }

            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            LogUtils.e(TAG, e.getLocalizedMessage());
            return false;
        }
    }

    /**
     * 保存错误信息到文件中
     * 
     * @param ex
     * @return 返回文件名称,便于将文件传送到服务器
     */
    public static String getCrashInfo(Throwable ex) {

        if(ex == null){
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            String logPath = LOG_DIR + fileName;
            if (Environment.getExternalStorageState().equals(
                    Environment.MEDIA_MOUNTED)) {
                File dir = new File(LOG_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(logPath);
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return logPath;
        } catch (Exception e) {
            LogUtils.e(TAG, "an error occured while writing file...");
        }
        return null;
    }

    /**
     * 压缩各log文件，用于上传服务器
     * 
     * @param srcFiles
     * @param zipFile
     * @return
     * @throws IOException
     */
    public static boolean compressLog(String[] srcFiles, String zipFile)
            throws IOException {
        if (srcFiles == null || srcFiles.length == 0 || zipFile == null || zipFile.length() == 0) {
            LogUtils.e(TAG, "src or zip file cannot be null !");
            return false;
        }

        for (int i = 0; i < srcFiles.length; i++) {
            LogUtils.d(TAG, "Source File:" + srcFiles[i]);
        }
        LogUtils.d(TAG, "Zip File:" + zipFile);

        File zipLog = new File(zipFile);
        if (zipLog.exists()) {
            zipLog.delete();
        }

        boolean flag = true;
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            byte[] buffer = new byte[1024];

            for (int i = 0; i < srcFiles.length; i++) {
                File file = new File(srcFiles[i]);
                if(file == null || !file.exists()){
                    continue;
                }
                String fileName = null;
                if (file.getAbsolutePath().contains(LOG_DIR)
                        && file.getAbsolutePath().contains("-")) {
                    int index = file.getName().lastIndexOf('-');
                    fileName = file.getName().substring(0, index) + ".txt";
                } else if (file.getAbsolutePath().contains("recovery/last_log")) {
                    fileName = "Recovery.log";
                } else {
                    fileName = file.getName();
                }

                zos.putNextEntry(new ZipEntry(fileName));

                FileInputStream is = new FileInputStream(srcFiles[i]);
                int len = 0;
                while ((len = is.read(buffer)) != -1)
                    zos.write(buffer, 0, len);
                is.close();
            }
        } catch (IOException e) {
            LogUtils.d(TAG, e.getLocalizedMessage());
            flag = false;
        } finally {
            if (zos != null)
                zos.close();
            // 将已压缩的文件删除
            for (int i = 0; i < srcFiles.length; i++) {
                if (srcFiles[i].contains(LOG_DIR)) {
                    File logFile = new File(srcFiles[i]);
                    if (logFile.exists()) {
                        logFile.delete();
                    }
                }
            }
        }

        return flag;
    }
    /**
     * 执行linux命名
     * 
     * @param cmd
     * @return
     */
    public static String MyExec(String cmd) {
        if (cmd == null || cmd.isEmpty()) {
            LogUtils.d(TAG, "cmd is null !");
            return null;
        }
        LogUtils.d(TAG, cmd);

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));
            int read;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            LogUtils.d(TAG, output.toString());

            return output.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }  
    }
    
}
