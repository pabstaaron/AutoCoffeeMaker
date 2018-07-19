package com.example.ndonaldson.beanster;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * This class holds all the network information on a raspBerry PI device on the LAN
 * Created by ndonaldson on 6/13/18.
 */
public class Device implements Parcelable, Serializable{

    private String macAddress;
    private String hostName;
    private String sN;
    private String iP;

    /**
     * Constructor
     * @param macAddress
     * @param hostName
     * @param sN
     * @param iP
     */
    public Device(String macAddress, String hostName, String sN, String iP){
        this.macAddress = macAddress;
        this.hostName = hostName;
        this.sN = sN;
        this.iP = iP;
    }

    /**
     * Make device out of parcel.
    */
    protected Device(Parcel in) {
        macAddress = in.readString();
        hostName = in.readString();
        sN = in.readString();
        iP = in.readString();
    }

    /**
     * Turns device into a parcelable
     */
    public static final Creator<Device> CREATOR = new Creator<Device>() {
        @Override
        public Device createFromParcel(Parcel in) {
            return new Device(in);
        }

        @Override
        public Device[] newArray(int size) {
            return new Device[size];
        }
    };

    public void setMacAddress(String macAddress){
        this.macAddress = macAddress;
    }

    public void setHostName(String hostName){
        this.hostName = hostName;
    }

    public void setsN(String sN){
        this.sN = sN;
    }

    public void setiP(String iP){
        this.iP = iP;
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

    public String getiP() { return iP;}

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @param dest
     * @param flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(macAddress);
        dest.writeString(hostName);
        dest.writeString(sN);
        dest.writeString(iP);
    }

    /**
     * @param in
     */
    public void readFromParcel(Parcel in){
        macAddress = in.readString();
        hostName = in.readString();
        sN = in.readString();
        iP = in.readString();
    }
}