package com.lix.camera.utils.pq;

import java.nio.FloatBuffer;

import android.media.effect.Effect;
import android.media.effect.EffectContext;
import android.media.effect.EffectFactory;
import android.opengl.GLES20;

public class GPUImageEffectFilter extends GPUImageFilter{
    
    private boolean mUseEffect = false;
    
    private boolean mEffectContextInitialized = false;
    private EffectContext mEffectContext;
    private Effect mEffect;
    private int[] mEffectTexture = new int[1];
    private int mEffectType;
    private int mImageWidth;
    private int mImageHeight;
    private float mScale;
    
    public static final int EFFECT_TYPE_NONE = 0;
    public static final int EFFECT_TYPE_AUTO_FIX = 1;
    
    public GPUImageEffectFilter(int effectType, int imageWidth, int imageHeight) {
        mEffectType = effectType;
        mImageWidth = imageWidth;
        mImageHeight = imageHeight;
    }
    
    public void setScale(float scale) {
        mScale = scale;
    }

    @Override
    public void onDraw(int textureId, FloatBuffer cubeBuffer, FloatBuffer textureBuffer) {
        if(!mEffectContextInitialized) {
            GLES20.glGenTextures(1, mEffectTexture, 0);
            mEffectContext = EffectContext.createWithCurrentGlContext();
            mEffectContextInitialized = true;
        }
        
        initEffect();
        
        int usedTextureId = applyEffect(textureId);
        
        super.onDraw(usedTextureId, cubeBuffer, textureBuffer);
    }
    
    private void initEffect() {
        EffectFactory effectFactory = mEffectContext.getFactory();
        if (mEffect != null) {
            mEffect.release();
        }
        
        mUseEffect = true;
        
        switch (mEffectType) {

            case EFFECT_TYPE_NONE:
                mUseEffect = false;
                break;

            case EFFECT_TYPE_AUTO_FIX:
                mEffect = effectFactory.createEffect(
                        EffectFactory.EFFECT_AUTOFIX);
                mEffect.setParameter("scale", mScale);
                break;

            default:
                mUseEffect = false;
                break;
        }
    }

    private int applyEffect(int originalTexture) {
        if(mUseEffect) {
            mEffect.apply(originalTexture, mImageWidth, mImageHeight, mEffectTexture[0]);
            return mEffectTexture[0];
        }else {
            return originalTexture;
        }
    }

    @Override
    public void onDestroy() {
        GLES20.glDeleteTextures(1, mEffectTexture, 0);
        super.onDestroy();
    }
}