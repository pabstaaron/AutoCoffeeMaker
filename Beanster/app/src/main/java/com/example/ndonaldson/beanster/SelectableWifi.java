package com.example.ndonaldson.beanster;

/**
 * Wrapper to identify if viewHolder is selected
 * Created by ndonaldson on 5/21/18.
 */

public class SelectableWifi extends WifiSelectItem {
    private boolean isSelected = false;
    private int mColor;


    public SelectableWifi(WifiSelectItem item, boolean isSelected, int color) {
        super(item.getDeviceID());
        this.isSelected = isSelected;
        this.mColor = color;
    }


    public boolean isSelected() {
        return isSelected;
    }

    public int getColor(){ return mColor;}

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
