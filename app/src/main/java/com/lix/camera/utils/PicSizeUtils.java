package com.lix.camera.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Size;
import android.view.WindowManager;

import com.lix.camera.base.BaseCamera;

@SuppressWarnings("deprecation")
public class PicSizeUtils {

    private static String TAG = PicSizeUtils.class.getSimpleName();
    
    public static Size getDefaultPictureSize(List<Size> supportPictureSize) {
        for(Size size : supportPictureSize) {
            if(isSameRatio(getPictureSize(size.getWidth(), size.getHeight()), getPictureSize(16, 9))
                    || isSameRatio(getPictureSize(size.getWidth(), size.getHeight()), getPictureSize(9, 16))) {
                return size;
            }
        }
        
        return supportPictureSize.get(0);
    }
    
    /**
     * Returns the optimal preview size for photo shots
     * 
     * @param currentActivity
     * @param sizes
     * @param targetRatio
     * @return
     */
    public static Size getOptimalPreviewSize(Activity currentActivity,
            List<Size> sizes, double targetRatio) {
        // Use a very small tolerance because we want an exact match.
        final double ASPECT_TOLERANCE = 0.01;
        if (sizes == null) {
            return null;
        }

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait mOrientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        int targetHeight = Math.min(sScreenSize.x, sScreenSize.y);
        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.getWidth() / size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }
        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            LogUtils.w(TAG, "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        return optimalSize;
    }
    
    /**
     * Returns the size of the screen
     * 
     * @param activity
     * @return Point where x=width and y=height
     */
    public static float sDensity = 1;
    public static int sRotation = 0;
    public static Point sScreenSize = null;
    public static void initScreenSizeAndDensity(Context ctx) {
        if (ctx != null) {
            sScreenSize = new Point();
            WindowManager service =
                    (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
            service.getDefaultDisplay().getSize(sScreenSize);
            sRotation = service.getDefaultDisplay().getRotation();
            sDensity = ctx.getResources().getDisplayMetrics().density;
        }
    }
    
    public static Size getPictureSize(String picSize) {
        Size size = null;
        if (picSize != null) {
            LogUtils.e(TAG, "picSize: " + picSize);
            int pos = picSize.indexOf("(");
            if(pos > 0){
                picSize = picSize.substring(0, pos);
            }
            pos = picSize.indexOf(RESOLUTION_X);
            if (pos > 0) {
                size = new Size(Integer.valueOf(picSize.substring(0, pos)), Integer.valueOf(picSize.substring(
                        pos + RESOLUTION_X.length(),
                        picSize.length())));
            }else{
                pos = picSize.indexOf("x");
                if (pos > 0) {
                    size = new Size(Integer.valueOf(picSize.substring(0, pos)),
                            Integer.valueOf(picSize.substring(
                                    pos + 1, picSize.length())));
                }
            }
        }
        return size;
    }
    
   
    /**
     *  remove picture size from supportSizes which smaller than screensize
     * @param supportSizes
     */
    public static void filterPictureSize(List<Size> supportSizes){
        if(null != supportSizes){
            Iterator<Size> it = supportSizes.iterator();
            while(it.hasNext()){
                Size tmpSize = it.next();
                if((tmpSize.getWidth() <= sScreenSize.y || tmpSize.getHeight() <= sScreenSize.x)
                        && 0 != supportSizes.indexOf(tmpSize)){
                    it.remove();
                }
            }
        }
    }
    
    /**
     * 
     * @param sourceList which is sort by w*h
     * @param destList get the maxsize of picsize of different ratio from sourceList
     */
     public static void getMaxPicSizeOfDiffRatio(List<String> sourceList, List<String> destList ){
         if(sourceList != null && !sourceList.isEmpty() && destList != null){
             for(int i = 0 ; i< sourceList.size() ; ++i){
                 if(destList.isEmpty()){
                     destList.add(sourceList.get(i)); 
                     continue;
                 }
                 
                 boolean tmpFlag = false;
                 for(String tmpSize : destList){
                     if(isSameRatio(tmpSize,sourceList.get(i))){
                         tmpFlag = true;
                         break;
                     }else{
                         continue;
                     }
                 }
                 
                 if(!tmpFlag){
                     destList.add(sourceList.get(i));
                 }
                 
             }
         }
         
     }
     
     public static boolean isSameRatio(String sourcePicSize, Size destPicSize) {
         if(sourcePicSize == null || destPicSize == null) {
             LogUtils.e(TAG, "isSameRatio parameter is null !");
             return false;
         }
         
         Size sourceSize = getPictureSize(sourcePicSize);
         
         if(sourceSize == null) {
             LogUtils.e(TAG, "sourceSize or destSize is null !");
             return false;
         }
         
         Size sourceRatioSize = closestRatioSize(sourceSize);
         Size destRatioSize = closestRatioSize(destPicSize);
         
         return (sourceRatioSize.getHeight() * destRatioSize.getWidth() == sourceRatioSize.getWidth() * destRatioSize.getHeight());
     }
     
     public static boolean isSameRatio(String sourcePicSize, String destPicSize) {
         if(sourcePicSize == null || destPicSize == null) {
             LogUtils.e(TAG, "isSameRatio parameter is null !");
             return false;
         }
         
         Size sourceSize = getPictureSize(sourcePicSize);
         Size destSize = getPictureSize(destPicSize);
         
         if(sourceSize == null || destSize == null) {
             LogUtils.e(TAG, "sourceSize or destSize is null !");
             return false;
         }
         
         Size sourceRatioSize = closestRatioSize(sourceSize);
         Size destRatioSize = closestRatioSize(destSize);
         
         return (sourceRatioSize.getHeight() * destRatioSize.getWidth() == sourceRatioSize.getWidth() * destRatioSize.getHeight());
     }
     
     public static Size closestRatioSize(Size size) {
         double closestDistance = Double.MAX_VALUE;
         int closestWidth = 0;
         int closestHeight = 0;
         
         double currentDistance = Double.MAX_VALUE;
         for(int i = 0; i < SUPPORT_RADIO.length; i++) {
             currentDistance = Math.abs((double)size.getWidth() / size.getHeight() - (double)SUPPORT_RADIO[i].getWidth() / SUPPORT_RADIO[i].getHeight());
             if(currentDistance < closestDistance) {
                 closestDistance = currentDistance;
                 closestWidth = SUPPORT_RADIO[i].getWidth();
                 closestHeight = SUPPORT_RADIO[i].getHeight();
             }
         }
         
         return new Size(closestWidth, closestHeight);
     }
     
     private static final String RESOLUTION_X = " x ";
     public static String getPictureSize(int width, int height) {
         int min = Math.min(width, height);
         int max = Math.max(width, height);
         int gcf = greatestCommonFactor(width, height);
         width /= gcf;
         height /= gcf;
         return max + RESOLUTION_X + min + closestRatioString(width, height);
     }
     
     public static void getPictureSizes(List<Size> sourceSizes, List<String> destSizes) {
         if(null == sourceSizes || null == destSizes){
             return ;
         }
         
         for(Size tmpSize : sourceSizes){
             int width = tmpSize.getWidth();
             int height = tmpSize.getHeight();
             int min = Math.min(width, height);
             int max = Math.max(width, height);
             int gcf = greatestCommonFactor(width, height);
             width /= gcf;
             height /= gcf;
             destSizes.add(max + RESOLUTION_X + min + closestRatioString(width, height));
         }
     }
     
     
     private static int greatestCommonFactor(int a, int b) {
         while( b > 0 ) {
             int temp = b;
             b = a % b;
             a = temp;
         }
         return a;
     }
     
     private static final Size[] SUPPORT_RADIO= {
         new Size(16, 9), new Size(16, 10),
         new Size(4, 3), new Size(1, 1) };
     
     private static String closestRatioString(int width, int height) {
         double closestDistance = Double.MAX_VALUE;
         int closestWidth = 0;
         int closestHeight = 0;
         
         double currentDistance = Double.MAX_VALUE;
         for(int i = 0; i < SUPPORT_RADIO.length; i++) {
             currentDistance = Math.abs((double)width / height - (double)SUPPORT_RADIO[i].getWidth() / SUPPORT_RADIO[i].getHeight());
             if(currentDistance < closestDistance) {
                 closestDistance = currentDistance;
                 closestWidth = SUPPORT_RADIO[i].getWidth();
                 closestHeight = SUPPORT_RADIO[i].getHeight();
             }
         }
             
         return "(" + closestWidth + ":" + closestHeight + ")";
     }
     
    //   ---------
 //      |       |
 //      |       |
 //      |-----  |
 //      | ++>   |
 //      |-----  |
 //      |       |
 //      |       |
 //      ---------
 // fling should be the right area
     /**
      * 
      * @param xDValue should > 1/6 screen width
      * @param y1 should 1/3 < y1 < 3/4
      * @param y2 should 1/3 < y2 < 3/4
      * @return
      */
     public static boolean isCorrectLeftFling(float xDValue, float y1, float y2){
         if(xDValue >= (sScreenSize.x/6)
                 && y1 >= (sScreenSize.y/3)
                 && y2 >= (sScreenSize.y/3)
                 && y1 <= (3*sScreenSize.y/4)
                 && y2 <= (3*sScreenSize.y/4)){
             return true;
         }
         return false;
     }
     
     /**
      * add sizesSourceA's size which in sizesSourceB to destSizeList
      * @param sizesSourceA
      * @param sizesSourceB
      * @param destSizeList
      */
    public static void refreshSizeList(String[] sizesSourceA, List<Size> sizesSourceB,
            ArrayList<String> destSizeList) {
        if (null == destSizeList || null == sizesSourceA || null == sizesSourceB
                || sizesSourceB.size() == 0) {
            return;
        }
        destSizeList.clear();

        for (Iterator<Size> it = sizesSourceB.iterator(); it.hasNext();) {
            try {
                Size size = it.next();
                if (checkResolutionSupport(size.getWidth(), size.getHeight(), sizesSourceA)) {
                    destSizeList.add(getPictureSize(size.getWidth(), size.getHeight()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
     
     public static boolean checkResolutionSupport(int width, int height, String[] support) {
         for (String value : support) {
             if (value != null && value.length() > 0) {
                 try{
                     String resolution = value.replaceAll(" ", "").toLowerCase();
                     int index = resolution.indexOf("x");
                     if (index >= 0) {
                         int widthExt = Integer.parseInt(resolution.substring(0, index));
                         int heightExt = Integer.parseInt(resolution.substring(index + 1, resolution.length()));
                         
                         if (heightExt == height && widthExt == width) {
                             return true;
                         }
                     }
                 }catch(Exception e) {
                     e.printStackTrace();
                 }
             }
         }
         return false;
     }
     
    public static Size getOptimalPreviewSize(List<Size> sizes,
            Size targetSize) {
        if (sizes == null) {
            return null;
        }
        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = Math.min(sScreenSize.x, sScreenSize.y);
        if (targetHeight <= 0) {
            targetHeight = sScreenSize.y;
        }

        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = getTargetRatioForPreview(targetSize);
        {
            // Try to find an size match aspect ratio and size
            for (Size size : sizes) {
                double ratio = (double) size.getWidth() / size.getHeight();
                if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                    continue;
                LogUtils.d(TAG, "    supported preview size: " + size.getWidth() + ", " + size.getHeight() + " "
                        + ratio);
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight() - targetHeight);
                }
            }
        }
        if (optimalSize == null) {
            // can't find match for aspect ratio, so find closest one
            LogUtils.d(TAG, "no preview size matches the aspect ratio");
            optimalSize = getClosestSize(sizes, targetRatio);
        }
        LogUtils.d(TAG, "chose optimalSize: " + optimalSize.getWidth() + " x " + optimalSize.getHeight());
        LogUtils.d(TAG, "optimalSize ratio: " + ((double) optimalSize.getWidth() / optimalSize.getHeight()));
        return optimalSize;
    }
     
     public static double getTargetRatioForPreview(Size targetSize) {
         double targetRatio;
         {
             if(targetSize == null){
                 return 16/9;
             }
             LogUtils.d(TAG, "picture_size: " + targetSize.getWidth() + " x " + targetSize.getHeight());
             targetRatio = ((double) targetSize.getWidth()) / (double) targetSize.getHeight();
         }
         return targetRatio;
     }

     public static Size getClosestSize(List<Size> sizes, double targetRatio) {
         Size optimalSize = null;
         double minDiff = Double.MAX_VALUE;
         for (Size size : sizes) {
             double ratio = (double) size.getWidth() / size.getHeight();
             if (Math.abs(ratio - targetRatio) < minDiff) {
                 optimalSize = size;
                 minDiff = Math.abs(ratio - targetRatio);
             }
         }
         return optimalSize;
     }
     
     /**
      * sort by small to big.
      */
     public static void sortPreviewSizes(List<Size> supportedPreviewSizes) {
         LogUtils.d(TAG, "sortPreviewSizes()");
         if (supportedPreviewSizes != null && supportedPreviewSizes.size() > 0){
             Collections.sort(supportedPreviewSizes, new Comparator<Size>() {
                 public int compare(final Size current, final Size next) {
                     return current.getWidth() * current.getHeight() - next.getWidth() * next.getHeight();
                 }
             });
         }
     }

     /**
      * sort by big to small.
      */
     public static void sortPictureSizes(List<Size> supportedPicSizes) {
         LogUtils.d(TAG, "sortPictureSizes()");
         if (supportedPicSizes != null && supportedPicSizes.size() > 0){
             Collections.sort(supportedPicSizes, new Comparator<Size>() {
                 public int compare(final Size current, final Size next) {
                     return next.getWidth() * next.getHeight() - current.getWidth() * current.getHeight();
                 }
             });
         }
     }
     
     private static Matrix mCameraToPreviewMatrix = new Matrix();

     private static Matrix mPreviewToCameraMatrix = new Matrix();
     
     public static void calculateCameraToPreviewMatrix(boolean isFrontCamera,float displayOrientation,
                                                       int viewWidth, int viewHeight, RectF rect) {

         mCameraToPreviewMatrix.reset();
         // from http://developer.android.com/reference/android/hardware/Camera.Face.html#rect
         // Need mirror for front camera
        
         mCameraToPreviewMatrix.setScale(isFrontCamera ? -1 : 1, 1);
         // This is the value for android.hardware.Camera.setDisplayOrientation.
         mCameraToPreviewMatrix.postRotate(displayOrientation);
         // Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
         // UI coordinates range from (0, 0) to (width, height).
         mCameraToPreviewMatrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
         mCameraToPreviewMatrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
         if(null != rect){
             mCameraToPreviewMatrix.mapRect(rect);
         }
     }

     public static void calculatePreviewToCameraMatrix(boolean isFrontCamera, float displayOrientation
             , int viewWidth, int viewHeight) {
         calculateCameraToPreviewMatrix(isFrontCamera, displayOrientation, viewWidth, viewHeight, null);
         if (!mCameraToPreviewMatrix.invert(mPreviewToCameraMatrix)) {
            LogUtils.d(TAG, "calculatePreviewToCameraMatrix failed to invert matrix!?");
         }
     }
     
     public static ArrayList<BaseCamera.Area> getAreas(float x, float y, boolean isFrontCamera, float displayOrientation
             , int viewWidth, int viewHeight) {
         float[]  coordinates = {
                 x, y
         };
         calculatePreviewToCameraMatrix(isFrontCamera, displayOrientation, viewWidth, viewHeight);
         mPreviewToCameraMatrix.mapPoints(coordinates);
         float focus_x = coordinates[0];
         float focus_y = coordinates[1];

         int focus_size = 50;
         LogUtils.d(TAG, "x, y: " + x + ", " + y);
         LogUtils.d(TAG, "focus x, y: " + focus_x + ", " + focus_y);
         
         Rect rect = new Rect();
         rect.left = (int) focus_x - focus_size;
         rect.right = (int) focus_x + focus_size;
         rect.top = (int) focus_y - focus_size;
         rect.bottom = (int) focus_y + focus_size;
         if (rect.left < -1000) {
             rect.left = -1000;
             rect.right = rect.left + 2 * focus_size;
         }
         else if (rect.right > 1000) {
             rect.right = 1000;
             rect.left = rect.right - 2 * focus_size;
         }
         if (rect.top < -1000) {
             rect.top = -1000;
             rect.bottom = rect.top + 2 * focus_size;
         }
         else if (rect.bottom > 1000) {
             rect.bottom = 1000;
             rect.top = rect.bottom - 2 * focus_size;
         }

         ArrayList<BaseCamera.Area> areas = new ArrayList<BaseCamera.Area>();
         areas.add(new BaseCamera.Area(rect, 1000));
         return areas;
     }

}
