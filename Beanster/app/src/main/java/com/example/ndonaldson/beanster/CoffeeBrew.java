package com.example.ndonaldson.beanster;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CoffeeBrew extends AppCompatActivity {

    private WifiRunner.ConnectStatus mConnectStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coffee_brew);
        mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;
    }

    /**
     * Return to device selection screen
     */
    @Override
    public void onBackPressed(){
        sendIntent("status");
        Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
        startActivity(deviceIntent);
        finish();
    }

    /**
     * Send selected deviceID or connection status change to wifirunner
     * @param type
     */
    private void sendIntent(String type){
        Intent intent = new Intent();
        if(type.equals("status")){
            intent.putExtra(type,mConnectStatus.name());
            intent.setAction("com.android.activity.WIFI_DATA_IN");
        }
        if(!intent.getExtras().isEmpty()) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }
}
