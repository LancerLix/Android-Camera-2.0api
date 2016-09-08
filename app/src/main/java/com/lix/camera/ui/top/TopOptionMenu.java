package com.lix.camera.ui.top;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import com.lix.camera.R;
import com.lix.camera.ui.base.optionmenu.BaseOptionMenu;
import com.lix.camera.utils.LogUtils;

/**
 * Created by lix on 2016/8/3.
 */
public class TopOptionMenu extends BaseOptionMenu {

    private final String TAG = TopOptionMenu.class.getSimpleName();

    public static final int TYPE_NOMAL = 0;
    public static final int TYPE_SPECIAL = 1;

    public TopOptionMenu(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public TopOptionMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TopOptionMenu(Context context) {
        super(context);
    }

    @Override
    protected ViewGroup getTypeContentView(int type) {
        if(null == getLayoutInflater()) {
            return null;
        }

        if(TYPE_NOMAL == type) {
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

    }

    @Override
    public void setOptionMenuUIRotation(int uiRotation) {

    }
}
