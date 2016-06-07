
package com.lix.camera.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.os.Environment;

public class ImageTools {

    // /////图片处理 begin
    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        return getRoundedCornerBitmap(bitmap);
    }

    public static Bitmap getCircleBitmap(String bitmapPath) {
        Bitmap mSrc;
        mSrc = BitmapFactory.decodeFile(bitmapPath);
        Bitmap ret = getRoundedCornerBitmap(mSrc);
        mSrc.recycle();
        return ret;
    }
    
    private final static float BASE_BITMAP_BORDER_WIDTH = 10;
    private final static float BASE_BITMAP_RADIUS = 153;

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) // 图片变成圆形
    {
        if (bitmap == null) {
            return null;
        }
        if (bitmap.isRecycled()) {
            return null;
        }
        
        Bitmap roundBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(roundBitmap);
        int color = 0xff424242;
        Paint paint = new Paint();
        // 设置圆形半径
        int radius;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            radius = bitmap.getHeight() / 2;
        } else {
            radius = bitmap.getWidth() / 2;
        }
        
        float borderWidth = BASE_BITMAP_BORDER_WIDTH * radius / BASE_BITMAP_RADIUS;
        // 绘制圆形
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, radius - borderWidth, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        
        //draw outline border
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_OVER));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, radius, paint);
        return roundBitmap;
    }
    
    public static Bitmap getSquareRoundedCornerBitmap(Bitmap bitmap)
    {
        if (bitmap == null) {
            return null;
        }
        if (bitmap.isRecycled()) {
            return null;
        }
        
        int sideLength;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            sideLength = bitmap.getHeight() ;
        } else {
            sideLength = bitmap.getWidth() ;
        }
        
        Bitmap roundBitmap = Bitmap.createBitmap(sideLength, sideLength, Config.ARGB_8888);
        Canvas canvas = new Canvas(roundBitmap);
        int color = 0xff424242;
        Paint paint = new Paint();

        // 绘制正方形
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        RectF rectf = new RectF(0,0,sideLength, sideLength);
        int rcd = sideLength/18;
        canvas.drawRoundRect(rectf,rcd , rcd, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return roundBitmap;
    
    }
    
    // /////图片处理 end

    public static File getBaseFolder() {
        return Environment.getExternalStorageDirectory();//Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    }

    public static File getImageFolder(String folder_name) {
        File file = null;
        if (folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length() - 1) {
            // ignore final '/' character
            folder_name = folder_name.substring(0, folder_name.length() - 1);
        }
        // if( folder_name.contains("/") ) {
        if (folder_name.startsWith("/")) {
            file = new File(folder_name);
        }
        else {
            file = new File(getBaseFolder(), folder_name);
        }
        return file;
    }

    public static File getImageFolder() {
        String folder_name = getSaveLocation();
        return getImageFolder(folder_name);
    }

    public static String getSaveLocation() {
        return Environment.DIRECTORY_DCIM+"/TextCamera";
    }
    
    @SuppressLint("SimpleDateFormat")
    public static File getOutputMediaFile() {
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
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + index + ".jpg");
            if (!mediaFile.exists()) {
                break;
            }
            index = "_" + count; // try to find a unique filename
        }
        return mediaFile;
    }

    public static String getPictureSize(int width, int height) {
        int min = Math.min(width, height);
        int max = Math.max(width, height);
        int gcf = greatestCommonFactor(width, height);
        width /= gcf;
        height /= gcf;
        return max + RESOLUTION_X + min + "(" + width + ":" + height + ")";
    }
    public static final String RESOLUTION_X = " x ";
    
    private static int greatestCommonFactor(int a, int b) {
        while( b > 0 ) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }
}
