package com.example.ndonaldson.beanster.wifi;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.ndonaldson.beanster.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView that holds WifiSelectItems in WifiViewHolders
 * Created by ndonaldson on 5/21/18.
 */

public class WifiAdapter extends RecyclerView.Adapter implements WifiViewHolder.OnItemSelectedListener {

    private final List<SelectableWifi> mValues;
    private boolean isMultiSelectionEnabled = false;
    private boolean creating = true;
    private String deviceSelected = null;
    private WifiViewHolder.OnItemSelectedListener listener;



    public WifiAdapter(WifiViewHolder.OnItemSelectedListener listener,
                       List<WifiSelectItem> items, boolean isMultiSelectionEnabled, String deviceSelectedName) {
        this.listener = listener;
        this.isMultiSelectionEnabled = isMultiSelectionEnabled;

        mValues = new ArrayList<>();
        for (WifiSelectItem item : items) {
            if(deviceSelectedName != null && !deviceSelectedName.isEmpty() && item.getDeviceID().equals(deviceSelectedName)){
                Log.i("WifiAdapter", "deviceSelectedName: " + deviceSelectedName);
                mValues.add(new SelectableWifi(item, true));
                deviceSelected = deviceSelectedName;
            } else {
                mValues.add(new SelectableWifi(item, false));
            }
        }
    }

    @Override
    public WifiViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.checkedtextview, parent, false);
        return new WifiViewHolder(itemView, this);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        WifiViewHolder holder = (WifiViewHolder) viewHolder;
        SelectableWifi selectableItem = mValues.get(position);
        String name = selectableItem.getDeviceID();
        holder.textView.setText(name);
        if (isMultiSelectionEnabled) {
            TypedValue value = new TypedValue();
            holder.textView.getContext().getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorMultiple, value, true);
            int checkMarkDrawableResId = value.resourceId;
            holder.textView.setCheckMarkDrawable(checkMarkDrawableResId);
        } else {
            if(creating && deviceSelected != null && deviceSelected.equals(selectableItem.getDeviceID())){
                holder.textView.setChecked(true);
                creating = false;
            }

            TypedValue value = new TypedValue();
            holder.textView.getContext().getTheme().resolveAttribute(android.R.attr.listChoiceIndicatorSingle, value, true);
            int checkMarkDrawableResId = value.resourceId;
            holder.textView.setCheckMarkDrawable(checkMarkDrawableResId);
        }

        holder.mItem = selectableItem;

        if(selectableItem.isSelected()){
            holder.setColor(Color.LTGRAY);
            holder.textView.setChecked(true);
        }
        else{
            holder.setColor(Color.DKGRAY);
            holder.textView.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public List<SelectableWifi> getSelectedItems() {

        List<SelectableWifi> selectedItems = new ArrayList<>();
        for (SelectableWifi item : mValues) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }

    @Override
    public int getItemViewType(int position) {
        if(isMultiSelectionEnabled){
            return WifiViewHolder.MULTI_SELECTION;
        }
        else{
            return WifiViewHolder.SINGLE_SELECTION;
        }
    }

    @Override
    public void onItemSelected(SelectableWifi item) {
        if (!isMultiSelectionEnabled) {
            for (SelectableWifi selectableItem : mValues) {
                if (!selectableItem.equals(item)
                        && selectableItem.isSelected()) {
                    selectableItem.setSelected(false);
                } else if (selectableItem.equals(item)
                        && item.isSelected()) {
                    selectableItem.setSelected(true);
                }
            }
            notifyDataSetChanged();
        }
        listener.onItemSelected(item);
    }
}