package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.victor.loading.newton.NewtonCradleLoading;

import java.util.ArrayList;
import java.util.List;

public class DeviceSelection extends AppCompatActivity implements WifiViewHolder.OnItemSelectedListener{

    private WifiRunner.ConnectStatus mConnectStatus;
    private ArrayList<Device> mDeviceIds;
    private Button cancelButton;
    private Button connectButton;
    private Button searchButton;
    private TextView connectText;
    private NewtonCradleLoading mLoadingProgress, mSearchProgress;
    private String deviceSelectedName;
    private Device deviceSelected;
    private ViewFlipper viewFlipper;
    private Context mContext;
    private ProgressBar progressBack;
    private Animation fade_in, fade_out;
    private TextView devicesLabel;
    private RecyclerView recyclerView;
    private WifiAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            mContext = this;
            deviceSelectedName = "";
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_device_select);

            try {
                viewFlipper = (ViewFlipper) this.findViewById(R.id.backgroundView);
                fade_in = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);
                fade_out = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
                fade_in.setInterpolator(new DecelerateInterpolator());
                fade_out.setInterpolator(new AccelerateDecelerateInterpolator());
                fade_in.setDuration(3000);
                fade_out.setStartOffset(1000);
                fade_out.setDuration(3000);
                viewFlipper.setInAnimation(fade_in);
                viewFlipper.setOutAnimation(fade_out);
                viewFlipper.setAutoStart(true);
                viewFlipper.setFlipInterval(10000);
                if(getIntent() != null && getIntent().hasExtra("flipper")){
                    viewFlipper.setDisplayedChild(getIntent().getIntExtra("flipper", 0));
                }
                viewFlipper.startFlipping();
            }
            catch(Exception e){
                e.printStackTrace();
                Log.i("DeviceSelection", "" + e.getLocalizedMessage());
            }

            mConnectStatus = WifiRunner.ConnectStatus.SEARCHING;
            mDeviceIds = new ArrayList<>();
            cancelButton = (Button) findViewById(R.id.deviceSelectCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
                    Intent intent = new Intent(getApplicationContext(), MainMenu.class);
                    intent.putExtra("selection", true);
                    intent.putExtra("flipper", viewFlipper.getDisplayedChild());
                    startActivity(intent);
                    finish();
                    mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;
                    sendIntent("status");
                }
            });
            searchButton = (Button) findViewById(R.id.searchDevices);
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mConnectStatus = WifiRunner.ConnectStatus.SEARCHING;
                    sendIntent("status");
                    searchButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                    searchButton.setTextColor(Color.rgb(204, 204, 204));
                    searchButton.setEnabled(false);
                    connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                    connectButton.setTextColor(Color.rgb(204, 204, 204));
                    connectButton.setEnabled(false);
                    mSearchProgress.setVisibility(View.VISIBLE);
                    deviceSelectedName = "";

                }
            });
            connectButton = (Button) findViewById(R.id.deviceConnect);
            connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
            connectButton.setTextColor(Color.rgb(204, 204, 204));
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    for(Device d : mDeviceIds){
                        if(d.getMacAddress().equals(deviceSelectedName) && !d.getsN().isEmpty()){
                            progressBack.setVisibility(View.VISIBLE);
                            mLoadingProgress.setVisibility(View.VISIBLE);
                            connectText.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.INVISIBLE);
                            recyclerView.setEnabled(false);
                            devicesLabel.setVisibility(View.INVISIBLE);
                            searchButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                            searchButton.setTextColor(Color.rgb(204, 204, 204));
                            searchButton.setEnabled(false);
                            connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                            connectButton.setTextColor(Color.rgb(204, 204, 204));
                            connectButton.setEnabled(false);
                            deviceSelected = d;
                            sendIntent("deviceID");
                            mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_RESPONSE;
                            sendIntent("status");
                            return;
                        }
                    }

                    final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

                    AlertDialog.Builder alert = new AlertDialog.Builder(mContext);

                    final EditText edittext = new EditText(mContext);
                    alert.setTitle("Please enter the SN on the device: ");

                    alert.setView(edittext);

                    alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            String deviceSn = edittext.getText().toString();
                            if (!deviceSn.isEmpty()){
                                for(Device d : mDeviceIds){
                                    if(deviceSelectedName.equals(d.getMacAddress())){
                                        d.setsN(deviceSn);
                                        deviceSelected = d;
                                    }
                                }
                                progressBack.setVisibility(View.VISIBLE);
                                mLoadingProgress.setVisibility(View.VISIBLE);
                                connectText.setVisibility(View.VISIBLE);
                                recyclerView.setVisibility(View.INVISIBLE);
                                recyclerView.setEnabled(false);
                                devicesLabel.setVisibility(View.INVISIBLE);
                                searchButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                                searchButton.setTextColor(Color.rgb(204, 204, 204));
                                searchButton.setEnabled(false);
                                connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                                connectButton.setTextColor(Color.rgb(204, 204, 204));
                                connectButton.setEnabled(false);
                                mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_RESPONSE;
                                sendIntent("deviceID");
                                sendIntent("status");

                            }
                            else{
                                Toast.makeText(mContext, "SN cannot be empty", Toast.LENGTH_SHORT).show();
                            }
                            imm.toggleSoftInput(InputMethodManager.RESULT_HIDDEN,0);
                        }
                    });

                    alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.cancel();
                            imm.toggleSoftInput(InputMethodManager.RESULT_HIDDEN,0);
                        }
                    });

                    alert.show();
                }
            });

            devicesLabel = (TextView) findViewById(R.id.deviceLabel);
            devicesLabel.setText(Html.fromHtml(getString(R.string.devicesTitle)));
            connectText = (TextView) findViewById(R.id.connectText);
            connectText.setVisibility(View.INVISIBLE);
            progressBack = (ProgressBar) findViewById(R.id.progressBar);
            progressBack.setVisibility(View.INVISIBLE);
            mSearchProgress = (NewtonCradleLoading) findViewById(R.id.searchDeviceLoad);
            mSearchProgress.setLoadingColor(Color.BLACK);
            mSearchProgress.setVisibility(View.INVISIBLE);
            mSearchProgress.start();
            mLoadingProgress = (NewtonCradleLoading) findViewById(R.id.progressLoading);
            mLoadingProgress.setVisibility(View.INVISIBLE);
            mLoadingProgress.start();
            LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                    new IntentFilter("com.android.activity.WIFI_DATA_OUT"));
            sendIntent("status");

            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            recyclerView = (RecyclerView) this.findViewById(R.id.selection_list);
            recyclerView.setLayoutManager(layoutManager);
            makeWifiAdapter();

            sendIntent("sendDevices");
        }
        catch(Exception e){
            e.printStackTrace();
            Log.i("DeviceSelection", e.getLocalizedMessage());
        }
    }

    /**
     *
     * @return
     */
    public List<WifiSelectItem> generateItems(){

        List<WifiSelectItem> selectableItems = new ArrayList<>();
        selectableItems.clear();
        for(Device d: mDeviceIds){
            selectableItems.add(new WifiSelectItem(d.getMacAddress()));
        }

        return selectableItems;
    }

    /**
     *
     * @param selectableItem
     */
    @Override
    public void onItemSelected(SelectableWifi selectableItem) {
        if(adapter.getSelectedItems().isEmpty()) {
            connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
            connectButton.setTextColor(Color.rgb(204, 204, 204));
            deviceSelectedName = "";
            connectButton.setEnabled(false);
        }
        else {
            connectButton.setEnabled(true);
            connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
            connectButton.setTextColor(Color.rgb(255, 239, 204));
            deviceSelectedName = selectableItem.getDeviceID();
        }
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
                        //overridePendingTransition(R.anim.slide_out, R.anim.slide_in);
                        Intent brewIntent = new Intent(getApplicationContext(), CoffeeBrew.class);
                        intent.putExtra("selection", true);
                        mSearchProgress.setVisibility(View.INVISIBLE);
                        startActivity(brewIntent);
                    break;
                    }
                    case WAITING_FOR_USER:{
                        progressBack = (ProgressBar) findViewById(R.id.progressBar);
                        progressBack.setVisibility(View.INVISIBLE);
                        mLoadingProgress = (NewtonCradleLoading) findViewById(R.id.progressLoading);
                        mLoadingProgress.setVisibility(View.INVISIBLE);
                        recyclerView.setEnabled(true);
                        recyclerView.setVisibility(View.VISIBLE);
                        devicesLabel.setVisibility(View.VISIBLE);
                        connectText.setVisibility(View.INVISIBLE);
                        mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;
                        searchButton.setEnabled(true);
                        searchButton.setBackground(getDrawable(R.drawable.buttonstyle));
                        searchButton.setTextColor(Color.rgb(255, 239, 204));
                        if(!adapter.getSelectedItems().isEmpty()){
                            connectButton.setEnabled(true);
                            connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
                            connectButton.setTextColor(Color.rgb(255, 239, 204));
                        }
                        mSearchProgress.setVisibility(View.INVISIBLE);
                        generateItems();
                        break;
                    }
                    case WAITING_FOR_RESPONSE:{
                        progressBack.setVisibility(View.VISIBLE);
                        connectText.setVisibility(View.VISIBLE);
                        mLoadingProgress.setVisibility(View.VISIBLE);
                        mSearchProgress.setVisibility(View.INVISIBLE);
                        break;
                    }
                    case UNKNOWN:{
                        //Default state....don't know what to do with it.
                        break;
                    }
                    case NO_WIFI:{
                        intent = new Intent(getApplicationContext(), MainMenu.class);
                        intent.putExtra("noWifi", true);
                        intent.putExtra("flipper", viewFlipper.getDisplayedChild());
                        startActivity(intent);
                        finish();
                    }
                }
            }
            else if(intent.hasExtra("deviceIds")){
                ArrayList<Device> deviceIds;
                deviceIds = intent.getParcelableArrayListExtra("deviceIds");
                Log.i("Device Selection", "FUCK YOU");
                mDeviceIds.clear();
                for (Device d: deviceIds) {
                    Log.i("Device Selection", "Bitch!" + d.getMacAddress());
                    mDeviceIds.add(d);
                }
                makeWifiAdapter();
            }
            else if(intent.hasExtra("Failure")){
                for(Device d: mDeviceIds){
                    if(d.getMacAddress().equals(deviceSelected.getMacAddress())){
                        d.setsN("");
                        deviceSelected = d;
                    }
                }
            }
        }
    };

    private void makeWifiAdapter(){
        List<WifiSelectItem> selectableItems = generateItems();
        adapter = new WifiAdapter(this, selectableItems, false);
        recyclerView.setAdapter(adapter);
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
        else if(type.equals("deviceID")){
            intent.putExtra("deviceID", (Parcelable) deviceSelected);
            intent.setAction("com.android.activity.WIFI_DATA_IN");
        }
        else if(type.equals("sendDevices")){
            intent.putExtra("sendDevices","");
            intent.setAction("com.android.activity.WIFI_DATA_IN");
        }
        if(!intent.getExtras().isEmpty()) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    @Override
    public void onBackPressed(){
        //overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        Intent intent = new Intent(getApplicationContext(), MainMenu.class);
        intent.putExtra("selection", true);
        intent.putExtra("flipper", viewFlipper.getDisplayedChild());
        startActivity(intent);
        finish();
        mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;
        sendIntent("status");
        intent.putExtra("status",mConnectStatus.name());
        intent.setAction("com.android.activity.WIFI_DATA_OUT");
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle){
    }
}
