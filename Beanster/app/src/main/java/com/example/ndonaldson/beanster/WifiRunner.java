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
    private ConnectStatus connectStatus = ConnectStatus.UNKNOWN;
    private HttpURLConnection client;
    private URL url;
    private Context context;
    private WifiManager wifiManager;
    private ConnectivityManager mConnManager;
    private NetworkInfo mWifi;
    private boolean isRunning;
    private String myIP;
    private static final String  DEVICES_LOCATION = Environment.getExternalStorageDirectory().getAbsolutePath() + "/deviceIds.txt";


    /**
     * @param context
     * Constructor for WifiRunner
     */
    public WifiRunner(Context context){
    try {
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
                if (savedDevices != null || !savedDevices.isEmpty())
                    lastDevice = savedDevices.get(savedDevices.size() - 1);
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


        if(devicesInRange != null && !devicesInRange.isEmpty() && lastDevice != null && !lastDevice.getMacAddress().isEmpty()
                && connectStatus != ConnectStatus.NO_WIFI) {
            for (Device d : devicesInRange) {
                if (d.getMacAddress().equals(lastDevice.getMacAddress()))
                    connectStatus = ConnectStatus.CONNECT_TO_LAST;
                    sendIntent("status");
            }
        }
        else if(connectStatus == ConnectStatus.UNKNOWN){
            connectStatus = ConnectStatus.WAITING_FOR_USER;
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
     * Check wifi connection every second to make sure that we are still connected.
     * If no connection kick out to main screen.
     * Keep checking response from raspberry PI.
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
                    if (devicesInRange != null) devicesInRange.clear();
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
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent("status");
                    }
                    client.disconnect();
                }
                else if (connectStatus == ConnectStatus.SEARCHING){
                    Log.i("WifiRunner", "SEARCHING!");
                    pingDevices();
                    createArpMap();
                    connectStatus = ConnectStatus.WAITING_FOR_USER;
                    sendIntent("data");
                    sendIntent("status");
                }
                else if (connectStatus == ConnectStatus.WAITING_FOR_RESPONSE) {
                    Log.i("WifiRunner", "WAITING FOR RESPONSE!");
                    url = new URL("http://" + lastDevice.getiP() + ":5000/connected/" + lastDevice.getsN());
                    client = (HttpURLConnection) url.openConnection();
                    if (lastDevice != null && !lastDevice.getiP().isEmpty()) {
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
                            Log.i("WifiRunner", "Failed to connect");
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            lastDevice = null;
                            sendIntent("status");
                        } else {
                            Log.i("WifiRunner", "Connected");
                            boolean exists = false;
                            if(savedDevices != null) {
                                for (Device d : savedDevices) {
                                    if (d.getMacAddress().equals(lastDevice.getMacAddress()))
                                        exists = true;
                                }
                            }

                            if(!exists) {
                                if (savedDevices == null) savedDevices = new ArrayList<>();
                                savedDevices.add(lastDevice);
                                FileOutputStream fos = context.openFileOutput(DEVICES_LOCATION, Context.MODE_PRIVATE);
                                ObjectOutputStream os = new ObjectOutputStream(fos);
                                os.writeObject(savedDevices);
                                os.close();
                                fos.close();
                            }
                            connectStatus = ConnectStatus.CONNECTED;
                            sendIntent("status");
                        }
                    } else {
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent("status");

                    }
                    client.disconnect();
                } else if (connectStatus == ConnectStatus.WAITING_FOR_USER) {
                    Log.i("WifiRunner", "WAITING FOR USER!");
                } else if (connectStatus == ConnectStatus.CONNECT_TO_LAST) {
                    Log.i("WifiRunner", "CONNECTING TO LAST!");
                    if (client != null && lastDevice != null) {
                        url = new URL("http://" + lastDevice.getiP() + ":5000/connected/" + lastDevice.getsN());
                        client = (HttpURLConnection) url.openConnection();
                        client.setRequestMethod("GET");
                        client.setConnectTimeout(1000);
                        int responseCode = client.getResponseCode();
                        Log.i("WifiRunner", "responseCode is: " + responseCode);
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            connectStatus = ConnectStatus.CONNECTED;
                            sendIntent("status");
                        } else {
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            Log.i("WifiRunner", "SENDING WAITING_FOR_USER INTENT!");
                            sendIntent("status");
                        }
                        client.disconnect();
                    } else {
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        Log.i("WifiRunner", "SENDING WAITING_FOR_USER INTENT!");
                        sendIntent("status");
                    }
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
     * Send out connection status change to any app with proper receiver
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
                    intent.putParcelableArrayListExtra("deviceIds", devicesInRange);
                }
                catch(Exception e){
                    Log.i("WifiRunner",e.getLocalizedMessage());
                }
            }
            else if (type.equals("failure")){
                intent.putExtra("Failure", "");
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
            else if(intent.hasExtra("deviceID")){
                Device device = intent.getParcelableExtra("deviceID");
                Log.d("WifiRunner", "wifiStatusReceiver got deviceID message: " + device);
                lastDevice = device;
            }
            else if(intent.hasExtra("sendDevices")){
                sendIntent("sendDevices");
            }
        }
    };

    /**
     * Extract and save ip and corresponding MAC address from arp table in HashMap
     */
    public void createArpMap() throws IOException {
        devicesInRange.clear();
        Map<String, String> checkMapARP = new HashMap<>();
        BufferedReader localBufferdReader = new BufferedReader(new FileReader(new File("/proc/net/arp")));
        String line;

        while ((line = localBufferdReader.readLine()) != null) {
            String[] ipmac = line.split("[ ]+");
            for(String s: ipmac){
                Log.i("WifiRunner", "ARP INFO! " + s + "\n");
            }
            if (!ipmac[0].matches("IP")) {
                String ip = ipmac[0];
                String mac = ipmac[3];
                if (!checkMapARP.containsKey(mac) && mac.startsWith("b8:27:eb")) {
                    checkMapARP.put(mac, ip);
                    devicesInRange.add(new Device(mac, "", "", ip));
                    Log.i("WifiRunner", "Adding RaspberryPi: " + mac);
                }
                Log.i("WifiRunner", "\n\n\n");
            }
        }
    }

    public void pingDevices(){
        try {
            NetworkInterface iFace = NetworkInterface
                    .getByInetAddress(InetAddress.getByName(myIP));

            for (int i = 0; i <= 255; i++) {

                // build the next IP address
                String addr = myIP;
                addr = addr.substring(0, addr.lastIndexOf('.') + 1) + i;
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
