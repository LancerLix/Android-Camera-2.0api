package com.lix.camera.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.ImageWriteException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.JpegImageData;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.constants.ExifTagConstants;
import org.apache.sanselan.formats.tiff.constants.TagInfo;
import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.constants.TiffDirectoryConstants;
import org.apache.sanselan.formats.tiff.constants.TiffFieldTypeConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputField;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.util.Log;

import com.lix.camera.utils.pq.ExposureCorrect;

public class ExifUtils {
    /**
     * copy exif data from source file to dest file
     * @param sourceFile
     * @param destFile
     * @param excludedFields not copy fields, can be null
     * @param userComment add user's comment, can be null
     * @return
     */
    @SuppressWarnings("deprecation")
    public static boolean copyExifData(File sourceFile, File destFile, List<TagInfo> excludedFields, String userComment, int exposureCorrectLevel)
    {
        String tempFileName = destFile.getAbsolutePath() + ".tmp";
        File tempFile = null;
        OutputStream tempStream = null;
     
        try
        {
            tempFile = new File (tempFileName);
     
            TiffOutputSet sourceSet = getSanselanOutputSet(sourceFile, TiffConstants.DEFAULT_TIFF_BYTE_ORDER);
            TiffOutputSet destSet = getSanselanOutputSet(destFile, sourceSet.byteOrder);
     
            // If the EXIF data endianess of the source and destination files
            // differ then fail. This only happens if the source and
            // destination images were created on different devices. It's
            // technically possible to copy this data by changing the byte
            // order of the data, but handling this case is outside the scope
            // of this implementation
            if (sourceSet.byteOrder != destSet.byteOrder) return false;
     
            destSet.getOrCreateExifDirectory();
     
            // Go through the source directories
            List<?> sourceDirectories = sourceSet.getDirectories();
            for (int i = 0 ; i < sourceDirectories.size(); i++)
            {
                
                TiffOutputDirectory sourceDirectory = (TiffOutputDirectory)sourceDirectories.get(i);
                TiffOutputDirectory destinationDirectory = getOrCreateExifDirectory(destSet, sourceDirectory);
                
                if (destinationDirectory == null) continue; // failed to create
     
                //thumbnail
                if(sourceDirectory != null){
                    if(TiffDirectoryConstants.DIRECTORY_TYPE_SUB == sourceDirectory.type || TiffDirectoryConstants.DIRECTORY_TYPE_THUMBNAIL == sourceDirectory.type){
                        if(ExposureCorrect.CorrectLevel.LEVEL_NONE != exposureCorrectLevel){
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inMutable = false;
                            options.inPurgeable = true;
                            Bitmap bitmap = BitmapFactory.decodeByteArray(sourceDirectory.getRawJpegImageData().data, 0, sourceDirectory.getRawJpegImageData().data.length, options);
                            ExposureCorrect.whiteBalanceCorrect(
                                    bitmap, bitmap.getWidth(), bitmap.getHeight(), exposureCorrectLevel);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos);
                            destinationDirectory.setJpegImageData(new JpegImageData(sourceDirectory.getRawJpegImageData().offset, baos.size(), baos.toByteArray()));
                        }else{
                            destinationDirectory.setJpegImageData(sourceDirectory.getRawJpegImageData());
                        }
                    }
                }
                // Loop the fields
                List<?> sourceFields = sourceDirectory.getFields();
                for (int j=0; j<sourceFields.size(); j++)
                {
                    // Get the source field
                    TiffOutputField sourceField = (TiffOutputField) sourceFields.get(j);
     
                    // Check exclusion list
                    if (excludedFields != null && excludedFields.contains(sourceField.tagInfo))
                    {
                        destinationDirectory.removeField(sourceField.tagInfo);
                        continue;
                    }
                    
                    // Remove any existing field
                    destinationDirectory.removeField(sourceField.tagInfo);
     
                    // Add field
                    destinationDirectory.add(sourceField);
                }
            }
     
            if(userComment != null){
                TiffOutputDirectory destinationDirectoryD = destSet.getOrCreateExifDirectory();
                if(destinationDirectoryD != null){
                    destinationDirectoryD.removeField(ExifTagConstants.EXIF_TAG_USER_COMMENT.tag);
                    byte[] bytesComment = ExifTagConstants.EXIF_TAG_USER_COMMENT.encodeValue(TiffFieldTypeConstants.FIELD_TYPE_ASCII, userComment, sourceSet.byteOrder); 
                    TiffOutputField commentField = new TiffOutputField(ExifTagConstants.EXIF_TAG_USER_COMMENT, ExifTagConstants.EXIF_TAG_USER_COMMENT.dataTypes[0], bytesComment.length, bytesComment); 
                    destinationDirectoryD.add(commentField);
                }
            }
            
            // Save data to destination
            tempStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            new ExifRewriter().updateExifMetadataLossless(destFile, tempStream, destSet);
            tempStream.close();
     
            // Replace file
            if (destFile.delete())
            {
                tempFile.renameTo(destFile);
            }
     
            return true;
        }
        catch (ImageReadException exception)
        {
            exception.printStackTrace();
        }
        catch (ImageWriteException exception)
        {
            exception.printStackTrace();
        }
        catch (IOException exception)
        {
            exception.printStackTrace();
        }
        finally
        {
            if (tempStream != null)
            {
                try
                {
                    tempStream.close();
                }
                catch (IOException e)
                {
                }
            }
     
            if (tempFile != null)
            {
                if (tempFile.exists()) tempFile.delete();
            }
        }
     
        return false;
    }
     
    private static TiffOutputSet getSanselanOutputSet(File jpegImageFile, int defaultByteOrder)
            throws IOException, ImageReadException, ImageWriteException
    {
        TiffImageMetadata exif = null;
        TiffOutputSet outputSet = null;
     
        IImageMetadata metadata = Sanselan.getMetadata(jpegImageFile);
        JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
        if (jpegMetadata != null)
        {
            exif = jpegMetadata.getExif();
     
            if (exif != null)
            {
                outputSet = exif.getOutputSet();
            }
        }
     
        // If JPEG file contains no EXIF metadata, create an empty set
        // of EXIF metadata. Otherwise, use existing EXIF metadata to
        // keep all other existing tags
        if (outputSet == null)
            outputSet = new TiffOutputSet(exif==null?defaultByteOrder:exif.contents.header.byteOrder);
     
        return outputSet;
    }
     
    private static TiffOutputDirectory getOrCreateExifDirectory(TiffOutputSet outputSet, TiffOutputDirectory outputDirectory)
    {
        TiffOutputDirectory result = outputSet.findDirectory(outputDirectory.type);
        if (result != null)
            return result;
        result = new TiffOutputDirectory(outputDirectory.type);
        try
        {
            outputSet.addDirectory(result);
        }
        catch (ImageWriteException e)
        {
            return null;
        }
        return result;
    }
    
    public static int getRotationByExifOrientation(String exifOrientation){
        if(null == exifOrientation){
            return 0;
        }

        int rotation;
        switch(exifOrientation) {
            case ""+ExifInterface.ORIENTATION_ROTATE_90:
                rotation = 90;
                break;
            case ""+ExifInterface.ORIENTATION_ROTATE_180:
                rotation = 180;
                break;
            case ""+ExifInterface.ORIENTATION_ROTATE_270:
                rotation = 270;
                break;
            default:
                rotation = 0;
                break;

        }

        return rotation;
    }

    public static String getExifOrientationByRotation(int rotation){
        switch(rotation){
            case 90:
                return ""+ExifInterface.ORIENTATION_ROTATE_90;
            case 180:
                return ""+ExifInterface.ORIENTATION_ROTATE_180;
            case 270:
                return ""+ExifInterface.ORIENTATION_ROTATE_270;
            case 0:
                return "1";
            default:
                return "1";
        }
    }
    public static int getExifOrientation(String filepath) {  
        int degree = 0;  
        ExifInterface exif = null;  
        try {  
            exif = new ExifInterface(filepath);  
        } catch (IOException ex) {  
            Log.d("ExifUtil", "cannot read exif" + ex);  
        }  
        if (exif != null) {  
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);  
            if (orientation != -1) {  
                switch(orientation) {  
                    case ExifInterface.ORIENTATION_ROTATE_90:  
                        degree = 90;  
                        break;  
                    case ExifInterface.ORIENTATION_ROTATE_180:  
                        degree = 180;  
                        break;  
                    case ExifInterface.ORIENTATION_ROTATE_270:  
                        degree = 270;  
                        break;  
                }  
            }  
        }  
        return degree;  
    } 
    
    /**
     * correct picture exif orientation
     * @param picFile picture file which the exif orientation message need to be correct
     * @param cameraRotation current camera rotation
     * @param isFrontCamera true if the front camera is working
     * @return the corrected orientation
     */
    public static String correctExifOrientation(File picFile, int cameraRotation , boolean isFrontCamera) {
        String exifOrientation = null;
        if (picFile != null) {

            try {
                ExifInterface exifO = new ExifInterface(picFile.getAbsolutePath());
                exifOrientation = exifO.getAttribute(ExifInterface.TAG_ORIENTATION);
               
                if (!exifOrientation.equals("0")) {
                    return exifOrientation;
                }

                if(PhoneUtils.needCorrectExif()) {
                    if (isFrontCamera && PhoneUtils.isMx2()) {
                        cameraRotation = (cameraRotation + 180) % 360;
                    }
                    exifOrientation = ExifUtils.getExifOrientationByRotation(cameraRotation);
                    exifO.setAttribute(ExifInterface.TAG_ORIENTATION, exifOrientation);
                    exifO.saveAttributes();
                }
              
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        return exifOrientation;
    }
    
}
