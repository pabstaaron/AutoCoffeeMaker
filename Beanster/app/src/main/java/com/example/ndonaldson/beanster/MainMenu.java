package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Text;

public class MainMenu extends AppCompatActivity {

    private WifiRunner.ConnectStatus connectStatus;
    private ProgressBar connecting;
    private Button connectButton;
    private TextView connectingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        connectStatus = WifiRunner.ConnectStatus.CONNECT_TO_LAST;
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_STATUS_OUT"));
        sendIntent(connectStatus.name(), "status");
        ProgressBar connecting = (ProgressBar) findViewById(R.id.connectProgress);
        Button connectButton = (Button) findViewById(R.id.connectButton);
        TextView connectingText = (TextView) findViewById(R.id.connectText);
        connectButton.setEnabled(false);
        connectButton.setVisibility(View.GONE);
    }

    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            String status = intent.getStringExtra("status");
            Log.d("MainMenu", "wifiStatusReceiver got message: " + status);
            connectStatus = WifiRunner.ConnectStatus.valueOf(status);
            switch(connectStatus){
                case CONNECTED:{
                    Intent brewIntent = new Intent(this, CoffeeBrew.class);
                    startActivity(brewIntent);
                    finish();
                    break;
                }
                case UNKNOWN:{
                    //Default state....don't know what to do with it.
                    break;
                }
                default:{
                    connectButton.setEnabled(true);
                    connectButton.setVisibility(View.VISIBLE);
                    connecting.setEnabled(false);
                    connecting.setVisibility(View.INVISIBLE);
                    connectingText.setEnabled(false);
                    connectingText.setVisibility(View.INVISIBLE);
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
