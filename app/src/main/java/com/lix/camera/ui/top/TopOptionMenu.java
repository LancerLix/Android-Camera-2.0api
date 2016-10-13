package com.lix.camera.ui.top;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.lix.camera.R;
import com.lix.camera.ui.base.optionmenu.BaseOptionMenu;
import com.lix.camera.ui.top.flashmenu.FlashSettingMenu;
import com.lix.camera.utils.LogUtils;

/**
 * Created by lix on 2016/8/3.
 *
 */
public class TopOptionMenu extends BaseOptionMenu
        implements View.OnClickListener, FlashSettingMenu.OnFlashMenuUpdateListener {

    private final String TAG = TopOptionMenu.class.getSimpleName();

    public interface OnOptionBtnClickListener {
        void onClick(View v);
    }

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_SPECIAL = 1;

    private FlashSettingMenu mFlashSettingMenu;
    private ImageButton mCameraSwitchButton;
    private ImageButton mAdvSettingButton;

    private OnOptionBtnClickListener mOnOptionBtnClickListener;

    public TopOptionMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TopOptionMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TopOptionMenu(Context context) {
        super(context);
    }

    public void setOnOptionBtnClickListener(OnOptionBtnClickListener listener) {
        mOnOptionBtnClickListener = listener;
    }

    public void setOnFlashMenuOptionSelectListener(FlashSettingMenu.OnFlashMenuOptionSelectListener listener) {
        if(null != mFlashSettingMenu) {
            mFlashSettingMenu.setOnFlashMenuOptionSelectListener(listener);
        }
    }

    @SuppressLint("InflateParams")
    @Override
    protected ViewGroup getTypeContentView(int type) {

        if(null == getLayoutInflater()) {
            return null;
        }

        if(TYPE_NORMAL == type) {
            return (ViewGroup) getLayoutInflater().inflate(R.layout.menu_top_option_normal, null);
        }else if(TYPE_SPECIAL == type) {
            return (ViewGroup) getLayoutInflater().inflate(R.layout.menu_top_option_special, null);
        }else {
            LogUtils.e(TAG, "error with Invalid type");
            return null;
        }
    }

    @Override
    protected void initContentView(ViewGroup viewGroup) {

        if(null == viewGroup) {
            return;
        }

        mFlashSettingMenu = (FlashSettingMenu) viewGroup.findViewById(R.id.flash_setting_menu);
        mCameraSwitchButton = (ImageButton) viewGroup.findViewById(R.id.btn_switch_camera);
        mAdvSettingButton = (ImageButton) viewGroup.findViewById(R.id.btn_adv_setting);

        if(null != mCameraSwitchButton) {
            mCameraSwitchButton.setOnClickListener(this);
        }

        if(null != mAdvSettingButton) {
            mAdvSettingButton.setOnClickListener(this);
        }

        if(null != mFlashSettingMenu) {
            mFlashSettingMenu.setOnFlashMenuUpdateListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_switch_camera:
                break;
            case R.id.btn_adv_setting:
                break;
        }

        if(null != mOnOptionBtnClickListener) {
            mOnOptionBtnClickListener.onClick(v);
        }
    }

    @Override
    public void onMenuExpanded() {
        if(null != mCameraSwitchButton) {
            mCameraSwitchButton.setVisibility(INVISIBLE);
        }

        if(null != mAdvSettingButton) {
            mAdvSettingButton.setVisibility(INVISIBLE);
        }
    }

    @Override
    public void onMenuCollapsed() {
        if(null != mCameraSwitchButton) {
            mCameraSwitchButton.setVisibility(VISIBLE);
        }

        if(null != mAdvSettingButton) {
            mAdvSettingButton.setVisibility(VISIBLE);
        }
    }

    @Override
    public void setOptionMenuUIRotation(int uiRotation) {

    }
}
