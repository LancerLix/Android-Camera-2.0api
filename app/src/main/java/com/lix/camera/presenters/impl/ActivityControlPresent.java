package com.lix.camera.presenters.impl;

import com.lix.camera.presenters.IActivityControlPresenter;
import com.lix.camera.ui.function.IActivityView;

/**
 * Created by lix on 2016/9/8.
 *
 * Implements activity control interface
 */
public class ActivityControlPresent implements IActivityControlPresenter {

    private IActivityView mActivityView;

    public ActivityControlPresent(IActivityView activityView) {

        mActivityView = activityView;
    }

    @Override
    public void exitActivity() {
        if(null != mActivityView) {
            mActivityView.destroy();
        }
    }

}
