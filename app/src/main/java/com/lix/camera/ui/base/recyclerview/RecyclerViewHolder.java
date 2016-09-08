package com.lix.camera.ui.base.recyclerview;

import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;

import com.lix.camera.utils.LogUtils;

public class RecyclerViewHolder extends RecyclerView.ViewHolder {
    
    private final String TAG = RecyclerViewHolder.class.getSimpleName();
    private SparseArray<View> mViewElementSet = null;
    
    public RecyclerViewHolder(View itemView) {
        super(itemView);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends View> T obtainView(int id) {
        if(mViewElementSet == null) {
            mViewElementSet = new SparseArray<View>();
        }
        
        View view = mViewElementSet.get(id);
        if(view != null) {
            return (T) view;
        }
        
        view = itemView.findViewById(id);
        if(view == null) {
            LogUtils.e(TAG, "no view that id is : " + id);
            return null;
        }
        
        mViewElementSet.put(id, view);
        
        return (T) view;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T obtainView(int id, Class<T> viewClass) {
        View view = obtainView(id);
        if(view == null) {
            return null;
        }
        
        return (T) view;
    }
}