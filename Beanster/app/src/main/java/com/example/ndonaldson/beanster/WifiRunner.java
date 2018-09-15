package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Log;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.content.Context.WIFI_SERVICE;


/**
 *@author Nathan Donaldson
 * This class keeps track of the state of communication with a device.
 */
public class WifiRunner implements Runnable {

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
    private boolean firstConnect;
    private boolean needsScan;
    private boolean isScanning;
    private String myIP;
    private static final String  DEVICES_LOCATION = Environment.getExternalStorageDirectory().getAbsolutePath() + "/deviceIds.txt";


    /**
     * CONSTRUCTOR
     * Retreive list of devices saved on this device so we can have a cache of passwords for each device as well as other info.
     * Also get current LAN information
     * @param context
     */
    public WifiRunner(final Context context){

    try {

        firstConnect = true;
        needsScan = true;
        isScanning = false;
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
                    Log.i("WifiRunner", "lastDevice is: " + lastDevice.getMacAddress() + " with passWord: " + lastDevice.getPassWord());
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
        wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        WifiScanReceiver dirka = new WifiScanReceiver();
        context.registerReceiver(dirka,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        myIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Log.i("WifiRunner", "My IP Address is: " + myIP);
        connectStatus = ConnectStatus.SEARCHING;
        sendIntent("status");


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
        try {
            if (!isRunning) {
                isRunning = true;
                try {
                    if (connectStatus == ConnectStatus.CONNECTED) {
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
                            if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                                sendIntent("badRequest");
                            }
                            connectStatus = ConnectStatus.NO_WIFI;
                            sendIntent("status");
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            sendIntent("status");
                        }
                        client.disconnect();
                    } else if (connectStatus == ConnectStatus.SEARCHING) {
                        Log.i("WifiRunner", "SEARCHING!");
                        if(needsScan && !isScanning) {
                            if(!wifiManager.isWifiEnabled()){
                                wifiManager.setWifiEnabled(true);
                            }
                            Log.i("WifiRunner", "Starting Scan!");
                            Log.i("WifiRunner", "Scan result: " + wifiManager.startScan());
                        } else if(!isScanning && !needsScan){
                            if (firstConnect) {
                                Log.i("WifiRunner", "CONNECT_TO_LAST");
                                connectStatus = ConnectStatus.CONNECT_TO_LAST;
                                sendIntent("status");
                            } else if (!firstConnect) {
                                connectStatus = ConnectStatus.WAITING_FOR_USER;
                                sendIntent("data");
                                sendIntent("status");
                            }
                        }
                    } else if (connectStatus == ConnectStatus.WAITING_FOR_RESPONSE || connectStatus == ConnectStatus.CONNECT_TO_LAST) {
                        connectToWifi();
                        if (lastDevice != null &&
                                (connectStatus != ConnectStatus.WAITING_FOR_RESPONSE || connectStatus != ConnectStatus.CONNECT_TO_LAST)) {
                            Log.i("WifiRunner", "WAITING FOR RESPONSE!");
                            url = new URL("http://192.168.5.1:5000/connected/" + lastDevice.getPassWord());
                            client = (HttpURLConnection) url.openConnection();
                            client.setRequestMethod("GET");
                            client.setConnectTimeout(10000);
                            client.setReadTimeout(10000);

                            Log.i("WifiRunner", client.getURL().toString());
                            int responseCode = 404;
                            try {
                                responseCode = client.getResponseCode();
                            } catch (Exception e) {
                                StringWriter writer = new StringWriter();
                                PrintWriter printWriter = new PrintWriter( writer );
                                e.printStackTrace( printWriter );
                                printWriter.flush();
                                String stackTrace = writer.toString();
                                Log.i("WifiRunner", e.getLocalizedMessage());
                                Log.i("WifiRunner", stackTrace);
                                sendIntent("failure");
                                lastDevice = null;
                            }
                            Log.i("WifiRunner", "responseCode:" + responseCode);
                            if (responseCode != HttpURLConnection.HTTP_OK) {
                                if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                                    if (connectStatus != ConnectStatus.CONNECT_TO_LAST)
                                        sendIntent("badRequest");
                                }
                                Log.i("WifiRunner", "Failed to connect");
                                connectStatus = ConnectStatus.NO_WIFI;
                                sendIntent("status");
                                connectStatus = ConnectStatus.WAITING_FOR_USER;
                                lastDevice = null;
                                sendIntent("status");
                            } else {
                                Log.i("WifiRunner", "Connected");
                                boolean exists = false;
                                boolean update = false;
                                if (savedDevices != null) {
                                    for (Device d : savedDevices) {
                                        if (d.getMacAddress().equals(lastDevice.getMacAddress()))
                                            if (d.getPassWord() != lastDevice.getPassWord()) {
                                                d.setPassWord(lastDevice.getPassWord());
                                                update = true;
                                            }
                                        exists = true;
                                    }
                                }

                                if (!exists || update) {
                                    if (savedDevices == null) savedDevices = new ArrayList<>();
                                    if (!update) savedDevices.add(lastDevice);
                                    try {
                                        File file = new File(DEVICES_LOCATION);
                                        FileOutputStream fos = new FileOutputStream(file, false);
                                        ObjectOutputStream os = new ObjectOutputStream(fos);
                                        os.writeObject(savedDevices);
                                        os.flush();
                                        fos.getFD().sync();
                                        os.close();
                                    } catch (EOFException e) {
                                        e.printStackTrace();
                                        Log.i("WifiRunner", "" + e.getLocalizedMessage());
                                    }

                                }
                                connectStatus = ConnectStatus.CONNECTED;
                                if (firstConnect) sendIntent("lastDevice");
                                sendIntent("status");
                                connectStatus = ConnectStatus.WAITING_FOR_USER;
                                sendIntent("status");
                            }
                        } else if((connectStatus != ConnectStatus.WAITING_FOR_RESPONSE || connectStatus != ConnectStatus.CONNECT_TO_LAST)){
                            //wifiManager.disconnect();
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            sendIntent("status");
                        }
                        firstConnect = false;
                        if(client != null)client.disconnect();
                    } else if (connectStatus == ConnectStatus.WAITING_FOR_USER) {
                        Log.i("WifiRunner", "WAITING FOR USER!");
                    }
                } catch (Exception e) {
                    Log.i("WifiRunner", e.getLocalizedMessage());
                    e.printStackTrace();
                }
                isRunning = false;
            }
        }
        catch(Exception e){
            Log.i("WifiRunner", "WifiRunner Crashed");
            //wifiManager.disconnect();
            connectStatus = ConnectStatus.NO_WIFI;
            sendIntent("status");
            connectStatus = ConnectStatus.WAITING_FOR_USER;
            sendIntent("status");
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
                        if(savedDevices != null) {
                            for (Device d2 : savedDevices) {
                                if (d.getMacAddress().equals(d2.getMacAddress())) {
                                    d.setPassWord(d2.getPassWord());
                                }
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
                if(connectStatus == ConnectStatus.SEARCHING){
                    needsScan = true;
                    isScanning = false;
                }
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
     * Connect to a specific network
     */
    private void connectToWifi(){

        if(lastDevice == null){
            Log.i("WifiRunner", "LastDevice is null");
            connectStatus = ConnectStatus.WAITING_FOR_USER;
            sendIntent("status");
            return;
        }
        String networkSSID = lastDevice.getHostName();
        String networkPass = lastDevice.getPassWord();
        Boolean exists = false;

        Log.i("WifiRunner", "connectToWifi: hostName: " + lastDevice.getHostName() + ", password: " + lastDevice.getPassWord());

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                Log.i("WifiRunner", "In configured networks!!  " + i.SSID);
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                exists = true;
                break;
            }
        }

        if(!exists) {
            WifiConfiguration wc = new WifiConfiguration();
            wc.SSID = "\"" + networkSSID + "\"";
            wc.preSharedKey = "\"" + networkPass + "\"";
            wc.hiddenSSID = true;
            wc.status = WifiConfiguration.Status.ENABLED;
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            wc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            wc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);


            int res = wifiManager.addNetwork(wc);

            wifiManager.disconnect();
            Log.i("WifiRunner", "add Network returned " + res);
            boolean b = wifiManager.enableNetwork(res, true);
            Log.i("WifiRunner", "enableNetwork returned " + b);
        }
    }

    public class WifiScanReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                devicesInRange.clear();

                List<ScanResult> scanResults = wifiManager.getScanResults();

                for(ScanResult s : scanResults){
                    Log.i("WifiRunner", s.toString());
                    if(s.BSSID != null && s.BSSID.startsWith("b8:27:eb")){
                        devicesInRange.add(new Device(s.BSSID, s.SSID, ""));
                        Log.i("WifiRunner", "Adding RaspberryPi mac: " + s.BSSID + " with hostName: " + s.SSID);
                    }
                }
                needsScan = false;
                isScanning = false;
            }
        }
    };
}
