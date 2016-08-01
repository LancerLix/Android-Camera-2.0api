package com.lix.camera.ui.base.recyclerview;

import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lix.camera.R;

public abstract class BaseRecyclerViewAdapter<T extends BaseRecyclerViewItemWithType>
        extends RecyclerView.Adapter<RecyclerViewHolder> {
    
    private final int RENDER_VIEW_BIND_KEY = R.id.recycler_view_render_tag;
    
    private List<T> mShowDataList;
    private LayoutInflater mLayoutInflater;
    
    protected abstract BaseAdapterTypeRender<T> getAdapterTypeRender(int viewType);
    
    public BaseRecyclerViewAdapter(Context context, List<T> list) {
        mLayoutInflater = LayoutInflater.from(context);
        mShowDataList = list;
    }
    
    public LayoutInflater getLayoutInflater() {
        return mLayoutInflater;
    }
    
    public List<T> getDataList() {
        return mShowDataList;
    }
    
    /**
     * Obtain a child view in this RecyclerView by the given position.
     * 
     * @param position the index where the child view you attempt to obtain.
     * @return a child view in this RecyclerView, maybe null.
     *
     */
    public View getTypeView(int position) {
        BaseAdapterTypeRender<T> typeRender = getAdapterTypeRender(getItemViewType(position));
        
        if(typeRender == null) {
            return null;
        }
        
        RecyclerViewHolder viewHolder = typeRender.getReusableComponent();
        
        if(viewHolder == null) {
            return null;
        }
        
        return viewHolder.itemView;
    }
    
    /**
     * Use the given show data to insert a child view in this RecyclerView at the given position.
     * 
     * @param viewItem the dataItem that the inserted view required to show.
     * @param position the index where the child view you attempt to insert.
     *
     */
    public void insertChildView(T viewItem, int position) {
        if(null == mShowDataList) {
            return;
        }
        
        if(position > mShowDataList.size()) {
            return;
        }
        
        if(null == viewItem) {
            return;
        }
        
        this.notifyItemInserted(position);
        mShowDataList.add(position, viewItem);
        this.notifyItemRangeChanged(position, this.getItemCount());
    }
    
    /**
     * Remove a child view in this RecyclerView at the given position.
     * 
     * @param position the index where the child view you attempt to remove.
     *
     */
    public void RemoveChildView(int position) {
        if(null == mShowDataList) {
            return;
        }
        
        if(position > mShowDataList.size()) {
            return;
        }
        
        this.notifyItemRemoved(position);
        mShowDataList.remove(position);
        this.notifyItemRangeChanged(position, this.getItemCount());
    }
    
    /**
     * Use the given show data to update a child view in this RecyclerView at the given position.
     * 
     * @param viewItem the dataItem that the inserted view required to show.
     * @param position the index where the child view you attempt to update.
     *
     */
    public void updateChildView(T viewItem, int position) {
        if(null == mShowDataList) {
            return;
        }
        
        if(position > mShowDataList.size()) {
            return;
        }
        
        if(null == viewItem) {
            return;
        }
        
        mShowDataList.set(position, viewItem);
        this.notifyItemChanged(position);
    }
    
    /**
     * Use the given show data list to update all child view in this RecyclerView.
     * 
     * @param dataList the list which contains the data for all child view to show,
     * null means invalidate current RecyclerView.
     *
     */
    public void updateAll(List<T> dataList) {
        if(null == mShowDataList) {
            return;
        }
        
        if(null == dataList) {
            this.notifyDataSetChanged();
            return;
        }
        
        mShowDataList.clear();
        
        for(Iterator<T> it = dataList.iterator();it.hasNext();) {
            mShowDataList.add(it.next());
        }
        
        this.notifyDataSetChanged();
    }
 
    @Override
    public int getItemCount() {
        if(mShowDataList == null) {
            return 0;
        }
        
        return mShowDataList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(mShowDataList == null) {
            return BaseRecyclerViewItemWithType.TYPE_UNKNOWN;
        }
        
        if(position > mShowDataList.size()) {
            return BaseRecyclerViewItemWithType.TYPE_UNKNOWN;
        }
        
        return mShowDataList.get(position).getViewType();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(RecyclerViewHolder viewHolder, int position) {
        BaseAdapterTypeRender<T> render = (BaseAdapterTypeRender<T>) viewHolder.itemView.getTag(RENDER_VIEW_BIND_KEY);
        render.fitDatas(position);
        render.fitEvents(position);
    }

    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        BaseAdapterTypeRender<T> render = getAdapterTypeRender(viewType);
        RecyclerViewHolder holder = render.getReusableComponent();
        holder.itemView.setTag(RENDER_VIEW_BIND_KEY, render);
        return holder;
    }
}