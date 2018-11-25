package com.example.ndonaldson.beanster.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ViewFlipper;

import com.example.ndonaldson.beanster.R;
import com.example.ndonaldson.beanster.data.UserData;
import com.google.gson.Gson;
import com.victor.loading.newton.NewtonCradleLoading;

import java.util.HashMap;

/**
 *@author Nathan Donaldson
 * This class is the main screen for Beanster, checking permissions and loading basic main menu.
 */
public class main extends AppCompatActivity {

    private Animation fade_in, fade_out;
    private ViewFlipper viewFlipper;
    private Button connectButton;
    private SharedPreferences sharedPreferences;


    private static final int WRITE_EXTERNAL_STORAGE_CODE = 1;
    private static final int READ_EXTERNAL_STORAGE_CODE = 2;
    private static final int INTERNET_CODE = 3;
    private static final int ACCESS_NETWORK_STATE_CODE = 4;
    private static final int ACCESS_WIFI_STATE_CODE = 5;
    private static final int CHANGE_WIFI_STATE_CODE = 6;

    private static final String REQUEST_WRITE_EXTERNAL_STORAGE= Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String REQUEST_READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String REQUEST_INTERNET = Manifest.permission.INTERNET;
    private static final String REQUEST_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;
    private static final String REQUEST_ACCESS_WIFI_STATE = Manifest.permission.ACCESS_WIFI_STATE;
    private static final String REQUEST_CHANGE_WIFI_STATE = Manifest.permission.CHANGE_WIFI_STATE;

    private NewtonCradleLoading cradle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.i("Main", ex.getLocalizedMessage());
                Intent mStartActivity = new Intent(getApplicationContext(), main.class);
                mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, mPendingIntent);
                System.exit(0);
            }
        });

        connectButton = (Button) findViewById(R.id.connectButtonMain);
        connectButton.setVisibility(View.INVISIBLE);


        sharedPreferences = getSharedPreferences("beanster", Context.MODE_PRIVATE);

        try {
            viewFlipper = (ViewFlipper) this.findViewById(R.id.backgroundViewMain);
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

        cradle = (NewtonCradleLoading) findViewById(R.id.newton_cradle_loading_main);
        cradle.start();

        checkWrite();
    }

    /**
     * Finished permissions, start the program
     */
    private void begin(){
        Intent intent = new Intent(this, MainMenu.class);
        if(!sharedPreferences.contains("userData")){
            HashMap<String, UserData> data = new HashMap<>();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Gson gson = new Gson();
            String json = gson.toJson(data);
            editor.putString("userData", json).commit();
        }
        startActivity(intent);
        finish();
    }

    /**
     * Let user know something weird happened
     */
    private void error(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getApplicationContext());
        alertBuilder.setCancelable(false);
        alertBuilder.setTitle("Error");
        alertBuilder.setMessage("An unexpected error occured");
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkWrite();
            }
        });
        AlertDialog dialog = alertBuilder.create();
        dialog.show();
    }

    /**
     * Let user know the application needs all the permissions to function
     */
    private void checkYourPriveledge(){
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getApplicationContext());
        alertBuilder.setCancelable(false);
        alertBuilder.setTitle("Permissions Error");
        alertBuilder.setMessage("Need all permissions for app to continue");
        alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkWrite();
            }
        });
        AlertDialog dialog = alertBuilder.create();
        dialog.show();
    }

    /**
     *Check write permissions for external storage
     */
    private void checkWrite(){
        Log.i("Main", "Request_Write_External_Storage: " + this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_WRITE_EXTERNAL_STORAGE));
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_CODE, this);
        }
        else checkRead();
    }

    /**
     * Check read permissions for external storage
     */
    private void checkRead(){
        Log.i("Main", "Request_Read_External_Storage: " + this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_READ_EXTERNAL_STORAGE));
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_CODE, this);
        }
        else checkInternet();
    }

    /**
     * Check internet permissions
     */
    private void checkInternet(){
        Log.i("Main", "Request_Internet: " + this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_INTERNET));
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_INTERNET, INTERNET_CODE, this);
        }
        else checkNetwork();
    }

    /**
     * Check network permissions
     */
    private void checkNetwork(){
        Log.i("Main", "Request_Access_Network_State: " + this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_ACCESS_NETWORK_STATE));
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_ACCESS_NETWORK_STATE, ACCESS_NETWORK_STATE_CODE, this);
        }
        else checkWifiState();
    }

    /**
     * Check wifi state permissions
     */
    private void checkWifiState(){
        Log.i("Main", "Request_Access_Wifi_State: " + this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_ACCESS_WIFI_STATE));
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_ACCESS_WIFI_STATE, ACCESS_WIFI_STATE_CODE, this);
        }
        else checkChangeWifi();
    }

    /**
     * Check wifi state permissions
     */
    private void checkChangeWifi(){
        Log.i("Main", "Request_Access_Wifi_State: " + this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_CHANGE_WIFI_STATE));
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_CHANGE_WIFI_STATE, CHANGE_WIFI_STATE_CODE, this);
        }
        else begin();
    }


    /**
     * @param permission
     * @param requestCode
     * Requests permission from user for a specific permission
     */
    private void requestPermission(final String permission, final int requestCode, final Activity activity){
        int result = -1;
        Log.i("Main", String.format("Checking permission %s with requestCode %d", permission, requestCode));
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
            alertBuilder.setCancelable(true);

                if (permission.equals(REQUEST_WRITE_EXTERNAL_STORAGE)){
                    alertBuilder.setTitle("External Storage permission necessary");
                    alertBuilder.setMessage("Permission needed to save important application data");
                }
                else if(permission.equals(REQUEST_READ_EXTERNAL_STORAGE)){
                    alertBuilder.setTitle("External Storage permission necessary");
                    alertBuilder.setMessage("Permission needed to read important application data");
                }
                else if (permission.equals(REQUEST_INTERNET)){
                    alertBuilder.setTitle("Internet permission necessary");
                    alertBuilder.setMessage("Permission needed to connect with device server");
                }
                else if (permission.equals(REQUEST_ACCESS_NETWORK_STATE)){
                    alertBuilder.setTitle("Network access permission necessary");
                    alertBuilder.setMessage("Permission needed to connect with device");
                }
                else if (permission.equals(REQUEST_ACCESS_WIFI_STATE)){
                    alertBuilder.setTitle("Wifi state access permission necessary");
                    alertBuilder.setMessage("Permission needed to connect with device");
                }
                else if (permission.equals(REQUEST_CHANGE_WIFI_STATE)){
                    alertBuilder.setTitle("Wifi change access permission necessary");
                    alertBuilder.setMessage("Permission needed to connect with device");
                }
                else{
                    Log.i("Main", "Unexpected Permission Request");
                    error();
                }

            alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                int result = -1;
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    result = activity.checkPermission(permission, requestCode, 0);
                    onRequestPermissionsResult(requestCode, result);
                }
            });
            alertBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (permission){
                        case REQUEST_WRITE_EXTERNAL_STORAGE:
                        case REQUEST_READ_EXTERNAL_STORAGE:
                        case REQUEST_INTERNET:
                        case REQUEST_ACCESS_NETWORK_STATE:
                        case REQUEST_ACCESS_WIFI_STATE:
                        case REQUEST_CHANGE_WIFI_STATE:{
                            checkYourPriveledge();
                            break;
                        }
                        default: Log.i("Main", "Unexpected Permission Request");
                    }
                }
            });

            AlertDialog dialog = alertBuilder.create();
            dialog.show();
        }



    /**
     *
     * @param requestCode
     * @param result
     * Acts on the permission request result
     */
    public void onRequestPermissionsResult(int requestCode, int result) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_CODE: {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    checkRead();
                } else {
                    checkYourPriveledge();
                }
                return;
            }
            case READ_EXTERNAL_STORAGE_CODE: {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    checkInternet();
                } else {
                    checkYourPriveledge();
                }
                break;
            }
            case INTERNET_CODE: {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    checkNetwork();
                } else {
                    checkYourPriveledge();
                }
                break;
            }
            case ACCESS_NETWORK_STATE_CODE: {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    checkWifiState();
                } else {
                     checkYourPriveledge();
                }
                break;
            }
            case ACCESS_WIFI_STATE_CODE: {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    checkChangeWifi();
                } else {
                    checkYourPriveledge();
                }
                break;
            }
            case CHANGE_WIFI_STATE_CODE: {
                if (result == PackageManager.PERMISSION_GRANTED) {
                    begin();
                } else {
                    checkYourPriveledge();
                }
                break;
            }
            default:{
                Log.i("Main", "Unexpected Permission Request");
                error();
                break;
            }
        }
    }
}


