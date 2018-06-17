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
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    private String lastID;
    private ArrayList<Device> devicesInRange;
    private ConnectStatus connectStatus = ConnectStatus.CONNECT_TO_LAST;
    private String connectedDevice = "";
    private HttpURLConnection client;
    private URL url;
    private String networkIP;
    private Context context;
    private int searchingCount;
    private WifiManager wifiManager;
    private boolean isRunning;


    /**
     * @param context
     * Constructor for WifiRunner
     */
    public WifiRunner(Context context){
    try {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "deviceIds.txt");
        if(file.exists()) {
            Scanner s = new Scanner(file);
            while (s.hasNext()) {
                lastID = s.next();
            }
            s.close();
        }
        devicesInRange = new ArrayList<>();
        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        String networkIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Log.i("WifiRunner", "Router ip address is: " + networkIP);
        searchingCount = 0;
        this.context = context;
        isRunning = false;
        createArpMap();


        LocalBroadcastManager.getInstance(context).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_DATA_IN"));

    }
    catch(Exception e){
        Log.i("WifiRunner", e.getLocalizedMessage());
    }

    }

    /**
     * Check wifi connection every second to make sure that we are still connected.
     * If no connection kick out to main screen.
     * Keep checking response from raspberry PI.
     */
    @Override
    public void run() {
        if(!isRunning) {
            isRunning = true;
            try {
                if (connectStatus == ConnectStatus.CONNECTED) {
                    Log.i("WifiRunner", "CONNECTED!");
                    client = (HttpURLConnection) url.openConnection();
                    if (devicesInRange != null) devicesInRange.clear();
                    client.setRequestMethod("GET");
                    //client.setRequestProperty("serial", deviceIDs.get(searchingCount));
                    int responseCode = 404;
                    try {
                        responseCode = client.getResponseCode();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.i("WifiRunner", e.getLocalizedMessage());
                    }
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        connectedDevice = "";
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                    }
                    client.disconnect();
                }
                //Maybe add a timer here
                else if (connectStatus == ConnectStatus.WAITING_FOR_RESPONSE) {
                    Log.i("WifiRunner", "WAITING FOR RESPONSE!");
                    client = (HttpURLConnection) url.openConnection();
                    if (!connectedDevice.isEmpty()) {
                        client.setRequestMethod("GET");
                        client.setConnectTimeout(1000);
                        //client.setRequestProperty("serial", connectedDevice);
                        Log.i("WifiRunner", client.getURL().toString());
                        int responseCode = 404;
                        try {
                            responseCode = client.getResponseCode();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("WifiRunner", e.getLocalizedMessage());
                        }
                        Log.i("WifiRunner", "responseCode:" + responseCode);
                        if (responseCode != HttpURLConnection.HTTP_OK) {
                            Log.i("WifiRunner", "Failed to connect");
                            connectedDevice = "";
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                        } else {
                            Log.i("WifiRunner", "Connected");
                            FileWriter writer = new FileWriter("deviceIds.txt");
                            writer.write(connectedDevice);
                            writer.close();
                            connectStatus = ConnectStatus.CONNECTED;
                            sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                        }
                    } else {
                        connectedDevice = "";
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                    }
                    client.disconnect();
                } else if (connectStatus == ConnectStatus.WAITING_FOR_USER) {
                    Log.i("WifiRunner", "WAITING FOR USER!");
                    if (devicesInRange != null) devicesInRange.clear();
                } else if (connectStatus == ConnectStatus.CONNECT_TO_LAST) {
                    Log.i("WifiRunner", "CONNECTING TO LAST!");
                    if ((lastID != null && !lastID.isEmpty()) && client != null) {
                        url = new URL("http://" + networkIP + "/connected/" + lastID);
                        client = (HttpURLConnection) url.openConnection();
                        client.setRequestMethod("GET");
                        client.setConnectTimeout(1000);
                        int responseCode = client.getResponseCode();
                        Log.i("WifiRunner", "responseCode is: " + responseCode);
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            connectStatus = ConnectStatus.CONNECTED;
                            connectedDevice = lastID;
                            sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                        } else {
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            Log.i("WifiRunner", "SENDING WAITING_FOR_USER INTENT!");
                            sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                        }
                        client.disconnect();
                    } else {
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        Log.i("WifiRunner", "SENDING WAITING_FOR_USER INTENT!");
                        sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                    }
                }
            } catch (Exception e) {
                Log.i("WifiRunner", e.getLocalizedMessage());
                e.printStackTrace();
            }

            isRunning = false;
        }
    }

    /**
     * Connection status definitions
     */
    public enum ConnectStatus{
        CONNECTED,
        WAITING_FOR_USER,
        CONNECT_TO_LAST,
        WAITING_FOR_RESPONSE,
        UNKNOWN
    }

    /**
     * Type of Broadcast to Send
     */
    public enum MessageType{
        DATA,
        CONNECT_STATUS
    }

    /**
     * Send out connection status change to any app with proper receiver
     * @param data
     */
    private void sendIntent(Object data, MessageType type){
        Intent intent = new Intent();
        switch(type){
            case CONNECT_STATUS:{
                try{
                    String connectStatus = (String) data;
                    intent.putExtra("status",connectStatus);
                }
                catch(Exception e){
                    Log.i("WifiRunner",e.getLocalizedMessage());
                }
            }
            case DATA:{
                try{
                    ArrayList deviceIDs = (ArrayList) data;
                    intent.putParcelableArrayListExtra("deviceIds", deviceIDs);
                }
                catch(Exception e){
                    Log.i("WifiRunner",e.getLocalizedMessage());
                }
            }
        }

        intent.setAction("com.android.activity.WIFI_DATA_OUT");
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
            if(intent.hasExtra("deviceID")){
                String deviceId = intent.getStringExtra("deviceID");
                Log.d("WifiRunner", "wifiStatusReceiver got deviceID message: " + deviceId);
                connectedDevice = deviceId;
            }
        }
    };

    /**
     * Extract and save ip and corresponding MAC address from arp table in HashMap
     */
    public void createArpMap() throws IOException {
        Map<String, String> checkMapARP = new HashMap<>();
        BufferedReader localBufferdReader = new BufferedReader(new FileReader(new File("/proc/net/arp")));
        String line;

        while ((line = localBufferdReader.readLine()) != null) {
            String[] ipmac = line.split("[ ]+");
            for(String s: ipmac){
                Log.i("WifiRunner", s);
            }
            if (!ipmac[0].matches("IP")) {
                String ip = ipmac[0];
                String mac = ipmac[3];
                if (!checkMapARP.containsKey(mac) && mac.startsWith("b8:27:eb")) {
                    checkMapARP.put(mac, ip);
                    devicesInRange.add(new Device(mac, "", ""));
                    Log.i("WifiRunner", "Adding RaspberryPi: " + mac);
                }
                Log.i("WifiRunner", "\n\n\n");
            }
        }
    }
}
