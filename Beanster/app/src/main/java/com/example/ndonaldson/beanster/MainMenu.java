package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.victor.loading.newton.NewtonCradleLoading;

public class MainMenu extends AppCompatActivity {

    private WifiRunner.ConnectStatus connectStatus;
    private Button connectButton;
    private TextView connectingText;
    private NewtonCradleLoading cradle;
    private ProgressBar circle;
    private Animation fade_in, fade_out;
    private ViewFlipper viewFlipper;
    private ImageButton wifiStatus;
    private Context mContext;

    private static ThreadManager tm;
    private static WifiRunner wr;
    private static Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        mContext = this;
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

        connectButton = (Button) findViewById(R.id.connectButton);
        cradle = (NewtonCradleLoading) findViewById(R.id.newton_cradle_loading);
        circle = (ProgressBar) findViewById(R.id.progressBar);
        connectingText = (TextView) findViewById(R.id.connectText);
        wifiStatus = (ImageButton) findViewById(R.id.wifiStatus);

        wifiStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        connectButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                sendIntent(WifiRunner.ConnectStatus.WAITING_FOR_USER.name(), "status");
                Intent brewIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                brewIntent.putExtra("flipper",viewFlipper.getDisplayedChild());
                startActivity(brewIntent);
                finish();
            }
        });

        if(getIntent() != null && getIntent().hasExtra("selection")){
            connectButton.setEnabled(true);
            connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
            connectButton.setTextColor(Color.rgb(255, 239, 204));
            wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
            cradle.setVisibility(View.INVISIBLE);
            circle.setVisibility(View.INVISIBLE);
            connectingText.setVisibility(View.INVISIBLE);
        }

        else if(getIntent() != null && getIntent().hasExtra("noWifi")){
            connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
            connectButton.setTextColor(Color.rgb(204, 204, 204));
            connectButton.setEnabled(false);
            wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.nowifi));
            cradle.setVisibility(View.INVISIBLE);
            circle.setVisibility(View.INVISIBLE);
            connectingText.setVisibility(View.INVISIBLE);
            connectStatus = WifiRunner.ConnectStatus.NO_WIFI;
        }

        else {
            connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
            connectButton.setTextColor(Color.rgb(204, 204, 204));
            connectButton.setEnabled(false);
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
                    tm = new ThreadManager();
                    wr = new WifiRunner(mContext);
                    tm.runInBackground(wr, 1000);
                }
            }, 1000);
        }
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_DATA_OUT"));
    }

    private BroadcastReceiver wifiStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Extract data included in the Intent
            if(!intent.hasExtra("status")) return;
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
                case NO_WIFI:{
                    connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                    connectButton.setTextColor(Color.rgb(204, 204, 204));
                    connectButton.setEnabled(false);
                    cradle.setVisibility(View.INVISIBLE);
                    wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.nowifi));
                    circle.setVisibility(View.INVISIBLE);
                    connectingText.setVisibility(View.INVISIBLE);
                    Toast toast = Toast.makeText(context, "Application requires wireless connection", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                    break;
                }
                case WAITING_FOR_USER:{
                    connectButton.setEnabled(true);
                    connectButton.setBackground(getDrawable(R.drawable.buttonstyle));
                    connectButton.setTextColor(Color.rgb(255, 239, 204));
                    wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
                    cradle.setVisibility(View.INVISIBLE);
                    circle.setVisibility(View.INVISIBLE);
                    connectingText.setVisibility(View.INVISIBLE);
                    break;
                }

                case CONNECT_TO_LAST:{
                    connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                    connectButton.setTextColor(Color.rgb(204, 204, 204));
                    connectButton.setEnabled(false);
                    cradle.setVisibility(View.VISIBLE);
                    circle.setVisibility(View.VISIBLE);
                    connectingText.setVisibility(View.VISIBLE);
                    wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
                    connectingText.setText("Trying to connect....");
                    cradle.start();
                    break;
                }

                case SEARCHING:{
                    connectButton.setBackground(getDrawable(R.drawable.buttonstyledisable));
                    connectButton.setTextColor(Color.rgb(204, 204, 204));
                    connectButton.setEnabled(false);
                    cradle.setVisibility(View.VISIBLE);
                    circle.setVisibility(View.VISIBLE);
                    connectingText.setVisibility(View.VISIBLE);
                    connectingText.setText("Scanning network....");
                    wifiStatus.setBackground(getApplicationContext().getDrawable(R.drawable.wifion));
                    cradle.start();
                    break;
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
    public void onBackPressed(){
        finishAndRemoveTask();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle){
        bundle.putInt("flipper", viewFlipper.getDisplayedChild());
    }
}
