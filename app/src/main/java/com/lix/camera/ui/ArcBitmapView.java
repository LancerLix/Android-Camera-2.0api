package com.lix.camera.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.lix.camera.R;

/**
 * Created by lix on 2017/4/6.
 *
 */
public class ArcBitmapView extends View {

    private Paint mPaint = null;

    private RectF mArcRect = new RectF();
    private float mRadius;

    private int mBitmapWidth;
    private int mBitmapHeight;

    private BitmapShader mBitmapShader;
    private Matrix mShaderMatrix;

    private ValueAnimator mAnimator;
    private int mArcAngle;

    public ArcBitmapView(Context context) {
        super(context);
        initPaintAndShader();
    }

    public ArcBitmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaintAndShader();
    }

    public ArcBitmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initPaintAndShader();
    }

    @NonNull
    private View getView() {
        return this;
    }

    @SuppressWarnings("deprecation")
    private void initPaintAndShader() {
        mPaint = new Paint();

        mBitmapShader = new BitmapShader(getBitmapFromDrawable(getResources().getDrawable(R.drawable.btn_filter_control_normal)), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        mShaderMatrix = new Matrix();

        mPaint.setShader(mBitmapShader);

        mAnimator = ValueAnimator.ofInt(0,360);

        mAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mArcAngle = (int)animation.getAnimatedValue();

                getView().invalidate();
            }
        });
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setDuration(5000);

        post(new Runnable() {
            @Override
            public void run() {
                if(null != mAnimator) {
                    mAnimator.start();
                }
            }
        });
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        mBitmapWidth = drawable.getIntrinsicWidth();
        mBitmapHeight = drawable.getIntrinsicHeight();
        Bitmap bitmap = Bitmap.createBitmap(mBitmapWidth, mBitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, mBitmapWidth, mBitmapHeight);
        drawable.draw(canvas);
        return bitmap;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int minSize = Math.min(getMeasuredHeight(), getMeasuredWidth());

        setMeasuredDimension(minSize, minSize);

        mArcRect.set(0, 0, minSize, minSize);
        mRadius = minSize / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {

        float xScale = (mRadius * 2.0f) / mBitmapWidth;
        float yScale = (mRadius * 2.0f) / mBitmapHeight;

        // this may cause the view be out of shape if the bitmap width and height are not equal
        mShaderMatrix.setScale(xScale, yScale);
        mBitmapShader.setLocalMatrix(mShaderMatrix);

        canvas.drawArc(mArcRect, 0, mArcAngle, true, mPaint);
    }
}
