package com.lix.camera.ui.base.optionmenu;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.lix.camera.R;

/**
 * An base option menu that support pop or hide with specify type.
 */
abstract public class BaseOptionMenu extends FrameLayout implements View.OnClickListener {
    
    public interface OnOptionMenuItemClickListener {
        void onClick(View v);
    }
    
    private OnOptionMenuItemClickListener mListener;
    
    private ViewGroup mSettingContentView = null;
    
    private Animation mOptionMenuPopupAnimation;
    private Animation mOptionMenuDismissAnimation;
    
    private LayoutInflater mLayoutInflater;
    
    //control two animation of mutual exclusion
    private boolean mAleadyShow = false;
    
    //control animation itself of mutual exclusion
    private boolean mIsShowing = false; 
    private boolean mIsHiding = false;
    
    private boolean mDisablePopup = false;
    
    abstract protected ViewGroup getTypeContentView(int type);
    abstract protected void initContentView(ViewGroup viewGroup);
    abstract public void setOptionMenuUIRotation(int uiRotation);
    
    public BaseOptionMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize(context);
    }
    
    public BaseOptionMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context);
    }
    
    public BaseOptionMenu(Context context) {
        super(context);
        initialize(context);
    }
    
    public LayoutInflater getLayoutInflater() {
        return mLayoutInflater;
    }
    
    public ViewGroup getContentView() {
        return mSettingContentView;
    }
    
    public FrameLayout getContentViewContainer() {
        return this;
    }
    
    public void setOnOptionMenuItemClickListener(OnOptionMenuItemClickListener listener) {
        mListener = listener;
    }
    
    private void initialize(Context context) {
        
        mOptionMenuPopupAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.option_menu_popup);
        mOptionMenuDismissAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.option_menu_dismiss);
        
        mOptionMenuPopupAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                mIsShowing = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mAleadyShow = true;
                mIsShowing = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            
        });
        
        mOptionMenuDismissAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
                mIsHiding = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mAleadyShow = false;
                mIsHiding = false;
                getContentViewContainer().setVisibility(GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            
        });
        
        mLayoutInflater = LayoutInflater.from(context);
    }
    
    private void attachContentView() {
        if(mSettingContentView != null && this.getChildAt(0) == null) {
            this.addView(mSettingContentView);
            this.requestLayout();
        }
    }
    
    private void detachContentView() {
        if(this.getChildAt(0) != null) {
            this.removeAllViews();
        }
    }

    public void prepareContentView(int type, boolean disablePopup) {
        detachContentView();
        mSettingContentView = getTypeContentView(type);
        initContentView(mSettingContentView);
        attachContentView();
        
        this.setVisibility(disablePopup ? VISIBLE : GONE);
        mDisablePopup = disablePopup;
    }
    
    public void popupOptionMenu() {
        if(mDisablePopup) {
            return;
        }
        
        if(mAleadyShow) {
            hideOptionMenu();
            return;
        }
        
        if(mOptionMenuPopupAnimation != null && mSettingContentView != null && !mIsShowing) {
            this.setVisibility(VISIBLE);
            this.startAnimation(mOptionMenuPopupAnimation);
        }
    }
    
    public void hideOptionMenu() {
        if(mDisablePopup) {
            return;
        }
        
        if(mOptionMenuDismissAnimation != null && mSettingContentView != null && mAleadyShow && !mIsHiding) {
            this.startAnimation(mOptionMenuDismissAnimation);
        }
    }
    
    @Override
    public void onClick(View v) {
        if(null != mListener) {
            mListener.onClick(v);
        }
    }
    
    public void setContentViewEnable(boolean enabled) {
        if(null != mSettingContentView) {
            mSettingContentView.setEnabled(enabled);
        }
        this.setEnabled(enabled);
    }
}