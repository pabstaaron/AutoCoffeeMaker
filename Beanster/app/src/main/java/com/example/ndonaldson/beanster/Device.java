package com.example.ndonaldson.beanster;

/**
 * Created by ndonaldson on 6/13/18.
 */

public class Device {

    private String macAddress;
    private String hostName;
    private String sN;

    public Device(String macAddress, String hostName, String sN){
        this.macAddress = macAddress;
        this.hostName = hostName;
        this.sN = sN;
    }

    public void setMacAddress(String macAddress){
        this.macAddress = macAddress;
    }

    public void setHostName(String hostName){
        this.hostName = hostName;
    }

    public void setsN(String sN){
        this.sN = sN;
    }

    public String getHostName() {
        return hostName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getsN() {
        return sN;
    }
}
