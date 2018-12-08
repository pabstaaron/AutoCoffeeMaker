package com.example.ndonaldson.beanster.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.ndonaldson.beanster.data.Device;
import com.example.ndonaldson.beanster.fragments.LoginFragment;
import com.example.ndonaldson.beanster.R;
import com.example.ndonaldson.beanster.wifi.SelectableWifi;
import com.example.ndonaldson.beanster.wifi.WifiAdapter;
import com.example.ndonaldson.beanster.wifi.WifiRunner;
import com.example.ndonaldson.beanster.wifi.WifiSelectItem;
import com.example.ndonaldson.beanster.wifi.WifiViewHolder;
import com.victor.loading.newton.NewtonCradleLoading;

import java.util.ArrayList;
import java.util.List;

public class DeviceSelection extends AppCompatActivity implements WifiViewHolder.OnItemSelectedListener, LoginFragment.OnFragmentInteractionListener {

    private WifiRunner.ConnectStatus mConnectStatus;
    private ArrayList<Device> mDeviceIds;
    private Button cancelButton;
    private Button connectButton;
    private Button loginButton;
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
    private ImageButton wifiStatus;
    private Boolean isConnected;
    private Boolean closingActivity;
    private Boolean leavingBack;
    private FrameLayout fragmentContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        try {
            leavingBack = false;
            mContext = this;
            deviceSelectedName = "";
            closingActivity = false;
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_device_select);

            Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Log.i("DeviceSelection", ex.getLocalizedMessage());
                    Intent mStartActivity = new Intent(mContext, main.class);
                    mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
                    PendingIntent mPendingIntent = PendingIntent.getActivity(mContext, 0, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                    AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                    mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, mPendingIntent);
                    System.exit(0);
                }
            });

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

            mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;
            mDeviceIds = new ArrayList<>();
            cancelButton = (Button) findViewById(R.id.deviceSelectCancel);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    leavingBack = true;
                    Intent intent = new Intent(getApplicationContext(), MainMenu.class);
                    intent.putExtra("selection", true);
                    intent.putExtra("flipper", viewFlipper.getDisplayedChild());
                    intent.putExtra("connected", isConnected);
                    startActivity(intent);
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
                }
            });
            connectButton = (Button) findViewById(R.id.deviceConnect);
            connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
            connectButton.setTextColor(Color.rgb(204, 204, 204));
            connectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    for(Device d : mDeviceIds){
                        Log.i("DeviceSelection", "d.MacAddress: " + d.getMacAddress() + ", d.password: " + d.getPassWord() + ", deviceSelectedName: " + deviceSelectedName);
                        if(d.getHostName().equals(deviceSelectedName) && !d.getPassWord().isEmpty()){
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
                            String devicePassword = edittext.getText().toString();
                            if (!devicePassword.isEmpty()){
                                for(Device d : mDeviceIds){
                                    if(deviceSelectedName.equals(d.getHostName())){
                                        d.setPassWord(devicePassword);
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
                                Toast toast = Toast.makeText(mContext, "SN cannot be empty...", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();                            }
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
            connectButton.setEnabled(false);

            loginButton = (Button) findViewById(R.id.loginButton2);
            loginButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!loginButton.getText().equals("Login")) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                        builder.setCancelable(false);
                        builder.setMessage("Would you like to logout?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SharedPreferences sharedPreferences = getSharedPreferences("beanster", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("currentUser", "").apply();
                                loginButton.setText("Login");

                                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                builder.setMessage("Would you like to login with a new user?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        openFragment();
                                    }
                                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                }).show();
                            }
                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).show();
                    }
                    else{
                        openFragment();
                    }
                }
            });

            SharedPreferences sharedPreferences = getSharedPreferences("beanster", MODE_PRIVATE);
            if(sharedPreferences.contains("currentUser")){
                if(!sharedPreferences.getString("currentUser", "").isEmpty()){
                    loginButton.setText(sharedPreferences.getString("currentUser", ""));
                }
            }

            fragmentContainer = (FrameLayout) findViewById(R.id.fragmentContainer2);

            devicesLabel = (TextView) findViewById(R.id.deviceLabel);
            devicesLabel.setText(Html.fromHtml(getString(R.string.devicesTitle)));
            connectText = (TextView) findViewById(R.id.connectText);
            connectText.setVisibility(View.INVISIBLE);
            progressBack = (ProgressBar) findViewById(R.id.progressBar);
            progressBack.setVisibility(View.INVISIBLE);
            mSearchProgress = (NewtonCradleLoading) findViewById(R.id.searchDeviceLoad);
            mSearchProgress.setVisibility(View.INVISIBLE);
            mSearchProgress.setLoadingColor(Color.BLACK);
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

            wifiStatus = (ImageButton) findViewById(R.id.wifiStatus2);


            if(getIntent() != null && getIntent().hasExtra("connected")){
                isConnected = (Boolean) getIntent().getExtras().get("connected");
                if(isConnected) wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
                else wifiStatus.setBackground(getApplication().getDrawable(R.drawable.nowifi));
            } else{
                isConnected = false;
                wifiStatus.setBackground(getApplication().getDrawable(R.drawable.nowifi));
            }

            sendIntent("sendLast");
            sendIntent("sendDevices");
        }
        catch(Exception e){
            e.printStackTrace();
            Log.i("DeviceSelection", e.getLocalizedMessage());
        }
    }

    /**
     * Create all the items in the recyclerView based on the raspberry PI's on the LAN found by WifiRunner scan
     * @return
     */
    public List<WifiSelectItem> generateItems(){

        List<WifiSelectItem> selectableItems = new ArrayList<>();
        selectableItems.clear();
        for(Device d: mDeviceIds){
            selectableItems.add(new WifiSelectItem(d.getHostName()));
        }

        return selectableItems;
    }

    /**
     * Allow/Disallow use of connect button when an item is selected.
     * @param selectableItem
     */
    @Override
    public void onItemSelected(SelectableWifi selectableItem) {
        if(adapter != null && adapter.getSelectedItems().isEmpty()) {
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
     * Messages received from WifiRunner:
     *
     * CONNECTED: Happen from successful connection to requested device.
     *
     * UNKNOWN: UNDEFINED
     *
     * NO_WIFI: Warns user to be connected to wifi and kicks them to main screen.
     *
     * WAITING_FOR_USER: Waiting for user input
     *
     * WAITING_FOR_RESPONSE: Waiting for connection response on selected device
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
                        Intent brewIntent = new Intent(getApplicationContext(), CoffeeBrew.class);
                        brewIntent.putExtra("selection", true);
                        Log.i("DeviceSelection", "Starting brewActivity with macAddress " + deviceSelected.getMacAddress() + ", password: " + deviceSelected.getPassWord() + ", and hostName: " + deviceSelected.getHostName());
                        brewIntent.putExtra("passWord", deviceSelected.getPassWord());
                        wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
                        mSearchProgress.setVisibility(View.INVISIBLE);
                        isConnected = true;
                        closingActivity = true;
                        startActivity(brewIntent);
                    break;
                    }
                    case WAITING_FOR_USER:{
                        if(closingActivity) break;
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
                        if(adapter != null && !adapter.getSelectedItems().isEmpty()){
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
                        wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.nowifi));
                        Toast toast = Toast.makeText(context, "Lost connection to device.....", Toast.LENGTH_LONG);
                        //mDeviceIds.clear();
                        deviceSelected = null;
                        deviceSelectedName = "";
                        isConnected = false;
                        connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                        connectButton.setTextColor(Color.rgb(204, 204, 204));
                        connectButton.setEnabled(false);
                        makeWifiAdapter(deviceSelected);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }
            }
            else if(intent.hasExtra("deviceIds")){
                Log.i("DeviceSelection", "DEVICEIDS INTENT SENT!");
                ArrayList<Device> deviceIds;
                deviceIds = intent.getParcelableArrayListExtra("deviceIds");
                mDeviceIds.clear();
                for (Device d: deviceIds) {
                    Log.i("DeviceSelection", String.format("d.MacAddress: %s, d.password: %s, d.hostName: %s", d.getMacAddress(), d.getPassWord(), d.getHostName()));
                    mDeviceIds.add(d);
                }

                makeWifiAdapter(deviceSelected);
            }
            else if(intent.hasExtra("Failure") || intent.hasExtra("badRequest")) {
                String previousPassword = "";
                for (Device d : mDeviceIds) {
                    Log.i("DeviceSelection", String.format("d.MacAddress: %s, d.password: %s, d.hostName: %s", d.getMacAddress(), d.getPassWord(), d.getHostName()));
                    if (deviceSelected != null && d.getMacAddress().equals(deviceSelected.getMacAddress())) {
                        Log.i("DeviceSelection", String.format("deviceSelected.MacAddress: %s, deviceSelected.password: %s, deviceSelected.hostName: %s", deviceSelected.getMacAddress(), deviceSelected.getPassWord(), deviceSelected.getHostName()));
                        previousPassword = d.getPassWord();
                        d.setPassWord("");
                        deviceSelected = d;
                        Log.i("DeviceSelection", String.format("AFTER BITCH!!! deviceSelected.MacAddress: %s, deviceSelected.password: %s, deviceSelected.hostName: %s", deviceSelected.getMacAddress(), deviceSelected.getPassWord(), deviceSelected.getHostName()));
                    }
                }
                String message = "";
                if(intent.hasExtra("Failure")) {
                    message = "Failure to get response from device...";
                }
                else if(!previousPassword.isEmpty())
                    message = "password " + previousPassword + " for device " + deviceSelected.getMacAddress() + " is not correct...";
                Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
            else if(intent.hasExtra("lastDevice")){
                deviceSelected = intent.getParcelableExtra("lastDevice");
                if(deviceSelected != null){
                    deviceSelectedName = deviceSelected.getHostName();
                }
            }
        }
    };

    /**
     * creates new recyclerview of raspberryPI devices on network.
     */
    private void makeWifiAdapter(Device device){
        if(device != null){
            connectButton.setEnabled(true);
            connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
            connectButton.setTextColor(Color.rgb(255, 239, 204));
        }
        List<WifiSelectItem> selectableItems = generateItems();
        adapter = new WifiAdapter(this, selectableItems, false, deviceSelectedName);
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
        else if(type.equals("sendLast")){
            intent.putExtra("sendLast", "");
            intent.setAction("com.android.activity.WIFI_DATA_IN");
        }
        if(!intent.getExtras().isEmpty()) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }

    /**
     * Go back to main screen
     */
    @Override
    public void onBackPressed(){
        leavingBack = true;
        Intent intent = new Intent(getApplicationContext(), MainMenu.class);
        intent.putExtra("selection", true);
        intent.putExtra("flipper", viewFlipper.getDisplayedChild());
        intent.putExtra("connected", isConnected);
        startActivity(intent);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiStatusReceiver);
    }

    /**
     * @param bundle
     */
    @Override
    public void onSaveInstanceState(Bundle bundle){
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiStatusReceiver);
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiStatusReceiver);
        onStartNewActivity();
    }

    protected void onStartNewActivity() {
        if(leavingBack) overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        else overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    private void openFragment(){
        LoginFragment fragment = LoginFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_from_top, R.anim.slide_to_top, R.anim.slide_from_top, R.anim.slide_to_top);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.add(R.id.fragmentContainer2, fragment, "LOGIN_FRAGMENT").commit();
    }

    @Override
    public void onFragmentInteraction(String sendBackUsername) {
        if(!sendBackUsername.isEmpty())loginButton.setText(sendBackUsername);
        getSupportFragmentManager().popBackStack();
    }
}
