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
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.example.ndonaldson.beanster.data.Device;
import com.example.ndonaldson.beanster.fragments.LoginFragment;
import com.example.ndonaldson.beanster.R;
import com.example.ndonaldson.beanster.data.ThreadManager;
import com.example.ndonaldson.beanster.wifi.WifiRunner;
import com.victor.loading.newton.NewtonCradleLoading;

/**
 * This class starts the WifiRunner, which scans the network and either:
 * A) Connects to previous device with successful connection
 * B) Waits for user input to proceed
 *
 * Also displays current connection state and requires LAN connection
 * to proceed.
 */
public class MainMenu extends AppCompatActivity implements LoginFragment.OnFragmentInteractionListener {

    private WifiRunner.ConnectStatus connectStatus;
    private Button connectButton;
    private Button loginButton;
    private TextView connectingText;
    private NewtonCradleLoading cradle;
    private ProgressBar circle;
    private Animation fade_in, fade_out;
    private ViewFlipper viewFlipper;
    private ImageButton wifiStatus;
    private Context mContext;
    private String deviceHostName;
    private String devicePassword;
    private String deviceMacAddress;
    private Boolean isConnected;
    private Boolean closingActivity;
    private FrameLayout fragmentContainer;

    private static ThreadManager tm;
    private static WifiRunner wr;
    private static Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_DATA_OUT"));
        devicePassword = "";
        deviceMacAddress = "";
        deviceHostName = "";
        mContext = this;
        closingActivity = false;
        fragmentContainer = (FrameLayout) findViewById(R.id.fragmentContainer);
        connectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;

        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.i("MainMenu", ex.getLocalizedMessage());
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
            else if(savedInstanceState != null && savedInstanceState.containsKey("flipper")) {
                viewFlipper.setDisplayedChild(savedInstanceState.getInt("flipper",0));
            }

            viewFlipper.startFlipping();
        }
        catch(Exception e){
            e.printStackTrace();
            Log.i("MainMenu", e.getLocalizedMessage());
        }

        cradle = (NewtonCradleLoading) findViewById(R.id.newton_cradle_loading);
        circle = (ProgressBar) findViewById(R.id.progressBar);
        connectingText = (TextView) findViewById(R.id.connectText);
        wifiStatus = (ImageButton) findViewById(R.id.wifiStatus);
        getWindow().setExitTransition(new Slide());

        connectButton = (Button) findViewById(R.id.connectButton);
        connectButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                sendIntent(WifiRunner.ConnectStatus.WAITING_FOR_USER.name(), "status");
                Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                deviceIntent.putExtra("flipper",viewFlipper.getDisplayedChild());
                deviceIntent.putExtra("connected", isConnected);
                startActivity(deviceIntent);
            }
        });



        loginButton = (Button) findViewById(R.id.loginButton);

        SharedPreferences sharedPreferences = getSharedPreferences("beanster", MODE_PRIVATE);
        if(sharedPreferences.contains("currentUser")){
            if(!sharedPreferences.getString("currentUser", "").isEmpty()){
                loginButton.setText(sharedPreferences.getString("currentUser", ""));
            }
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!loginButton.getText().toString().equals("Login")) {
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

        if(getIntent() != null && getIntent().hasExtra("connected")){
            isConnected = (Boolean) getIntent().getExtras().get("connected");
            if(isConnected) wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
            else wifiStatus.setBackground(getApplication().getDrawable(R.drawable.nowifi));
        } else{
            isConnected = false;
            wifiStatus.setBackground(getApplication().getDrawable(R.drawable.nowifi));
        }

        if(getIntent() != null && getIntent().hasExtra("selection")){
            connectButton.setEnabled(true);
            connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
            connectButton.setTextColor(Color.rgb(255, 239, 204));

            loginButton.setEnabled(true);
            loginButton.setBackground(getDrawable(R.drawable.buttonstyle));
            loginButton.setTextColor(Color.rgb(255, 239, 204));

            cradle.setVisibility(View.INVISIBLE);
            circle.setVisibility(View.INVISIBLE);
            connectingText.setVisibility(View.INVISIBLE);
        }


        else {
            connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
            connectButton.setTextColor(Color.rgb(204, 204, 204));
            connectButton.setEnabled(false);

            loginButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
            loginButton.setTextColor(Color.rgb(204, 204, 204));
            loginButton.setEnabled(false);

            cradle.setVisibility(View.VISIBLE);
            circle.setVisibility(View.VISIBLE);
            connectingText.setVisibility(View.VISIBLE);
            wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.nowifi));
            connectingText.setText("Loading....");
            cradle.start();

            mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tm = new ThreadManager(mContext);
                    wr = new WifiRunner(mContext);
                    tm.runInBackground(wr, 1000);
                }
            }, 1000);
        }
    }

    /**
     * Messages received from WifiRunner:
     *
     * CONNECTED: Happen from successful connection to previous device with successful communication.
     *
     * UNKNOWN: UNDEFINED
     *
     * NO_WIFI: Warns user to be connected to wifi and prevents further process.
     *
     * WAITING_FOR_USER: Unsuccessful reconnection to previous device with communication. Wait for user input.
     *
     * CONNECT_TO_LAST: Trying to connect to last device, display proper indications.
     *
     * SEARCHING: Scanning wifi network for devices with certain mac address
     */
    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if(intent.hasExtra("status")) {
                String status = intent.getStringExtra("status");
                Log.d("MainMenu", "wifiStatusReceiver got message: " + status);
                connectStatus = WifiRunner.ConnectStatus.valueOf(status);
                switch (connectStatus) {
                    case CONNECTED: {
                        wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
                        isConnected = true;
                        Intent brewIntent = new Intent(getApplicationContext(), CoffeeBrew.class);
                        brewIntent.putExtra("passWord", devicePassword);
                        closingActivity = true;
                        startActivity(brewIntent);
                        break;
                    }
                    case UNKNOWN: {
                        //Default state....don't know what to do with it.
                        break;
                    }
                    case NO_WIFI: {

                        if(isConnected){
                            Toast toast = Toast.makeText(context, "Lost connection to device.....", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        } else{
                            Toast toast = Toast.makeText(context, "Can't connect to previous device...", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }

                        wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.nowifi));
                        isConnected = false;
                        connectButton.setEnabled(true);
                        connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
                        connectButton.setTextColor(Color.rgb(255, 239, 204));

                        loginButton.setEnabled(true);
                        loginButton.setBackground(getDrawable(R.drawable.buttonstyle));
                        loginButton.setTextColor(Color.rgb(255, 239, 204));

                        cradle.setVisibility(View.INVISIBLE);
                        circle.setVisibility(View.INVISIBLE);
                        connectingText.setVisibility(View.INVISIBLE);
                        break;
                    }
                    case WAITING_FOR_USER: {
                        if(closingActivity) break;
                        connectButton.setEnabled(true);
                        connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
                        connectButton.setTextColor(Color.rgb(255, 239, 204));

                        loginButton.setEnabled(true);
                        loginButton.setBackground(getDrawable(R.drawable.buttonstyle));
                        loginButton.setTextColor(Color.rgb(255, 239, 204));

                        cradle.setVisibility(View.INVISIBLE);
                        circle.setVisibility(View.INVISIBLE);
                        connectingText.setVisibility(View.INVISIBLE);
                        break;
                    }

                    case CONNECT_TO_LAST: {
                        connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                        connectButton.setTextColor(Color.rgb(204, 204, 204));
                        connectButton.setEnabled(false);

                        loginButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                        loginButton.setTextColor(Color.rgb(204, 204, 204));
                        loginButton.setEnabled(false);

                        cradle.setVisibility(View.VISIBLE);
                        circle.setVisibility(View.VISIBLE);
                        connectingText.setVisibility(View.VISIBLE);
                        connectingText.setText("Trying to connect....");
                        cradle.start();
                        break;
                    }

                    case SEARCHING: {
                        connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                        connectButton.setTextColor(Color.rgb(204, 204, 204));
                        connectButton.setEnabled(false);

                        loginButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                        loginButton.setTextColor(Color.rgb(204, 204, 204));
                        loginButton.setEnabled(false);

                        cradle.setVisibility(View.VISIBLE);
                        circle.setVisibility(View.VISIBLE);
                        connectingText.setVisibility(View.VISIBLE);
                        connectingText.setText("Scanning network....");
                        cradle.start();
                        break;
                    }
                }
            }

            else if(intent.hasExtra("lastDevice")){
                Device d = intent.getParcelableExtra("lastDevice");
                deviceHostName = d.getHostName();
                deviceMacAddress = d.getMacAddress();
                devicePassword = d.getPassWord();
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

    /**
     * Closes application
     */
    @Override
    public void onBackPressed(){
        tm.clearThreads();
        int pid = android.os.Process.myPid();
        android.os.Process.killProcess(pid);
    }

    /**
     * Keep track of image we are currently on
     * @param bundle
     */
    @Override
    public void onSaveInstanceState(Bundle bundle){
        bundle.putInt("flipper", viewFlipper.getDisplayedChild());
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
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    /**
     * Login fragment
     */
    private void openFragment(){
        LoginFragment fragment = LoginFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_from_top, R.anim.slide_to_top, R.anim.slide_from_top, R.anim.slide_to_top);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.add(R.id.fragmentContainer, fragment, "LOGIN_FRAGMENT").commit();
    }

    /**
     * Received info from login fragment
     * @param sendBackUsername
     */
    @Override
    public void onFragmentInteraction(String sendBackUsername) {
        if(!sendBackUsername.isEmpty())loginButton.setText(sendBackUsername);
        getSupportFragmentManager().popBackStack();
    }
}
