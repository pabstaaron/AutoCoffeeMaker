package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class DeviceSelection extends AppCompatActivity {

    private WifiRunner.ConnectStatus connectStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_selection);
        connectStatus = WifiRunner.ConnectStatus.CONNECT_TO_LAST;
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_STATUS_OUT"));
        sendIntent(connectStatus.name(), "status");
    }

    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String status = intent.getStringExtra("status");
            Log.d("MainMenu", "wifiStatusReceiver got message: " + status);
            connectStatus = WifiRunner.ConnectStatus.valueOf(status);
            switch(connectStatus){
                case CONNECT_TO_LAST:{
                    //Do nothing but have loading symbol and text because wifiRunner will take care of it
                }
                case CONNECTED:{
                    //Continue to the next activity
                }
                case SEARCHING:{
                    //Do nothing but display each new device within range by
                    //accessing wifiRunner data somehow
                }
                case WAITING_FOR_RESPONSE:{
                    //Do nothing but have loading symbol and text because wifiRunner will take care of it
                }
                case UNKNOWN:{
                    //Default state....don't know what to do with it.
                }
                case WAITING_FOR_USER:{
                    //Do nothing but wait for user to push "connect"
                }
            }
        }
    };

    /**
     * Send selected deviceID or connection status change to wifirunner
     * @param connectStatus
     * @param type
     */
    private void sendIntent(String connectStatus, String type){
        Intent intent = new Intent();
        intent.putExtra(type,connectStatus);
        intent.setAction("com.android.activity.WIFI_DATA_IN");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }
}
