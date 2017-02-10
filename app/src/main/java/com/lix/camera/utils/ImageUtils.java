package com.lix.camera.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Bitmap.Config;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.provider.MediaStore;

import com.lix.camera.utils.pq.ExposureCorrect;

public class ImageUtils {
    private final static String TAG = ImageUtils.class.getSimpleName();
    
    public static Bitmap zoomImage(Bitmap bgImage, double newWidth,
            double newHeight) {
        float width = bgImage.getWidth();
        float height = bgImage.getHeight();
      
        Matrix matrix = new Matrix();
      
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(bgImage, 0, 0, (int) width,
                (int) height, matrix, true);
    }

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
    
    public static Bitmap getSquareRoundedCornerBitmap(Bitmap bitmap, int picRotation)
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
        
        Matrix roundBitmapMatrix = new Matrix();
        roundBitmapMatrix.setRotate(picRotation, bitmap.getWidth() * 0.5f, bitmap.getHeight() * 0.5f);
        Bitmap rotated_thumbnail = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), roundBitmapMatrix, true);
        
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
        canvas.drawBitmap(rotated_thumbnail, 0, 0, paint);
        return roundBitmap;
    }
    
    // 九宫格   中心剪裁正方形
    public static Bitmap centerSquareScaleBitmap(Bitmap bitmap) {
        if (null == bitmap)
        {
            return null;
        }
        if (bitmap.isRecycled()){
            return null;
        }
        
        // 得到图片的宽，高
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        // 裁切后所取的正方形区域边长
        int wh = w > h ? h : w;

        //基于原图，取正方形左上角x坐标
        int retX = w > h ? (w - h) / 2 : 0;
        int retY = w > h ? 0 : (h - w) / 2;

        //下面这句是关键
        return Bitmap.createBitmap(bitmap, retX, retY, wh, wh, null, false);
    }
    
    @SuppressWarnings("deprecation")
    public static Bitmap getDecodeBitmap(File file) {
        if(null == file){
            return null;
        }
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inPurgeable = true;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
    
    public static void compressBitmapToFile(File file, Bitmap bitmap, int quality) {
        try {
            OutputStream outputStream = new FileOutputStream(file);
            if (outputStream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
                outputStream.flush();
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    //图片压缩
    public static String compressImage(Context context,String srcPath) {
        
        if(srcPath == null || srcPath.isEmpty()){
            return null;
        }
        Bitmap image = BitmapFactory.decodeFile(srcPath); 
        if(image == null){
            return null;
        }
         
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024>100) { //循环判断如果压缩后图片是否大于100kb,大于继续压缩       
            baos.reset();//重置baos即清空baos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
            if(options <= 10){
                break;
            }
        }
        Bitmap bitmap;
        try{
            ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
            bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片    
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        
        return saveBitmap(context,bitmap,srcPath);
    }
    
    public static Bitmap getBitmapThumbnail(MediaUtils.MediaData media, Context ctx){
        Bitmap thumbnail = null;
        if (media != null) {
            if (media.mVideo) {
                thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                        ctx.getContentResolver(), media.mId,
                        MediaStore.Video.Thumbnails.MINI_KIND, null);
            } else {
                thumbnail = MediaStore.Images.Thumbnails.getThumbnail(
                        ctx.getContentResolver(), media.mId,
                        MediaStore.Images.Thumbnails.MINI_KIND, null);
            }
            if (thumbnail != null) {
                if (media.mOrientation != 0) {
                    LogUtils.d(TAG, "mThumbnail size is " + thumbnail.getWidth() + " x " + thumbnail.getHeight());
                    Matrix matrix = new Matrix();
                    matrix.setRotate(media.mOrientation,
                            thumbnail.getWidth() * 0.5f,
                            thumbnail.getHeight() * 0.5f);
                    try {
                        Bitmap rotated_thumbnail = Bitmap.createBitmap(
                                thumbnail, 0, 0, thumbnail.getWidth(),
                                thumbnail.getHeight(), matrix, true);
                        // careful, as rotated_thumbnail is sometimes not a
                        // copy!
                        if (rotated_thumbnail != thumbnail) {
                            thumbnail.recycle();
                            thumbnail = rotated_thumbnail;
                        }
                    } catch (Throwable t) {
                        LogUtils.d(TAG, "failed to rotate mThumbnail");
                    }
                }
            }
        }
        return thumbnail;
    }

    public static String saveBitmap(Context context, Bitmap bmp, String sourcePath) {
        String path = context.getFilesDir().toString();
        String picName = "tmp.jpg";
        File f = new File(path, picName);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.JPEG, 30, out);
            out.flush();
            out.close();
            //copy  exif
            ExifUtils.copyExifData(new File(sourcePath), f, null, null, ExposureCorrect.CorrectLevel.LEVEL_NONE);
            return f.getPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }  
    
    @SuppressWarnings("deprecation")
    public static Bitmap getPreviewImage(File picFile, int picWidth){
        if(null == picFile){
            return null;
        }
        
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = false;
        options.inPurgeable = true;
        if(picWidth <= 2 * PicSizeUtils.sScreenSize.x){
            options.inSampleSize = 1; 
        }else{
            options.inSampleSize = 2;  
        }
        
        return BitmapFactory.decodeFile(picFile.getAbsolutePath(), options);
    }
    
    public static Bitmap dealWithWaterMark(Bitmap srcBitmap, boolean addTimeStamp, Drawable watermarkLogo, int exifRotateDegree) {
        int textOffsetX = 0;
        int textOffsetY = 0;
        int logoOffsetX = 0;
//        int logo_offset_y = 0;
        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();
        int textSize = (int) (Math.min(width, height) / 18 + 0.5f);
        
//        int logoDrawableHeight = 0;
//        if(null != logoDrawable) {
//            logoDrawableHeight = logoDrawable.getIntrinsicHeight();
//        }
        
        Canvas canvas = new Canvas(srcBitmap);
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(textSize); // convert dps to pixels
        String timeStamp = CommonUtils.formatDateTime(System.currentTimeMillis());
        paint.setTextAlign(Align.RIGHT);
        canvas.rotate(360 - exifRotateDegree);
        switch (exifRotateDegree) {
            case 180:
                textOffsetX = 0;
                textOffsetY = -textSize;

                logoOffsetX = -width;
//                logo_offset_y = -logoDrawableHeight;
                break;
            case 90:
                textOffsetX = 0;
                textOffsetY = width - textSize;

                logoOffsetX = -height;
//                logo_offset_y = width - logoDrawableHeight;
                break;
            case 270:
                textOffsetX = height;
                textOffsetY = 0;

                logoOffsetX = 0;
//                logo_offset_y = -logoDrawableHeight;
                break;
            case 0:
                textOffsetX = width;
                textOffsetY = height - textSize;

                logoOffsetX = 0;
//                logo_offset_y = height - logoDrawableHeight;
                break;
        }

        if(addTimeStamp) {
            drawTextWithBackground(canvas, paint, timeStamp, watermarkLogo, Color.WHITE,
                    Color.TRANSPARENT, textOffsetX, textOffsetY, false, logoOffsetX);
        }else {
            drawTextWithBackground(canvas, paint, null, watermarkLogo, Color.WHITE,
                    Color.TRANSPARENT, textOffsetX, textOffsetY, false, logoOffsetX);
        }
        
        return srcBitmap;
    }
    
    @SuppressWarnings("deprecation")
    public static Bitmap dealWithWaterMark(File file, boolean addTime, Drawable watermarkLogo) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        options.inPurgeable = true;
        Bitmap srcBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        
        if(null == srcBitmap || srcBitmap.isRecycled()) {
            return null;
        }
        
        int degree = ExifUtils.getExifOrientation(file.getPath());
        
        return dealWithWaterMark(srcBitmap, addTime, watermarkLogo, degree);
    }

    public static void drawPreviewGrid(Canvas canvas, Paint paint){
        canvas.drawLine(canvas.getWidth() / 3.0f, 0.0f, canvas.getWidth() / 3.0f, canvas.getHeight() - 1.0f, paint);
        canvas.drawLine(2.0f * canvas.getWidth() / 3.0f, 0.0f, 2.0f * canvas.getWidth() / 3.0f,
                canvas.getHeight() - 1.0f, paint);
        canvas.drawLine(0.0f, canvas.getHeight() / 3.0f, canvas.getWidth() - 1.0f, canvas.getHeight() / 3.0f, paint);
        canvas.drawLine(0.0f, 2.0f * canvas.getHeight() / 3.0f, canvas.getWidth() - 1.0f,
                2.0f * canvas.getHeight() / 3.0f, paint);
    }

    private static Rect sTextBounds = new Rect();
    private static Rect sLogoBounds = new Rect();
    public static void drawTextWithBackground(Canvas canvas, Paint paint, String text, Drawable logoDrawable, int foreground, int background,
            int locationX, int locationY) {
        drawTextWithBackground(canvas, paint, text, logoDrawable, foreground, background, locationX, locationY, false);
    }

    public static void drawTextWithBackground(Canvas canvas, Paint paint, String text, Drawable logoDrawable, int foreground, int background,
            int locationX, int locationY, boolean alignTop) {
        drawTextWithBackground(canvas, paint, text, logoDrawable, foreground, background, locationX, locationY, alignTop, 0);
    }

    /**
     * Draw a text or a logo drawable or maybe both on the indicative canvas
     *
     * @param canvas where the text or logo will be drawn
     * @param paint paint used to draw the text
     * @param text the text will be drawn on the canvas, null if not need to draw
     * @param logoDrawable the logo drawable will be drawn on the canvas, null if not need to draw
     * @param foreground the text color
     * @param background the text background color
     * @param locationX the x coordinate where the text will be  drawn
     * @param locationY the y coordinate where the text will be  drawn
     * @param alignTop whether the text or logo will be aligned to the canvas's top
     * @param logoLocationX the x coordinate where the logo will be  drawn
     */
    @SuppressWarnings("deprecation")
    public static void drawTextWithBackground(Canvas canvas, Paint paint, String text, Drawable logoDrawable, int foreground, int background,
            int locationX, int locationY, boolean alignTop, int logoLocationX) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(background);
        paint.setAlpha(0);

        final int padding = (int) (2 * PicSizeUtils.sDensity + 0.5f); // convert dps to pixels

        if(null != text) {
            paint.getTextBounds(text, 0, text.length(), sTextBounds);

            if (paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER) {
                // n.b., need to use measureText rather than getTextBounds here
                float width = paint.measureText(text);

                if (paint.getTextAlign() == Paint.Align.CENTER) {
                    width /= 2.0f;
                }

                sTextBounds.left -= width;
                sTextBounds.right -= width;
            }

            sTextBounds.left += locationX - padding;
            sTextBounds.right += locationX + padding;
            if (alignTop) {
                int height = sTextBounds.bottom - sTextBounds.top + 2 * padding;
                // unclear why we need the offset of -1, but need this to align properly on Galaxy Nexus at least
                int yDiff = -sTextBounds.top + padding - 1;
                sTextBounds.top = locationY - 1;
                sTextBounds.bottom = sTextBounds.top + height;
                locationY += yDiff;
            } else {
                sTextBounds.top += locationY - padding;
                sTextBounds.bottom += locationY + padding;
            }

            canvas.drawRect(sTextBounds, paint);
            paint.setColor(foreground);
            canvas.drawText(text, locationX, locationY, paint);
        }

        if(null != logoDrawable) {
            // just make sure that the logo drawable is as high as the text
            text = "logo";
            paint.getTextBounds(text, 0, text.length(), sTextBounds);
            if (alignTop) {
                int height = sTextBounds.bottom - sTextBounds.top + 2 * padding;
                sTextBounds.top = locationY - 1;
                sTextBounds.bottom = sTextBounds.top + height;
            } else {
                sTextBounds.top += locationY - padding;
                sTextBounds.bottom += locationY + padding;
            }
            /////////////////////////////////////////////////////////////////

            paint.reset();
            sLogoBounds.left = logoLocationX + padding;
            sLogoBounds.right = logoLocationX + logoDrawable.getIntrinsicWidth() + padding;
            sLogoBounds.top = sTextBounds.top;
            sLogoBounds.bottom = sTextBounds.bottom;

            logoDrawable.setBounds(sLogoBounds);
            logoDrawable.draw(canvas);
        }
    }
}