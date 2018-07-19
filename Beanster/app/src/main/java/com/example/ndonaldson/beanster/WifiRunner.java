package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static android.content.Context.WIFI_SERVICE;


/**
 *@author Nathan Donaldson
 * This class keeps track of the state of communication with a device.
 */
public class    WifiRunner implements Runnable {

    private Device lastDevice;
    private ArrayList<Device> savedDevices;
    private ArrayList<Device> devicesInRange;
    private ArrayList<String[]> arpList;
    private ConnectStatus connectStatus = ConnectStatus.UNKNOWN;
    private HttpURLConnection client;
    private URL url;
    private Context context;
    private WifiManager wifiManager;
    private ConnectivityManager mConnManager;
    private NetworkInfo mWifi;
    private boolean isRunning;
    private boolean firstConnect;
    private String myIP;
    private static final String  DEVICES_LOCATION = Environment.getExternalStorageDirectory().getAbsolutePath() + "/deviceIds.txt";


    /**
     * CONSTRUCTOR
     * Retreive list of devices saved on this device so we can have a cache of passwords for each device as well as other info.
     * Also get current LAN information
     * @param context
     */
    public WifiRunner(Context context){
    try {
        arpList = new ArrayList<>();
        firstConnect = true;
        isRunning = false;
        this.context = context;
        try {
            File file = new File(DEVICES_LOCATION);
            if(!file.exists()){
                file.createNewFile();
            }
            else {
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream is = new ObjectInputStream(fis);
                savedDevices = (ArrayList<Device>) is.readObject();
                if (savedDevices != null || !savedDevices.isEmpty()) {
                    lastDevice = savedDevices.get(savedDevices.size() - 1);
                    Log.i("WifiRunner", "lastDevice is: " + lastDevice.getMacAddress() + " with sN: " + lastDevice.getsN());
                }
                is.close();
                fis.close();
            }
        }
        catch(EOFException e){
            e.printStackTrace();
            Log.i("WifiRunner", "" + e.getLocalizedMessage());
        }
        devicesInRange = new ArrayList<>();
        mConnManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifi = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if(!mWifi.isConnected()){
            connectStatus = ConnectStatus.NO_WIFI;
            sendIntent("status");
        }
        else{
            wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
            myIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
            Log.i("WifiRunner", "My IP Address is: " + myIP);
            connectStatus = ConnectStatus.SEARCHING;
            sendIntent("status");
        }

        LocalBroadcastManager.getInstance(context).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_DATA_IN"));

    }
    catch(Exception e){
        e.printStackTrace();
        Log.i("WifiRunner", e.getLocalizedMessage());
    }

    }

    /**
     * - This checks to see if we are STILL connected to a device or wifi.
     * - If not connected to wifi, it kicks user to main screen and waits for wifi connection.
     * - If not connected to a device, it kicks the user to the device selection activity if they have gotten to that point,
     * otherwise it waits for user input on main screen.
     * - Tries to connect to a device once receiving broadcast from other activities.
     */
    @Override
    public void run() {
        if(!isRunning) {
            mWifi = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            isRunning = true;
            try {
                if(!mWifi.isConnected() && connectStatus != ConnectStatus.NO_WIFI){
                    connectStatus = ConnectStatus.NO_WIFI;
                    sendIntent("status");
                }
                else if (connectStatus == ConnectStatus.CONNECTED) {
                    Log.i("WifiRunner", "CONNECTED!");
                    client = (HttpURLConnection) url.openConnection();
                    client.setRequestMethod("GET");
                    //client.setRequestProperty("serial", deviceIDs.get(searchingCount));
                    int responseCode = 404;
                    try {
                        responseCode = client.getResponseCode();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("WifiRunner", e.getLocalizedMessage());
                        sendIntent("failure");
                    }
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        if(responseCode == HttpURLConnection.HTTP_BAD_REQUEST){
                            sendIntent("badRequest");
                        }
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent("status");
                    }
                    client.disconnect();
                }
                else if (connectStatus == ConnectStatus.SEARCHING){
                    Log.i("WifiRunner", "SEARCHING!");
                    createArpList();
                    pingDevices();
                    findDevices();
                    if(firstConnect) {
                        connectStatus = ConnectStatus.CONNECT_TO_LAST;
                        sendIntent("status");
                    }
                    else {
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent("data");
                        sendIntent("status");
                    }
                }
                else if (connectStatus == ConnectStatus.WAITING_FOR_RESPONSE || connectStatus == ConnectStatus.CONNECT_TO_LAST) {
                    Log.i("WifiRunner", "WAITING FOR RESPONSE!");
                    if (lastDevice != null && !lastDevice.getiP().isEmpty()) {
                        url = new URL("http://" + lastDevice.getiP() + ":5000/connected/" + lastDevice.getsN());
                        client = (HttpURLConnection) url.openConnection();
                        client.setRequestMethod("GET");
                        client.setConnectTimeout(1000);

                        Log.i("WifiRunner", client.getURL().toString());
                        int responseCode = 404;
                        try {
                            responseCode = client.getResponseCode();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("WifiRunner", e.getLocalizedMessage());
                            sendIntent("failure");
                            lastDevice = null;
                        }
                        Log.i("WifiRunner", "responseCode:" + responseCode);
                        if (responseCode != HttpURLConnection.HTTP_OK) {
                            if(responseCode == HttpURLConnection.HTTP_BAD_REQUEST){
                               if(connectStatus != ConnectStatus.CONNECT_TO_LAST) sendIntent("badRequest");
                            }
                            Log.i("WifiRunner", "Failed to connect");
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            lastDevice = null;
                            sendIntent("status");
                        }
                        else {
                            Log.i("WifiRunner", "Connected");
                            boolean exists = false;
                            boolean update = false;
                            if(savedDevices != null) {
                                for (Device d : savedDevices) {
                                    if (d.getMacAddress().equals(lastDevice.getMacAddress()))
                                        if(d.getsN() != lastDevice.getsN()) {
                                            d.setsN(lastDevice.getsN());
                                            update = true;
                                        }
                                        exists = true;
                                }
                            }

                            if(!exists || update) {
                                if (savedDevices == null) savedDevices = new ArrayList<>();
                                if(!update) savedDevices.add(lastDevice);
                                try {
                                    File file = new File(DEVICES_LOCATION);
                                    FileOutputStream fos = new FileOutputStream(file, false);
                                    ObjectOutputStream os = new ObjectOutputStream(fos);
                                    os.writeObject(savedDevices);
                                    os.flush();
                                    fos.getFD().sync();
                                    os.close();
                                } catch(EOFException e){
                                    e.printStackTrace();
                                    Log.i("WifiRunner", "" + e.getLocalizedMessage());
                                }

                            }
                            connectStatus = ConnectStatus.CONNECTED;
                            if(firstConnect) sendIntent("lastDevice");
                            sendIntent("status");
                        }
                    } else {
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent("status");
                    }
                    firstConnect = false;
                    client.disconnect();
                } else if (connectStatus == ConnectStatus.WAITING_FOR_USER) {
                    Log.i("WifiRunner", "WAITING FOR USER!");
                }
                else if(connectStatus == ConnectStatus.NO_WIFI){
                    Log.i("WifiRunner", "NO WIFI!");
                    if(mWifi.isConnected()){
                        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
                        myIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
                        connectStatus = ConnectStatus.SEARCHING;
                        sendIntent("status");
                    }
                }
            } catch (Exception e) {
                Log.i("WifiRunner", e.getLocalizedMessage());
                e.printStackTrace();
            }
            isRunning = false;
            Log.i("WifiRunner", String.format("SAVED DEVICES SIZE: %d, DEVICES IN RANGE SIZE: %d", savedDevices.size(), devicesInRange.size()));
        }
    }

    /**
     * Connection status definitions
     */
    public enum ConnectStatus{
        CONNECTED,
        WAITING_FOR_USER,
        SEARCHING,
        CONNECT_TO_LAST,
        WAITING_FOR_RESPONSE,
        NO_WIFI,
        UNKNOWN
    }


    /**
     * Send out connection status change, deviceId's, failures to connect, or bad requests
     * to other activities.
     * @param type
     */
    private void sendIntent(String type){
        Intent intent = new Intent();

            if (type.equals("status")){
                try{
                    intent.putExtra("status",connectStatus.name());
                }
                catch(Exception e){
                    Log.i("WifiRunner",e.getLocalizedMessage());
                }
            }
            else if (type.equals("data") || type.equals("sendDevices")){
                try{
                    for(Device d: devicesInRange){
                        for(Device d2: savedDevices){
                            if(d.getMacAddress().equals(d2.getMacAddress())){
                                d.setsN(d2.getsN());
                                d.setHostName(d2.getHostName());
                                d.setiP(d2.getiP());
                            }
                        }
                    }
                    intent.putParcelableArrayListExtra("deviceIds", devicesInRange);
                }
                catch(Exception e){
                    Log.i("WifiRunner",e.getLocalizedMessage());
                }
            }
            else if(type.equals("lastDevice")){
                intent.putExtra("lastDevice", (Parcelable) lastDevice);
            }
            else if (type.equals("failure")){
                intent.putExtra("Failure", "");
            }
            else if (type.equals("badRequest")){
                intent.putExtra("badRequest", "");
            }

        if(!intent.getExtras().isEmpty()) {
            intent.setAction("com.android.activity.WIFI_DATA_OUT");
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }
    }

    /**
     * Receive info on selected or entered deviceID from user or wifi state change
     */
    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra("status")){
                String status = intent.getStringExtra("status");
                Log.d("WifiRunner", "wifiStatusReceiver got status message: " + status);
                connectStatus = ConnectStatus.valueOf(status);
            }
            else if(intent.hasExtra("deviceID")){
                Device device = intent.getParcelableExtra("deviceID");
                Log.d("WifiRunner", "wifiStatusReceiver got deviceID message: " + device);
                lastDevice = device;
            }
            else if(intent.hasExtra("sendDevices")){
                Log.d("WifiRunner", "wifiStatusReceiver got message SEND DEVICES");
                sendIntent("sendDevices");
            }
        }
    };

    /**
     * Extract and save ip and corresponding MAC address from arp table in HashMap
     */
    private void findDevices() throws IOException {
        devicesInRange.clear();

        for(String[] s: arpList) {
            if (!s[0].matches("IP")) {
                String ip = s[0];
                String mac = s[3];
                if (mac.startsWith("b8:27:eb")) {
                    devicesInRange.add(new Device(mac, "", "", ip));
                    Log.i("WifiRunner", "Adding RaspberryPi: " + mac);
                }
            }
        }
    }

    private void createArpList() {
        arpList.clear();

        try {
            BufferedReader localBufferdReader = new BufferedReader(new FileReader(new File("/proc/net/arp")));

            String line;

            while ((line = localBufferdReader.readLine()) != null) {
                String[] ipmac = line.split("[ ]+");

                for (String s : ipmac) {
                    Log.i("WifiRunner", "ARP INFO! " + s + "\n");
                }

                Log.i("WifiRunner", "\n\n\n");
                arpList.add(ipmac);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
            Log.i("WifiRunner", e.getLocalizedMessage());
        }
    }

    /**
     * Ping all devices on LAN
     * The reason for this is to refresh the /proc/net/arp/ table on the device.
     */
    private void pingDevices(){
        try {
            NetworkInterface iFace = NetworkInterface
                    .getByInetAddress(InetAddress.getByName(myIP));
            boolean foundIP = false;

            for (int i = 0; i <= 255; i++) {
                // build the next IP address
                String addr = myIP;
                addr = addr.substring(0, addr.lastIndexOf('.') + 1) + i;


                for(String[] s: arpList) {
                    if (!s[0].matches("IP")) {
                        String ip = s[0];
                        if(ip.equals(addr)){
                            foundIP = true;
                            break;
                        }
                    }
                }

                if(foundIP) {
                    foundIP = false;
                    continue;
                }

                InetAddress pingAddr = InetAddress.getByName(addr);

                // 50ms Timeout for the "ping"
                if (pingAddr.isReachable(iFace, 200, 50)) {
                    Log.i("WifiRunner", "PINGING: " + pingAddr.getHostAddress());
                }
            }
        } catch (UnknownHostException ex) {
        } catch (IOException ex) {
        }
    }
}
