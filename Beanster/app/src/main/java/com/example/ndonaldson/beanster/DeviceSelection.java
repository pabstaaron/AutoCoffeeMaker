package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class DeviceSelection extends AppCompatActivity implements WifiViewHolder.OnItemSelectedListener{

    private WifiRunner.ConnectStatus mConnectStatus;
    private ArrayList<String> mDeviceIds;
    private Button cancelButton;
    private Button connectButton;
    private Button newDeviceButton;
    private TextView connectText;
    private NewtonCradleLoading mLoadingProgress;
    private String deviceSelected;
    private ViewFlipper viewFlipper;
    private String mDeviceText;
    private Context context;
    private ProgressBar progressBack;
    private Animation fade_in, fade_out;
    RecyclerView recyclerView;
    WifiAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            context = this;
            deviceSelected = "";
            mDeviceText = "";
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
                Log.i("MainMenu", e.getLocalizedMessage());
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
                    sendIntent(WifiRunner.ConnectStatus.WAITING_FOR_USER.name(), "status");
                }
            });
            connectButton = (Button) findViewById(R.id.deviceConnect);
            connectButton.setEnabled(false);
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendIntent("deviceID", deviceSelected);
                    sendIntent("status", WifiRunner.ConnectStatus.WAITING_FOR_RESPONSE.name());
                }
            });
            newDeviceButton = (Button) findViewById(R.id.newDevice);
            newDeviceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);

                    AlertDialog.Builder alert = new AlertDialog.Builder(context);

                    final EditText edittext = new EditText(context);
                    alert.setTitle("Please enter the SN on the device: ");

                    alert.setView(edittext);

                    alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dialog.dismiss();
                            mDeviceText = edittext.getText().toString();
                            mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_RESPONSE;
                            sendIntent(mDeviceText, "deviceID");
                            sendIntent(mConnectStatus.name(), "status");
                            progressBack.setVisibility(View.VISIBLE);
                            mLoadingProgress.setVisibility(View.VISIBLE);
                            connectText.setVisibility(View.VISIBLE);
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
            connectText = (TextView) findViewById(R.id.connectText);
            connectText.setVisibility(View.GONE);
            progressBack = (ProgressBar) findViewById(R.id.progressBar);
            progressBack.setVisibility(View.GONE);
            mLoadingProgress = (NewtonCradleLoading) findViewById(R.id.progressLoading);
            mLoadingProgress.setVisibility(View.GONE);
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
            e.printStackTrace();
            Log.i("DeviceSelection", e.getLocalizedMessage());
        }
    }

    /**
     *
     * @return
     */
    public List<WifiSelectItem> generateItems(){

//        List<WifiSelectItem> selectableItems = new ArrayList<>();
//        for(String s: mDeviceIds){
//            selectableItems.add(new WifiSelectItem(s));
//        }
//
//        return selectableItems;
        List<WifiSelectItem> selectableItems = new ArrayList<>();
        for(int i = 0; i < 101; i++)
        selectableItems.add(new WifiSelectItem("Machine" + i));

        return selectableItems;
    }

    /**
     *
     * @param selectableItem
     */
    @Override
    public void onItemSelected(SelectableWifi selectableItem) {
        if(adapter.getSelectedItems().isEmpty()) {
            connectButton.setEnabled(false);
            deviceSelected = "";
        }
        else {
            connectButton.setEnabled(true);
            deviceSelected = selectableItem.getDeviceID();
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
                        startActivity(brewIntent);
                    break;
                    }
                    case SEARCHING:{
                        progressBack = (ProgressBar) findViewById(R.id.progressBar);
                        progressBack.setVisibility(View.GONE);
                        mLoadingProgress = (NewtonCradleLoading) findViewById(R.id.progressLoading);
                        mLoadingProgress.setVisibility(View.GONE);
                        connectText.setVisibility(View.GONE);
                        mConnectStatus = WifiRunner.ConnectStatus.SEARCHING;
                        generateItems();
                        break;
                    }
                    case WAITING_FOR_RESPONSE:{
                        progressBack.setVisibility(View.VISIBLE);
                        connectText.setVisibility(View.VISIBLE);
                        mLoadingProgress.setVisibility(View.VISIBLE);
                        break;
                    }
                    case UNKNOWN:{
                        //Default state....don't know what to do with it.
                        break;
                    }
                    case WAITING_FOR_USER:{
                        progressBack = (ProgressBar) findViewById(R.id.progressBar);
                        progressBack.setVisibility(View.GONE);
                        connectText.setVisibility(View.GONE);
                        mLoadingProgress.setVisibility(View.GONE);
                        mConnectStatus = WifiRunner.ConnectStatus.SEARCHING;
                        sendIntent(WifiRunner.ConnectStatus.SEARCHING.name(), "status");
                        mConnectStatus = WifiRunner.ConnectStatus.SEARCHING;
                        break;
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
