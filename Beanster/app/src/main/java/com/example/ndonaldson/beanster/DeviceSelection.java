package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.nispok.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class DeviceSelection extends AppCompatActivity implements WifiViewHolder.OnItemSelectedListener{

    private WifiRunner.ConnectStatus connectStatus;
    RecyclerView recyclerView;
    WifiAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_selection);
        connectStatus = WifiRunner.ConnectStatus.SEARCHING;
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_STATUS_OUT"));
        sendIntent(connectStatus.name(), "status");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView = (RecyclerView) this.findViewById(R.id.selection_list);
        recyclerView.setLayoutManager(layoutManager);
        List<WifiSelection> selectableItems = generateItems();
        adapter = new WifiAdapter(this,selectableItems,false);
        recyclerView.setAdapter(adapter);
    }

    public List<WifiSelection> generateItems(){

        List<WifiSelection> selectableItems = new ArrayList<>();
        selectableItems.add(new WifiSelection("cheese"));
        selectableItems.add(new WifiSelection("is"));
        selectableItems.add(new WifiSelection("awesome"));

        return selectableItems;
    }

    @Override
    public void onItemSelected(SelectableWifi selectableItem) {

        List<SelectableWifi> selectedItems = adapter.getSelectedItems();
        Toast.makeText(this,"Selected item is "+selectableItem.getDeviceID()+
                ", Totally  selectem item count is "+selectedItems.size(),Toast.LENGTH_LONG).show();
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
                    //Continue to the next activity
                }
                case SEARCHING:{
                    //Do nothing but display each known device within range
                }
                case WAITING_FOR_RESPONSE:{
                    //Wait on response from user chosen device
                }
                case UNKNOWN:{
                    //Default state....don't know what to do with it.
                }
                case WAITING_FOR_USER:{
                    //Wait for user to select from device list or enter new device
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
