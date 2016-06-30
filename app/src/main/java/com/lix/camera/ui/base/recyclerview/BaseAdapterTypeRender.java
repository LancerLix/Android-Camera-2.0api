package com.lix.camera.ui.base.recyclerview;

import android.view.View;

public abstract class BaseAdapterTypeRender<T extends BaseRecyclerViewItemWithType> {
    
    private RecyclerViewHolder mViewHolder;
    private BaseRecyclerViewAdapter<T> mBaseAdapter;
    
    public BaseAdapterTypeRender(View extraView, BaseRecyclerViewAdapter<T> baseAdapter) {
        mViewHolder = new RecyclerViewHolder(extraView);
        mBaseAdapter = baseAdapter;
    }
    
    public RecyclerViewHolder getReusableComponent() {
        return mViewHolder;
    }
    
    public BaseRecyclerViewAdapter<T> getAdapter() {
        return mBaseAdapter;
    }
    
    protected abstract void fitEvents(int position);
    
    protected abstract void fitDatas(int position);
}