package com.example.ndonaldson.beanster;

/**
 * Created by ndonaldson on 5/21/18.
 */

public class    WifiSelectItem {

    private String deviceID;

    public WifiSelectItem(String deviceID){
        this.deviceID = deviceID;
    }

    public void setDeviceID(String deviceID){
        this.deviceID = deviceID;
    }

    public String getDeviceID(){
        return deviceID;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        WifiSelectItem itemCompare = (WifiSelectItem) obj;
        if(itemCompare.getDeviceID().equals(this.getDeviceID()))
            return true;

        return false;
    }
}
