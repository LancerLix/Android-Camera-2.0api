package com.lix.camera.utils;

import java.io.File;
import java.io.IOException;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Images.ImageColumns;
import android.provider.MediaStore.Video.VideoColumns;

@SuppressWarnings("deprecation")
public class MediaUtils {
    private final static String TAG = MediaUtils.class.getSimpleName();

    public static final int MEDIA_FORMAT_IMAGE = 1;

    public static final int MEDIA_FORMAT_VIDEO = 2;

    public static class MediaData {
        public long mId;

        public boolean mVideo;

        public Uri mUri;

        public long mDate;

        public int mOrientation;

        MediaData(long mId, boolean mVideo, Uri mUri, long mDate, int mOrientation) {
            this.mId = mId;
            this.mVideo = mVideo;
            this.mUri = mUri;
            this.mDate = mDate;
            this.mOrientation = mOrientation;
        }
    }
    
    public static MediaData getLatestMedia(int mediaFormat, Context ctx) {
        MediaData imageMedia = null;
        MediaData videoMedia = null;
        if (mediaFormat == MEDIA_FORMAT_VIDEO) {
            videoMedia = getLatestMedia(true, ctx);
            if (videoMedia == null) {
                imageMedia = getLatestMedia(false, ctx);
            }
        } else {
            imageMedia = getLatestMedia(false ,ctx);
            if (imageMedia == null) {
                videoMedia = getLatestMedia(true, ctx);
            }
        }

        MediaData media = null;
        if (imageMedia != null) {
            LogUtils.d(TAG, "mediaFormat : " + mediaFormat + ", found for images");
            media = imageMedia;
        } else if (videoMedia != null) {
            LogUtils.d(TAG, "mediaFormat : " + mediaFormat + ", found for videos");
            media = videoMedia;
        }

        return media;
    }

    private static MediaData getLatestMedia(boolean isVideo, Context ctx) {
        MediaData media = null;
        Uri baseUri = isVideo ? Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = isVideo ? new String[] {
                VideoColumns._ID,
                VideoColumns.DATE_TAKEN, Video.Media.DATA
        }
                : new String[] {
                ImageColumns._ID, ImageColumns.DATE_TAKEN,
                ImageColumns.ORIENTATION, MediaStore.Images.Media.DATA
        };
        String selection = isVideo ? ""
                : ImageColumns.MIME_TYPE + "='image/jpeg'";
        String order = isVideo ? VideoColumns.DATE_TAKEN + " DESC," + VideoColumns._ID + " DESC"
                : ImageColumns.DATE_TAKEN + " DESC," + ImageColumns._ID + " DESC";
        Cursor cursor = null;
        try {
            cursor = ctx.getContentResolver().query(baseUri, projection, selection,
                    null, order);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String name = isVideo ? cursor.getString(2) : cursor
                            .getString(3);
                    if (FileUtils.isCustomPathFile(name)) {
                        long id = cursor.getLong(0);
                        long date = cursor.getLong(1);
                        int orientation = isVideo ? 0 : cursor.getInt(2);
                        Uri uri = ContentUris.withAppendedId(baseUri, id);
                        LogUtils.d(TAG, "found most recent mUri for "
                                + (isVideo ? "mVideo" : "images")
                                + ": " + uri + "===>" + name);
                        media = new MediaData(id, isVideo, uri, date, orientation);
                        break;
                    }
                    cursor.moveToNext();
                } while (!cursor.isAfterLast());

            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return media;
    }
    
    public static void startGallery(Uri uri, Context ctx){
        if(null == ctx) {
            LogUtils.e(TAG, "start gallery ");
            return;
        }

        if (null != uri) {
            // check mUri exists
            LogUtils.d(TAG, "found most recent mUri: " + uri);
            try {
                ContentResolver cr = ctx.getContentResolver();
                ParcelFileDescriptor pfd = cr.openFileDescriptor(uri, "r");
                if (pfd == null) {
                    LogUtils.d(TAG, "mUri no longer exists (1): " + uri);
                    uri = null;
                }else{
                    pfd.close();
                }
            } catch (IOException e) {
                LogUtils.d(TAG, "mUri no longer exists (2): " + uri);
                uri = null;
            }
        }
        
        if (null == uri) {
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        final String REVIEW_ACTION = "com.eos.camera.action.REVIEW";
        try {
            if(PhoneUtils.isHuaweiPhone()) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                //huawei phone must set data and type, nor will open the system gallery
                intent.setDataAndType(uri, "image/*");
                ctx.startActivity(intent);
            }else {
                Intent intent = new Intent(REVIEW_ACTION, uri);
                ctx.startActivity(intent);
            }
        } catch (ActivityNotFoundException e) {
            LogUtils.d(TAG, "REVIEW_ACTION intent didn't work, try ACTION_VIEW");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            
            if (intent.resolveActivity(ctx.getPackageManager()) != null) {
                ctx.startActivity(intent);
            }  
        }
    }
    
    public static void insertImageToMediaStore(final File file, final String filePath, final Context ctx) {
        LogUtils.d(TAG, "broadcastFile");
        if(null == file || null == ctx){
            return;
        }

        if (!file.isDirectory()) {
            MediaScannerConnection.scanFile(ctx, new String[] {
                    filePath
            }, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    LogUtils.d(TAG, "Scanned " + path + ":");
                    LogUtils.d(TAG, "-> mUri=" + uri);
                }
            });

            insertImageToMediaStoreProvider(file, filePath, ctx);
        }
    }
    
    private static void insertImageToMediaStoreProvider(final File localFile, final String path, final Context ctx) {
        String filePath = path == null ? localFile.getAbsolutePath() : path;
        LogUtils.d(TAG, "insertMediaStoreImage= " + filePath);
         
        ContentValues localContentValues = new ContentValues();
        localContentValues.put(MediaStore.Images.Media.DATA, filePath);
        localContentValues.put(MediaStore.Images.Media.SIZE, localFile.length());
        localContentValues.put(MediaStore.Images.Media.DISPLAY_NAME, localFile.getName());
        localContentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        localContentValues.put(MediaStore.Images.Media.DATE_MODIFIED,
                System.currentTimeMillis() / 1000);
        Uri imageFileUri = ctx.getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, localContentValues);
        LogUtils.d(TAG, "imageFileUri = " + imageFileUri);
        if (imageFileUri != null) {
            ctx.sendBroadcast(new Intent(Camera.ACTION_NEW_PICTURE, imageFileUri));
            ctx.sendBroadcast(new Intent("com.eos.camera.NEW_PICTURE", imageFileUri));
        }
    }
}
