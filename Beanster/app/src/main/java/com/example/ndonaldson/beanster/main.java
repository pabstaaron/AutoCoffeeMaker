package com.example.ndonaldson.beanster;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.PermissionRequest;

import java.security.Permission;

/**
 *@author Nathan Donaldson
 * This class is the main screen for Beanster, checking permissions and loading basic main menu.
 */
public class main extends AppCompatActivity {

    private static ThreadManager tm;
    private static WifiRunner wr;

    private static final int WRITE_EXTERNAL_STORAGE_CODE = 1;
    private static final int READ_EXTERNAL_STORAGE_CODE = 2;
    private static final int INTERNET_CODE = 3;
    private static final int ACCESS_NETWORK_STATE_CODE = 4;

    private static final String REQUEST_WRITE_EXTERNAL_STORAGE= Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private static final String REQUEST_READ_EXTERNAL_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE;
    private static final String REQUEST_INTERNET = Manifest.permission.INTERNET;
    private static final String REQUEST_ACCESS_NETWORK_STATE = Manifest.permission.ACCESS_NETWORK_STATE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkWrite();
    }

    /**
     * Finished permissions, start the program
     */
    private void begin(){
        tm = new ThreadManager();
        wr = new WifiRunner(this);
        tm.runInBackground(wr, 1000);
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
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_CODE, this);
        }
        else checkRead();
    }

    /**
     * Check read permissions for external storage
     */
    private void checkRead(){
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_CODE, this);
        }
        else checkInternet();
    }

    /**
     * Check internet permissions
     */
    private void checkInternet(){
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_INTERNET) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_INTERNET, INTERNET_CODE, this);
        }
        else checkNetwork();
    }

    /**
     * Check network permissions
     */
    private void checkNetwork(){
        if(this.getApplicationContext().checkCallingOrSelfPermission(REQUEST_ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermission(REQUEST_ACCESS_NETWORK_STATE, ACCESS_NETWORK_STATE_CODE, this);
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
                        case REQUEST_WRITE_EXTERNAL_STORAGE:{
                            checkYourPriveledge();
                            break;
                        }
                        case REQUEST_READ_EXTERNAL_STORAGE:{
                            checkYourPriveledge();
                            break;
                        }
                        case REQUEST_INTERNET:{
                            checkYourPriveledge();
                            break;
                        }
                        case REQUEST_ACCESS_NETWORK_STATE:{
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


