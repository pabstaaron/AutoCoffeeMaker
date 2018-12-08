package com.example.ndonaldson.beanster.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.example.ndonaldson.beanster.data.Device;

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
    private Context mContext;
    private WifiManager wifiManager;
    private ConnectivityManager mConnManager;
    private NetworkInfo mWifi;
    private boolean isRunning;
    private boolean firstConnect;
    private boolean needsScan;
    private boolean isScanning;
    private boolean isConnected;
    private WifiScanReceiver wiFight;
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
        isConnected = false;
        mContext = context;
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
        catch(Exception e){
            e.printStackTrace();
            Log.i("WifiRunner", "" + e.getLocalizedMessage());
        }
        devicesInRange = new ArrayList<>();
        mConnManager = (ConnectivityManager) mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);
        mWifi = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        wifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
        wiFight = new WifiScanReceiver();


        myIP = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        Log.i("WifiRunner", "My IP Address is: " + myIP);
        connectStatus = ConnectStatus.SEARCHING;
        sendIntent("status");


        LocalBroadcastManager.getInstance(mContext).registerReceiver(wifiStatusReceiver,
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
                mWifi = mConnManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                try {
                    if (connectStatus == ConnectStatus.SEARCHING) {
                        Log.i("WifiRunner", "SEARCHING!");
                        if(needsScan && !isScanning) {
                            if(!wifiManager.isWifiEnabled()){
                                Log.i("WifiRunner", "Enabling wifi for searching");
                                wifiManager.setWifiEnabled(true);
                            }
                            Log.i("WifiRunner", "Starting Scan!");
                            mContext.registerReceiver(wiFight,
                                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                            Log.i("WifiRunner", "Scan result: " + wifiManager.startScan());
                            isScanning = true;
                        } else if(!isScanning && !needsScan){
                            if (firstConnect) {
                                Log.i("WifiRunner", "CONNECT_TO_LAST");
                                connectStatus = ConnectStatus.CONNECT_TO_LAST;
                                sendIntent("status");
                            } else {
                                Log.i("WifiRunner", "WAITING_FOR_USER");
                                connectStatus = ConnectStatus.WAITING_FOR_USER;
                                sendIntent("data");
                                sendIntent("status");
                            }
                        }
                    } else if (connectStatus == ConnectStatus.WAITING_FOR_RESPONSE || connectStatus == ConnectStatus.CONNECT_TO_LAST) {
                        if(!wifiManager.isWifiEnabled()){
                            Log.i("WifiRunner", "Enabling wifi for response");
                            wifiManager.setWifiEnabled(true);
                        }
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
                                if(isConnected) connectStatus = ConnectStatus.NO_WIFI;
                                isConnected = false;
                                if(connectStatus != ConnectStatus.CONNECT_TO_LAST) sendIntent("status");
                                connectStatus = ConnectStatus.WAITING_FOR_USER;
                               // devicesInRange.clear();
                                //lastDevice = null;

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
                                isConnected = true;
                                if (firstConnect) sendIntent("lastDevice");
                                sendIntent("status");
                                connectStatus = ConnectStatus.WAITING_FOR_USER;
                                sendIntent("status");
                            }
                        } else{
                            connectStatus = ConnectStatus.WAITING_FOR_USER;
                            sendIntent("status");
                        }
                        firstConnect = false;
                        if(client != null) client.disconnect();
                    } else if (connectStatus == ConnectStatus.WAITING_FOR_USER) {
                        Log.i("WifiRunner", "WAITING FOR USER!");
                        if(isConnected) {
                            Log.i("WifiRunner", "mWifi name: " + mWifi.getExtraInfo());
                            Log.i("WifiRunner", "mWifi isConnectedOrConnecting: " + mWifi.isConnectedOrConnecting());
                            Log.i("WifiRunner", "mWifi state: " + mWifi.getState().name());
                            Log.i("WifiRunner", "WifiManager wifi enabled: " + wifiManager.isWifiEnabled());
                            if(!mWifi.isConnectedOrConnecting() || !wifiManager.isWifiEnabled()){
                                Log.i("WifiRunner", "NOT CONNECTED!");
                                wifiManager.disconnect();
                                isConnected = false;
                                connectStatus = ConnectStatus.NO_WIFI;
                                sendIntent("status");
                                connectStatus = ConnectStatus.WAITING_FOR_USER;
                                sendIntent("status");
                                devicesInRange.clear();
                            } else {
                                Log.i("WifiRunner", "CONNECTED!");
                                url = new URL("http://192.168.5.1:5000/connected/" + lastDevice.getPassWord());
                                client = (HttpURLConnection) url.openConnection();
                                client.setRequestMethod("GET");
                                client.setConnectTimeout(10000);
                                client.setReadTimeout(10000);
                                //client.setRequestProperty("serial", deviceIDs.get(searchingCount));
                                Log.i("WifiRunner", "URL: " + client.getURL().toString());
                                int responseCode = 404;
                                try {
                                    responseCode = client.getResponseCode();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Log.i("WifiRunner", e.getLocalizedMessage());
                                    //if(isConnected != false) sendIntent("failure");
                                }
                                if (responseCode != HttpURLConnection.HTTP_OK) {
                                    if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                                        sendIntent("badRequest");
                                    }
                                    if(isConnected) connectStatus = ConnectStatus.NO_WIFI;
                                    isConnected = false;
                                    sendIntent("status");
                                    connectStatus = ConnectStatus.WAITING_FOR_USER;
                                    sendIntent("status");
                                    //devicesInRange.clear();
                                }
                                client.disconnect();
                            }
                        }
                        else{
                            Log.i("WifiRunner", "NOT CONNECTED!");
                        }
                    }
                    else{
                        Log.i("WifiRunner", "YOU LOSE!!");
                        connectStatus = ConnectStatus.WAITING_FOR_USER;
                        sendIntent("status");
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
            isConnected = false;
            connectStatus = ConnectStatus.NO_WIFI;
            sendIntent("status");
            connectStatus = ConnectStatus.WAITING_FOR_USER;
            sendIntent("status");
            devicesInRange.clear();
        } catch(Throwable t){
            Log.i("WifiRunner", "WifiRunner Crashed");
            isConnected = false;
            connectStatus = ConnectStatus.NO_WIFI;
            sendIntent("status");
            connectStatus = ConnectStatus.WAITING_FOR_USER;
            sendIntent("status");
            devicesInRange.clear();
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
                    if(devicesInRange != null && !devicesInRange.isEmpty())
                    for(Device d: devicesInRange){
                        Log.i("WifiRunner", "Devices to send: " + d.getHostName());
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
                if(!devicesInRange.isEmpty())
                    intent.putExtra("lastDevice", (Parcelable) lastDevice);
            }
            else if (type.equals("failure")){
                intent.putExtra("Failure", "");
            }
            else if (type.equals("badRequest")){
                intent.putExtra("badRequest", "");
            }

        if(intent.getExtras() != null && !intent.getExtras().isEmpty()) {
            Log.i("WifiRunner", "Sending Intent " +  type + " from wifiRunner");
            intent.setAction("com.android.activity.WIFI_DATA_OUT");
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
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
            else if(intent.hasExtra("sendLast")){
                sendIntent("lastDevice");
            }
        }
    };

    /**
     * Connect to a specific network
     */
    private void connectToWifi(){

        if(lastDevice == null){
            Log.i("WifiRunner", "LastDevice is null");
            return;
        }
        String networkSSID = lastDevice.getHostName();
        String networkPass = lastDevice.getPassWord();
        String networkMac = lastDevice.getMacAddress();
        Boolean exists = false;

        Log.i("WifiRunner", "connectToWifi: hostName: " + networkSSID + ", password: " + networkPass + ", macAddress: " + networkMac);
        WifiInfo info = wifiManager.getConnectionInfo();
        Log.i("WifiRunner", "connectToWifi: myCurrentHostName: " + info.getSSID() + ", myBSSID: " + info.getBSSID() + ", myMACADDY: " + info.getMacAddress());
        if(info.getBSSID().equals(networkMac) && info.getSSID().split("\"")[1].equals(networkSSID)){
            Log.i("WifiRunner", "Already connected to device!");
            return;
        }

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

            try {
                Thread.sleep(5000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public class WifiScanReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                devicesInRange.clear();
                Log.i("WifiRunner", "Scan received!");
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
            else{
                Log.i("WifiRunner", "Fuck you!");
            }
            c.unregisterReceiver(this);
        }
    }
}
