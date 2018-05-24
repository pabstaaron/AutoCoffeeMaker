package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
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

import java.util.ArrayList;
import java.util.List;

public class DeviceSelection extends AppCompatActivity implements WifiViewHolder.OnItemSelectedListener{

    private WifiRunner.ConnectStatus mConnectStatus;
    private ArrayList<String> mDeviceIds;
    RecyclerView recyclerView;
    WifiAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_device_select);
            mConnectStatus = WifiRunner.ConnectStatus.SEARCHING;
            mDeviceIds = new ArrayList<>();
            LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                    new IntentFilter("com.android.activity.WIFI_DATA_OUT"));
            sendIntent(mConnectStatus.name(), "status");

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView = (RecyclerView) this.findViewById(R.id.selection_list);
            recyclerView.setLayoutManager(layoutManager);
            List<WifiSelectItem> selectableItems = generateItems();
            adapter = new WifiAdapter(this, selectableItems, false);
            recyclerView.setAdapter(adapter);
        }
        catch(Exception e){
            Log.i("DeviceSelection", e.getLocalizedMessage());
        }
    }

    /**
     *
     * @return
     */
    public List<WifiSelectItem> generateItems(){

        List<WifiSelectItem> selectableItems = new ArrayList<>();
        for(String s: mDeviceIds){
            selectableItems.add(new WifiSelectItem(s));
        }

        return selectableItems;
    }

    /**
     *
     * @param selectableItem
     */
    @Override
    public void onItemSelected(SelectableWifi selectableItem) {

        List<SelectableWifi> selectedItems = adapter.getSelectedItems();
        Toast.makeText(this,"Selected item is "+selectableItem.getDeviceID()+
                ", Totally  selectem item count is "+selectedItems.size(),Toast.LENGTH_LONG).show();

        //Make connect button enabled, clicking will set into waiting for response, also unclicking will disable
        //Also want another button where user can enter in own deviceID, state would go to waiting for response on submission
    }

    /**
     *
     */
    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if(intent.hasExtra("status")){
                String status = intent.getStringExtra("status");
                mConnectStatus = WifiRunner.ConnectStatus.valueOf(status);
                switch(mConnectStatus){
                    case CONNECTED:{
                        //Continue to the next activity
                    }
                    case SEARCHING:{
                        //Just display current devices you can connect to.
                        generateItems();
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
            if(intent.hasExtra("deviceIds")){
                ArrayList<? extends String> deviceIds;
                // use local var intent
                deviceIds = intent.getParcelableArrayListExtra("deviceIds");
                Log.d("onReceive", "got myList");

                for (String s: deviceIds) {
                    if(!mDeviceIds.contains(s)){
                        mDeviceIds.add(s);
                        adapter.addItem(new WifiSelectItem(s));   //Might have to set recycler view adapter again but not sure.....
                    }
                }
            }
            Log.d("DeviceSelection", "wifiStatusReceiver got message: " );

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
