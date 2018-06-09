package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import static android.content.Context.WIFI_SERVICE;


/**
 *@author Nathan Donaldson
 * This class keeps track of the state of communication with a device.
 */
public class    WifiRunner implements Runnable {

    private ArrayList<String> deviceIDs;
    private ArrayList<String> devicesInRange;
    private ConnectStatus connectStatus = ConnectStatus.CONNECT_TO_LAST;
    private String connectedDevice = "";
    private HttpURLConnection client;
    private URL url;
    private Context context;
    private int searchingCount;
    private WifiManager wifiManager;


    /**
     * @param context
     * Constructor for WifiRunner
     */
    public WifiRunner(Context context){
    try {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "deviceIds.txt");
        if(file.exists()) {
            Scanner s = new Scanner(file);
            deviceIDs = new ArrayList<>();
            devicesInRange = new ArrayList<>();
            while (s.hasNext()) {
                deviceIDs.add(s.next());
            }
            s.close();
        }

        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Log.i("WifiRunner", "Ip address is: " + ip);
        url = new URL("http://" + ip + "/connected/");
        searchingCount = 0;
        this.context = context;

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
        try {
            if(connectStatus == ConnectStatus.CONNECTED){
                Log.i("WifiRunner", "CONNECTED!");
                if(devicesInRange != null) devicesInRange.clear();
                client.setRequestMethod("GET");
                client.setRequestProperty("serial", deviceIDs.get(searchingCount));
                int responseCode = client.getResponseCode();
                if(responseCode != HttpURLConnection.HTTP_OK){
                    connectedDevice = "";
                    connectStatus = ConnectStatus.WAITING_FOR_USER;
                    sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                }
            }
            else if(connectStatus == ConnectStatus.SEARCHING) {
                Log.i("WifiRunner", "SEARCHING!");
                client = (HttpURLConnection) url.openConnection();
                client.setRequestMethod("GET");
                if(deviceIDs != null) {
                    client.setRequestProperty("serial", deviceIDs.get(searchingCount));
                    int responseCode = client.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        if (devicesInRange.contains(deviceIDs.get(searchingCount))) {
                            int positionToRemove = 0;
                            for (int i = 0; i <= devicesInRange.size() - 1; i++) {
                                if (deviceIDs.get(searchingCount).equals(devicesInRange.get(i)))
                                    positionToRemove = i;
                            }
                            devicesInRange.remove(positionToRemove);
                            sendIntent(devicesInRange, MessageType.DATA);
                        }
                    }
                    if(!devicesInRange.contains(deviceIDs.get(searchingCount))){
                        devicesInRange.add(deviceIDs.get(searchingCount));
                        sendIntent(devicesInRange, MessageType.DATA);
                    }
                    if(searchingCount == deviceIDs.size()-1) searchingCount = 0;
                }
                client.disconnect();
            }
            //Maybe add a timer here
            else if(connectStatus == ConnectStatus.WAITING_FOR_RESPONSE){
                Log.i("WifiRunner", "WAITING FOR RESPONSE!");
                client = (HttpURLConnection) url.openConnection();
                if(!connectedDevice.isEmpty()){
                    client.setRequestMethod("GET");
                    client.setRequestProperty("serial", connectedDevice);
                    int responseCode = -1;
                    try {
                        responseCode = client.getResponseCode();
                    }
                    catch(Exception e){
                        e.printStackTrace();
                        Log.i("WifiRunner", e.getLocalizedMessage());
                    }
                    if(responseCode != HttpURLConnection.HTTP_OK){
                        connectedDevice = "";
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                    }
                    else{
                        if(!deviceIDs.contains(connectedDevice)){
                            deviceIDs.add(0,connectedDevice);
                            FileWriter writer = new FileWriter("deviceIs.txt");
                            for(String str: deviceIDs) {
                                writer.write(str);
                            }
                            writer.close();
                        }
                        connectStatus = ConnectStatus.CONNECTED;
                        sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                    }
                }
                else{
                    connectedDevice = "";
                    connectStatus = ConnectStatus.WAITING_FOR_USER;
                    sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                }
                client.disconnect();
            }
            else if(connectStatus == ConnectStatus.WAITING_FOR_USER){
                Log.i("WifiRunner", "WAITING FOR USER!");
                if(devicesInRange != null) devicesInRange.clear();
            }
            else if(connectStatus == ConnectStatus.CONNECT_TO_LAST){
                client = (HttpURLConnection) url.openConnection();
                Log.i("WifiRunner", "CONNECTING TO LAST!");
                if(devicesInRange != null) devicesInRange.clear();
                if(deviceIDs != null && deviceIDs.size() > 0 && client != null) {
                    client.setRequestMethod("GET");
                    client.setRequestProperty("serial", deviceIDs.get(0));
                    client.setConnectTimeout(900);
                    int responseCode = client.getResponseCode();
                    Log.i("WifiRunner", "responseCode is: " + responseCode);
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        connectStatus = ConnectStatus.CONNECTED;
                        connectedDevice = deviceIDs.get(0);
                        sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                    }
                    else{
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        Log.i("WifiRunner", "SENDING WAITING_FOR_USER INTENT!");
                        sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                    }
                }
                else{
                    connectStatus = ConnectStatus.WAITING_FOR_USER;
                    Log.i("WifiRunner", "SENDING WAITING_FOR_USER INTENT!");
                    sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                }
            }
            else{
                if(devicesInRange != null) devicesInRange.clear();
                connectStatus = ConnectStatus.UNKNOWN;
                sendIntent(connectStatus.name(), MessageType.CONNECT_STATUS);
                Log.i("WifiRunner", "Unknown connection state");
            }
            client.disconnect();
        }
        catch(Exception e){
            Log.i("WifiRunner", e.getLocalizedMessage());
            e.printStackTrace();
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
}
