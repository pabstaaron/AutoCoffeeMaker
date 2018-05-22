package com.example.ndonaldson.beanster;

/**
 * Created by ndonaldson on 5/21/18.
 */

public class SelectableWifi extends WifiSelection {
    private boolean isSelected = false;


    public SelectableWifi(WifiSelection item, boolean isSelected) {
        super.WifiSelection(item.getDeviceID());
        this.isSelected = isSelected;
    }


    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
