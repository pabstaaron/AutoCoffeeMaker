package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;



/**
 *@author Nathan Donaldson
 * This class keeps track of the state of communication with a device.
 */
public class WifiRunner implements Runnable {

    private ArrayList<String> deviceIDs;
    private ArrayList<String> devicesInRange;
    private ConnectStatus connectStatus = ConnectStatus.CONNECT_TO_LAST;
    private String connectedDevice = "";
    private HttpURLConnection client;
    private URL url;
    private Context context;
    private int searchingCount;

    /**
     * @param context
     * Constructor for WifiRunner
     */
    public WifiRunner(Context context){
    try {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "deviceIds.txt");
        Scanner s = new Scanner(file);
        deviceIDs = new ArrayList<>();
        devicesInRange = new ArrayList<>();
        while(s.hasNext()){
            deviceIDs.add(s.next());
        }
        s.close();

        url = new URL("http://127.0.0.1:5000/connect/");
        client = (HttpURLConnection) url.openConnection();
        searchingCount = 0;
        this.context = context;

        LocalBroadcastManager.getInstance(context).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_SATUS_IN"));

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
                devicesInRange.clear();
                client.setRequestMethod("GET");
                client.setRequestProperty("serial", deviceIDs.get(searchingCount));
                int responseCode = client.getResponseCode();
                if(responseCode != HttpURLConnection.HTTP_OK){
                    connectedDevice = "";
                    connectStatus = ConnectStatus.WAITING_FOR_USER;
                    sendIntent(connectStatus.name());
                }
            }
            else if(connectStatus == ConnectStatus.SEARCHING) {
                client.setRequestMethod("GET");
                client.setRequestProperty("serial", deviceIDs.get(searchingCount));
                int responseCode = client.getResponseCode();
                if(responseCode != HttpURLConnection.HTTP_OK){
                    if(devicesInRange.contains(deviceIDs.get(searchingCount))){
                        int positionToRemove = 0;
                        for(int i = 0; i <= devicesInRange.size()-1; i++){
                            if(deviceIDs.get(searchingCount).equals(devicesInRange.get(i))) positionToRemove = i;
                        }
                        devicesInRange.remove(positionToRemove);
                    }
                }
                else{
                    if(!devicesInRange.contains(deviceIDs.get(searchingCount))){
                        devicesInRange.add(deviceIDs.get(searchingCount));
                    }
                }
                if(searchingCount == deviceIDs.size()-1) searchingCount = 0;
                else searchingCount++;
            }
            else if(connectStatus == ConnectStatus.WAITING_FOR_RESPONSE){
                if(!connectedDevice.isEmpty()){
                    client.setRequestMethod("GET");
                    client.setRequestProperty("serial", connectedDevice);
                    int responseCode = client.getResponseCode();
                    if(responseCode != HttpURLConnection.HTTP_OK){
                        connectedDevice = "";
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent(connectStatus.name());
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
                        sendIntent(connectStatus.name());
                    }
                }
            }
            else if(connectStatus == ConnectStatus.WAITING_FOR_USER){
                devicesInRange.clear();
            }
            else if(connectStatus == ConnectStatus.CONNECT_TO_LAST){
                devicesInRange.clear();
                client.setRequestMethod("GET");
                client.setRequestProperty("serial", deviceIDs.get(0));
                int responseCode = client.getResponseCode();
                if(responseCode == HttpURLConnection.HTTP_OK){
                    connectStatus = ConnectStatus.CONNECTED;
                    connectedDevice = deviceIDs.get(0);
                    sendIntent(connectStatus.name());
                }
                else{
                    connectStatus = ConnectStatus.WAITING_FOR_USER;
                    sendIntent(connectStatus.name());
                }
            }
            else{
                devicesInRange.clear();
                connectStatus = ConnectStatus.UNKNOWN;
                sendIntent(connectStatus.name());
                Log.i("WifiRunner", "Unknown connection state");
            }
        }
        catch(Exception e){
            Log.i("WifiRunner", e.getLocalizedMessage());
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
     * Send out connection status change to any app with proper receiver
     * @param connectStatus
     */
    private void sendIntent(String connectStatus){
        Intent intent = new Intent();
        intent.putExtra("status",connectStatus);
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
            else if(intent.hasExtra("deviceID")){
                String deviceId = intent.getStringExtra("deviceID");
                Log.d("WifiRunner", "wifiStatusReceiver got deviceID message: " + deviceId);
                connectedDevice = deviceId;
            }
        }
    };
}
