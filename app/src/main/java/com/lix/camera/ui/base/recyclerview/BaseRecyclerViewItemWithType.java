package com.lix.camera.ui.base.recyclerview;

import java.util.HashMap;

public class BaseRecyclerViewItemWithType {
    
    public static final int TYPE_UNKNOWN = 0;
    
    private int mViewType;
    private HashMap<String, Object> mViewData;
    
    public BaseRecyclerViewItemWithType(int viewType) {
        mViewType = viewType;
        mViewData = new HashMap<String, Object>();
    }
    
    public int getViewType() {
        return mViewType;
    }
    
    public void putData(String key, Object value) {
        if(mViewData == null) {
            return;
        }
        
        mViewData.put(key, value);
    }
    
    public Object getData(String key) {
        if(mViewData == null) {
            return null;
        }
        
        return mViewData.get(key);
    }
}