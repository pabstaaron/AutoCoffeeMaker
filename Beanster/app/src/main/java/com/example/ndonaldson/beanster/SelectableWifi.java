package com.example.ndonaldson.beanster;

/**
 * Wrapper to identify if viewHolder is selected
 * Created by ndonaldson on 5/21/18.
 */

public class SelectableWifi extends WifiSelectItem {
    private boolean isSelected = false;
    private int mColor;


    public SelectableWifi(WifiSelectItem item, boolean isSelected) {
        super(item.getDeviceID());
        this.isSelected = isSelected;
    }


    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
