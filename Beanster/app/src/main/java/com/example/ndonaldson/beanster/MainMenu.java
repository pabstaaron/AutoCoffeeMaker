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
import android.widget.Toast;

import org.w3c.dom.Text;

import static android.R.attr.value;

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
                new IntentFilter("com.android.activity.WIFI_DATA_OUT"));
        sendIntent(connectStatus.name(), "status");

        ProgressBar connecting = (ProgressBar) findViewById(R.id.connectProgress);
        Button connectButton = (Button) findViewById(R.id.connectButton);
        TextView connectingText = (TextView) findViewById(R.id.connectText);
        connectButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                sendIntent(WifiRunner.ConnectStatus.SEARCHING.name(), "status");
                Intent brewIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                startActivity(brewIntent);
                finish();
            }
        });
        connectButton.setVisibility(View.INVISIBLE);
        connectButton.setEnabled(false);
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
                    Intent brewIntent = new Intent(getApplicationContext(), CoffeeBrew.class);
                    startActivity(brewIntent);
                    finish();
                    break;
                }
                case UNKNOWN:{
                    //Default state....don't know what to do with it.
                    break;
                }
                case WAITING_FOR_USER:{
                    try {
                        findViewById(R.id.connectButton).setEnabled(true);
                        findViewById(R.id.connectButton).setVisibility(View.VISIBLE);
                        findViewById(R.id.connectProgress).setVisibility(View.INVISIBLE);
                        findViewById(R.id.connectText).setVisibility(View.INVISIBLE);
                    }
                    catch(Exception e)
                    {
                        Log.i("MainMenu", e.getLocalizedMessage());
                    }
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

    @Override
    public void onSaveInstanceState(Bundle bundle){
        bundle.putString("connectStatus", connectStatus.name());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {

        if(savedInstanceState.containsKey("connectStatus")) connectStatus = WifiRunner.ConnectStatus.valueOf(savedInstanceState.getString("connectStatus"));

        switch(connectStatus){
            case CONNECTED:{
                Intent brewIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                startActivity(brewIntent);
                finish();
                break;
            }
            case UNKNOWN:{
                //Default state....don't know what to do with it.
                break;
            }
            case WAITING_FOR_USER:{
                connectButton.setEnabled(true);
                connectButton.setVisibility(View.VISIBLE);
                connecting.setVisibility(View.INVISIBLE);
                connectingText.setVisibility(View.INVISIBLE);
            }
        }
    }
}
