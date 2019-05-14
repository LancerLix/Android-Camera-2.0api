package com.lix.camera.ui.bottom;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.lix.camera.R;
import com.lix.camera.utils.ImageUtils;

/**
 * Created by lix on 2017/2/13.
 *
 */
public class GalleryButton extends ImageView {

    private Paint mPaint;

    private int mWidth;
    private int mHeight;
    private int mRadius;

    public GalleryButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        updateGalleryThumbnail(ImageUtils.decodeResource(context.getResources(), R.drawable.color_effects_putong));
    }

    public GalleryButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        updateGalleryThumbnail(ImageUtils.decodeResource(context.getResources(), R.drawable.color_effects_putong));
    }

    public GalleryButton(Context context) {
        super(context);

        updateGalleryThumbnail(ImageUtils.decodeResource(context.getResources(), R.drawable.color_effects_putong));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mWidth = w;
        mHeight = h;
        mRadius = Math.min(mWidth, mHeight) / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(mWidth / 2, mHeight / 2, mRadius, mPaint);
    }

    public void updateGalleryThumbnail(Bitmap thumbnail) {
        BitmapShader bitmapShader = new BitmapShader(thumbnail, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        if(null == mPaint) {
            mPaint = new Paint();
        }

        mPaint.setAntiAlias(true);
        mPaint.setShader(bitmapShader);
    }
}
