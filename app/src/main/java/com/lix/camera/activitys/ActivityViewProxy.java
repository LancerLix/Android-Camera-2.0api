package com.lix.camera.activitys;

import android.app.AlertDialog;
import android.content.DialogInterface;

import com.lix.camera.ui.function.IActivityView;

/**
 * Created by lix on 2016/9/8.
 *
 * All activity view control
 */
public class ActivityViewProxy implements IActivityView {

    private CameraActivityHolder mCameraActivityHolder;

    public ActivityViewProxy(CameraActivityHolder holder) {
        mCameraActivityHolder = holder;
    }

    public CameraActivityHolder getHolder() {
        return mCameraActivityHolder;
    }

    @Override
    public void destroy() {
        if(null == getHolder() || null == getHolder().getActivity()) {
            return;
        }

        AlertDialog isExit = new AlertDialog.Builder(getHolder().getActivity()).create();
        isExit.setTitle("System");
        isExit.setMessage("Exit?");
        isExit.setButton("Yes", mListenerButton);
        isExit.setButton2("No", mListenerButton);
        isExit.show();
    }

    DialogInterface.OnClickListener mListenerButton = new DialogInterface.OnClickListener() {

        public void onClick(DialogInterface dialog, int which) {

            switch (which) {
                case AlertDialog.BUTTON_POSITIVE:

                    if(null == getHolder() || null == getHolder().getActivity()) {
                        return;
                    }
                    getHolder().getActivity().finish();

                    break;
                case AlertDialog.BUTTON_NEGATIVE:
                    break;
                default:
                    break;

            }
        }
    };
}
