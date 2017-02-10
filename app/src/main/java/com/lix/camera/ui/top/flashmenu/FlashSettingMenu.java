package com.lix.camera.ui.top.flashmenu;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.lix.camera.R;
import com.lix.camera.base.BaseCamera;

/**
 * Created by lix on 2016/8/8.
 *
 */
@SuppressWarnings("deprecation")
public class FlashSettingMenu extends ViewGroup {

    private final String TAG = FlashSettingMenu.class.getSimpleName();

    private Context mContext;

    public static final int EXPAND_UP = 0;
    public static final int EXPAND_DOWN = 1;
    public static final int EXPAND_LEFT = 2;
    public static final int EXPAND_RIGHT = 3;

    private static final int ANIMATION_DURATION = 300;

    private int mExpandDirection;

    private int mButtonSpacing;

    private boolean mExpanded;

    private AnimatorSet mExpandAnimation = new AnimatorSet().setDuration(ANIMATION_DURATION);
    private AnimatorSet mCollapseAnimation = new AnimatorSet().setDuration(ANIMATION_DURATION);
    private ImageButton mCurrentFlashButton;
    private int mMaxButtonWidth;
    private int mMaxButtonHeight;
    private int mButtonsCount;

    private TouchDelegateGroup mTouchDelegateGroup;

    private OnFlashMenuUpdateListener mOnFlashMenuUpdateListener;

    private OnFlashMenuOptionSelectListener mOnFlashMenuOptionSelectListener;

    private String[] FLASH_VALUE_OPTIONS = {
            BaseCamera.FlashValue.FLASH_ON, BaseCamera.FlashValue.FLASH_OFF,
            BaseCamera.FlashValue.FLASH_AUTO, BaseCamera.FlashValue.FLASH_TORCH
    };

    private int[] FLASH_IMAGE_RES_IDS = {
            R.drawable.ic_btn_flash_on, R.drawable.ic_btn_flash_off,
            R.drawable.ic_btn_flash_auto, R.drawable.ic_btn_flash_always_on
    };

    public interface OnFlashMenuUpdateListener {
        void onMenuExpanded();
        void onMenuCollapsed();
    }

    public interface OnFlashMenuOptionSelectListener {
        void onOptionSelected(String tag);
    }

    public FlashSettingMenu(Context context) {
        this(context, null);
    }

    public FlashSettingMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public FlashSettingMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {

        mContext = context;

        mButtonSpacing = (int) (getResources().getDimension(R.dimen.flash_menu_button_spacing));

        mTouchDelegateGroup = new TouchDelegateGroup(this);
        setTouchDelegate(mTouchDelegateGroup);

        TypedArray attr = context.obtainStyledAttributes(attributeSet, R.styleable.FloatingActionsMenu, 0, 0);
        mExpandDirection = attr.getInt(R.styleable.FloatingActionsMenu_fab_expandDirection, EXPAND_RIGHT);
        attr.recycle();

        initCapability(BaseCamera.FlashValue.FLASH_AUTO, new String[]{
                BaseCamera.FlashValue.FLASH_ON, BaseCamera.FlashValue.FLASH_OFF,
                BaseCamera.FlashValue.FLASH_AUTO, BaseCamera.FlashValue.FLASH_TORCH
        });
    }

    public void setOnFlashMenuUpdateListener(OnFlashMenuUpdateListener listener) {
        mOnFlashMenuUpdateListener = listener;
    }

    public void setOnFlashMenuOptionSelectListener(OnFlashMenuOptionSelectListener listener) {
        mOnFlashMenuOptionSelectListener = listener;
    }

    private boolean expandsHorizontally() {
        return mExpandDirection == EXPAND_LEFT || mExpandDirection == EXPAND_RIGHT;
    }

    public void initCapability(String currentFlashValue, String[] supportFlashValue) {

        if(0 != getChildCount()) {
            removeAllViews();
        }

        mCurrentFlashButton = new ImageButton(mContext);

        mCurrentFlashButton.setTag(currentFlashValue);
        mCurrentFlashButton.setBackground(getResources().getDrawable(getFlashImgResId(currentFlashValue)));
        mCurrentFlashButton.setScaleType(ImageView.ScaleType.FIT_XY);

        mCurrentFlashButton.setLayoutParams(new LayoutParams(new ViewGroup.LayoutParams(
                (int) (getResources().getDimension(R.dimen.top_menu_button_size)),
                (int) (getResources().getDimension(R.dimen.top_menu_button_size)))));

        mCurrentFlashButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

        addView(mCurrentFlashButton);
        mButtonsCount++;

        for(String flashValue : supportFlashValue) {
            if(!flashValue.equals(currentFlashValue)) {
                addFlashOption(flashValue);
            }
        }
    }

    public void setCurrentFlashValue(String flashValue) {

        String oldFlashValue = (String) mCurrentFlashButton.getTag();

        View selectChildView =  null;
        for(int i = 0; i < getChildCount(); i++) {
            if(flashValue.equals(getChildAt(i).getTag())) {
                selectChildView = getChildAt(i);
            }
        }

        if(null == selectChildView) {
            return;
        }

        selectChildView.setTag(oldFlashValue);
        selectChildView.setBackground(getResources().getDrawable(getFlashImgResId(oldFlashValue)));

        mCurrentFlashButton.setTag(flashValue);
        mCurrentFlashButton.setBackground(getResources().getDrawable(getFlashImgResId(flashValue)));
    }

    private void addFlashOption(String flashValue) {

        int flashImgResId = getFlashImgResId(flashValue);

        if(-1 == flashImgResId) {
            Log.e(TAG, "flash value not supported to be shown!");
            return;
        }

        ImageButton flashOptionButton = new ImageButton(mContext);

        flashOptionButton.setTag(flashValue);
        flashOptionButton.setBackground((getResources().getDrawable(flashImgResId)));
        flashOptionButton.setScaleType(ImageView.ScaleType.FIT_XY);

        flashOptionButton.setLayoutParams(new LayoutParams(new ViewGroup.LayoutParams(
                (int) (getResources().getDimension(R.dimen.top_menu_button_size)),
                (int) (getResources().getDimension(R.dimen.top_menu_button_size)))));

        flashOptionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();

                String selectFlashValue = (String)(v.getTag());

                setCurrentFlashValue(selectFlashValue);

                if(null != mOnFlashMenuOptionSelectListener) {
                    mOnFlashMenuOptionSelectListener.onOptionSelected(selectFlashValue);
                }
            }
        });

        addView(flashOptionButton);
        mButtonsCount++;
    }

    private int getFlashImgResId(String flashValue) {

        int index = 0;

        for(String flashOption : FLASH_VALUE_OPTIONS) {
            if(flashOption.equals(flashValue)) {
                return FLASH_IMAGE_RES_IDS[index];
            }

            index++;
        }

        return -1;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int width = 0;
        int height = 0;

        mMaxButtonWidth = 0;
        mMaxButtonHeight = 0;

        for (int i = 0; i < mButtonsCount; i++) {
            View child = getChildAt(i);

            if (child.getVisibility() == GONE) {
                continue;
            }

            switch (mExpandDirection) {
                case EXPAND_UP:
                case EXPAND_DOWN:
                    mMaxButtonWidth = Math.max(mMaxButtonWidth, child.getMeasuredWidth());
                    height += child.getMeasuredHeight();
                    break;
                case EXPAND_LEFT:
                case EXPAND_RIGHT:
                    width += child.getMeasuredWidth();
                    mMaxButtonHeight = Math.max(mMaxButtonHeight, child.getMeasuredHeight());
                    break;
            }
        }

        if (!expandsHorizontally()) {
            width = mMaxButtonWidth;
        } else {
            height = mMaxButtonHeight;
        }

        switch (mExpandDirection) {
            case EXPAND_UP:
            case EXPAND_DOWN:
                height += mButtonSpacing * (mButtonsCount - 1);
                height = adjustForOvershoot(height);
                break;
            case EXPAND_LEFT:
            case EXPAND_RIGHT:
                width += mButtonSpacing * (mButtonsCount - 1);
                width = adjustForOvershoot(width);
                break;
        }

        setMeasuredDimension(width, height);
    }

    private int adjustForOvershoot(int dimension) {
        return dimension * 12 / 10;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        switch (mExpandDirection) {
            case EXPAND_UP:
            case EXPAND_DOWN:
                boolean expandUp = mExpandDirection == EXPAND_UP;

                if (changed) {
                    mTouchDelegateGroup.clearTouchDelegates();
                }

                int addButtonY = expandUp ? b - t - mCurrentFlashButton.getMeasuredHeight() : 0;
                // Ensure mCurrentFlashButton is centered on the line where the buttons should be
                int buttonsHorizontalCenter = mMaxButtonWidth / 2;

                int addButtonLeft = buttonsHorizontalCenter - mCurrentFlashButton.getMeasuredWidth() / 2;
                mCurrentFlashButton.layout(addButtonLeft, addButtonY, addButtonLeft + mCurrentFlashButton.getMeasuredWidth(), addButtonY + mCurrentFlashButton.getMeasuredHeight());

                int labelsOffset = mMaxButtonWidth / 2;
                int labelsXNearButton = buttonsHorizontalCenter + labelsOffset;

                int nextY = expandUp ?
                        addButtonY - mButtonSpacing :
                        addButtonY + mCurrentFlashButton.getMeasuredHeight() + mButtonSpacing;

                for (int i = mButtonsCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);

                    if (child == mCurrentFlashButton || child.getVisibility() == GONE) continue;

                    int childX = buttonsHorizontalCenter - child.getMeasuredWidth() / 2;
                    int childY = expandUp ? nextY - child.getMeasuredHeight() : nextY;
                    child.layout(childX, childY, childX + child.getMeasuredWidth(), childY + child.getMeasuredHeight());

                    float collapsedTranslation = addButtonY - childY;
                    float expandedTranslation = 0f;

                    child.setTranslationY(isExpanded() ? expandedTranslation : collapsedTranslation);
                    child.setAlpha(isExpanded() ? 1f : 0f);

                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                    params.setAnimationsTarget(child);

                    View label = (View) child.getTag(R.id.fab_label);
                    if (label != null) {
                        int labelXAwayFromButton = labelsXNearButton + label.getMeasuredWidth();

                        int labelTop = childY + (child.getMeasuredHeight() - label.getMeasuredHeight()) / 2;

                        label.layout(labelsXNearButton, labelTop, labelXAwayFromButton, labelTop + label.getMeasuredHeight());

                        Rect touchArea = new Rect(
                                Math.min(childX, labelsXNearButton),
                                childY - mButtonSpacing / 2,
                                Math.max(childX + child.getMeasuredWidth(), labelXAwayFromButton),
                                childY + child.getMeasuredHeight() + mButtonSpacing / 2);
                        mTouchDelegateGroup.addTouchDelegate(new TouchDelegate(touchArea, child));

                        label.setTranslationY(isExpanded() ? expandedTranslation : collapsedTranslation);
                        label.setAlpha(isExpanded() ? 1f : 0f);

                        LayoutParams labelParams = (LayoutParams) label.getLayoutParams();
                        labelParams.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                        labelParams.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                        labelParams.setAnimationsTarget(label);
                    }

                    nextY = expandUp ?
                            childY - mButtonSpacing :
                            childY + child.getMeasuredHeight() + mButtonSpacing;
                }
                break;

            case EXPAND_LEFT:
            case EXPAND_RIGHT:
                boolean expandLeft = mExpandDirection == EXPAND_LEFT;

                int addButtonX = expandLeft ? r - l - mCurrentFlashButton.getMeasuredWidth() : 0;
                // Ensure mCurrentFlashButton is centered on the line where the buttons should be
                int addButtonTop = b - t - mMaxButtonHeight + (mMaxButtonHeight - mCurrentFlashButton.getMeasuredHeight()) / 2;
                mCurrentFlashButton.layout(addButtonX, addButtonTop, addButtonX + mCurrentFlashButton.getMeasuredWidth(), addButtonTop + mCurrentFlashButton.getMeasuredHeight());

                int nextX = expandLeft ?
                        addButtonX - mButtonSpacing :
                        addButtonX + mCurrentFlashButton.getMeasuredWidth() + mButtonSpacing;

                for (int i = mButtonsCount - 1; i >= 0; i--) {
                    final View child = getChildAt(i);

                    if (child == mCurrentFlashButton || child.getVisibility() == GONE) continue;

                    int childX = expandLeft ? nextX - child.getMeasuredWidth() : nextX;
                    int childY = addButtonTop + (mCurrentFlashButton.getMeasuredHeight() - child.getMeasuredHeight()) / 2;
                    child.layout(childX, childY, childX + child.getMeasuredWidth(), childY + child.getMeasuredHeight());

                    float collapsedTranslation = addButtonX - childX;
                    float expandedTranslation = 0f;

                    child.setTranslationX(isExpanded() ? expandedTranslation : collapsedTranslation);
                    child.setAlpha(isExpanded() ? 1f : 0f);

                    LayoutParams params = (LayoutParams) child.getLayoutParams();
                    params.mCollapseDir.setFloatValues(expandedTranslation, collapsedTranslation);
                    params.mExpandDir.setFloatValues(collapsedTranslation, expandedTranslation);
                    params.setAnimationsTarget(child);

                    nextX = expandLeft ?
                            childX - mButtonSpacing :
                            childX + child.getMeasuredWidth() + mButtonSpacing;
                }

                break;
        }
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(super.generateDefaultLayoutParams());
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(super.generateLayoutParams(attrs));
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(super.generateLayoutParams(p));
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return super.checkLayoutParams(p);
    }

    private static Interpolator sExpandInterpolator = new OvershootInterpolator();
    private static Interpolator sCollapseInterpolator = new DecelerateInterpolator(3f);
    private static Interpolator sAlphaExpandInterpolator = new DecelerateInterpolator();

    private class LayoutParams extends ViewGroup.LayoutParams {

        private ObjectAnimator mExpandDir = new ObjectAnimator();
        private ObjectAnimator mExpandAlpha = new ObjectAnimator();
        private ObjectAnimator mCollapseDir = new ObjectAnimator();
        private ObjectAnimator mCollapseAlpha = new ObjectAnimator();
        private boolean animationsSetToPlay;

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);

            mExpandDir.setInterpolator(sExpandInterpolator);
            mExpandAlpha.setInterpolator(sAlphaExpandInterpolator);
            mCollapseDir.setInterpolator(sCollapseInterpolator);
            mCollapseAlpha.setInterpolator(sCollapseInterpolator);

            mCollapseAlpha.setProperty(View.ALPHA);
            mCollapseAlpha.setFloatValues(1f, 0f);

            mExpandAlpha.setProperty(View.ALPHA);
            mExpandAlpha.setFloatValues(0f, 1f);

            switch (mExpandDirection) {
                case EXPAND_UP:
                case EXPAND_DOWN:
                    mCollapseDir.setProperty(View.TRANSLATION_Y);
                    mExpandDir.setProperty(View.TRANSLATION_Y);
                    break;
                case EXPAND_LEFT:
                case EXPAND_RIGHT:
                    mCollapseDir.setProperty(View.TRANSLATION_X);
                    mExpandDir.setProperty(View.TRANSLATION_X);
                    break;
            }
        }

        public void setAnimationsTarget(View view) {
            mCollapseAlpha.setTarget(view);
            mCollapseDir.setTarget(view);
            mExpandAlpha.setTarget(view);
            mExpandDir.setTarget(view);

            // Now that the animations have targets, set them to be played
            if (!animationsSetToPlay) {
                addLayerTypeListener(mExpandDir, view);
                addLayerTypeListener(mCollapseDir, view);

                mCollapseAnimation.play(mCollapseAlpha);
                mCollapseAnimation.play(mCollapseDir);
                mExpandAnimation.play(mExpandAlpha);
                mExpandAnimation.play(mExpandDir);
                animationsSetToPlay = true;
            }
        }

        private void addLayerTypeListener(Animator animator, final View view) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setLayerType(LAYER_TYPE_NONE, null);
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    view.setLayerType(LAYER_TYPE_HARDWARE, null);
                }
            });
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        bringChildToFront(mCurrentFlashButton);
        mButtonsCount = getChildCount();
    }

    public void collapse() {
        collapse(false);
    }

    public void collapseImmediately() {
        collapse(true);
    }

    private void collapse(boolean immediately) {
        if (isExpanded()) {
            mExpanded = false;
            mTouchDelegateGroup.setEnabled(false);
            mCollapseAnimation.setDuration(immediately ? 0 : ANIMATION_DURATION);
            mCollapseAnimation.start();
            mExpandAnimation.cancel();

            if (null != mOnFlashMenuUpdateListener) {
                mOnFlashMenuUpdateListener.onMenuCollapsed();
            }
        }
    }

    public void toggle() {
        if (isExpanded()) {
            collapse();
        } else {
            expand();
        }
    }

    public void expand() {
        if (!isExpanded()) {
            mExpanded = true;
            mTouchDelegateGroup.setEnabled(true);
            mCollapseAnimation.cancel();
            mExpandAnimation.start();

            if (null != mOnFlashMenuUpdateListener) {
                mOnFlashMenuUpdateListener.onMenuExpanded();
            }
        }
    }

    public boolean isExpanded() {
        return mExpanded;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        mCurrentFlashButton.setEnabled(enabled);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.mExpanded = mExpanded;

        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState savedState = (SavedState) state;
            mExpanded = savedState.mExpanded;
            mTouchDelegateGroup.setEnabled(mExpanded);

            super.onRestoreInstanceState(savedState.getSuperState());
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    public static class SavedState extends BaseSavedState {
        public boolean mExpanded;

        public SavedState(Parcelable parcel) {
            super(parcel);
        }

        private SavedState(Parcel in) {
            super(in);
            mExpanded = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mExpanded ? 1 : 0);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {

            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
