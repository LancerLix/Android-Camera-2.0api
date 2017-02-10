package com.lix.camera.ui.main;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.lix.camera.utils.LogUtils;

public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private final String TAG = CameraSurfaceView.class.getSimpleName();

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;
    private boolean mBHasAspectRatio = false;

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtils.d(TAG, "surfaceCreated...");
        this.setWillNotDraw(false);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtils.d(TAG, "surfaceDestroyed...");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        
    }

    private void setAspectRatio(final int width, final int height) {
        LogUtils.d(TAG, "setAspectRatio width:" + width + " height:" + height);

        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }

        mBHasAspectRatio = true;

        if(mRatioWidth != width || mRatioHeight != height) {
            this.post(new Runnable() {

                @Override
                public void run() {
                    mRatioWidth = width;
                    mRatioHeight = height;
                    requestLayout();
                }
            });
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!mBHasAspectRatio) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        double aspectRatio = (double)mRatioWidth / (double)mRatioHeight;

        int previewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int previewHeight = MeasureSpec.getSize(heightMeasureSpec);

        // Get the padding of the border background.
        int hPadding = getPaddingLeft() + getPaddingRight();
        int vPadding = getPaddingTop() + getPaddingBottom();

        // Resize the preview frame with correct aspect ratio.
        previewWidth -= hPadding;
        previewHeight -= vPadding;

        boolean widthLonger = previewWidth > previewHeight;
        int longSide = (widthLonger ? previewWidth : previewHeight);
        int shortSide = (widthLonger ? previewHeight : previewWidth);
        if (longSide > shortSide * aspectRatio) {
            longSide = (int) ((double) shortSide * aspectRatio);
        }/* else {
            shortSide = (int) ((double) longSide / mAspectRatio);
        }*/
        if (widthLonger) {
            previewWidth = longSide;
            previewHeight = shortSide;
        } else {
            previewWidth = shortSide;
            previewHeight = longSide;
        }

        // Add the padding of the border.
        previewWidth += hPadding;
        previewHeight += vPadding;

        LogUtils.d(TAG, "onMeasure: w,"+previewWidth+" h,"+previewHeight);
        // Ask children to follow the new preview dimension.
        super.onMeasure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
    }
}
