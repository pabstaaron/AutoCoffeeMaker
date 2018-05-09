package com.example.ndonaldson.beanster;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

/**
 *
 */
public class main extends AppCompatActivity {

    private static ThreadManager tm;
    private static WifiRunner wr;
    private static boolean permissionsComplete = false;
    private static boolean writeComplete = false;
    private static boolean readComplete = false;
    private static boolean internetComplete = false;
    private static boolean networkComplete = false;
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

        while(!permissionsComplete) {
            if (!checkPermissions()) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(false);
                alertBuilder.setTitle("Permissions Error");
                alertBuilder.setMessage("Need all permissions for app to continue");
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
            }
        }
        tm = new ThreadManager();
        wr = new WifiRunner(this.getApplicationContext());
        tm.runInBackground(wr);
    }

    /**
     *
     * @return
     */
    private boolean checkPermissions(){
        if(this.getApplicationContext().checkSelfPermission(REQUEST_WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermission(REQUEST_WRITE_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE_CODE);
        }
        if(this.getApplicationContext().checkSelfPermission(REQUEST_READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermission(REQUEST_READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_CODE);
        }
        if(this.getApplicationContext().checkSelfPermission(REQUEST_INTERNET) != PackageManager.PERMISSION_GRANTED){
            requestPermission(REQUEST_INTERNET, INTERNET_CODE);
        }
        if(this.getApplicationContext().checkSelfPermission(REQUEST_ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED){
            requestPermission(REQUEST_ACCESS_NETWORK_STATE, ACCESS_NETWORK_STATE_CODE);
        }
        return permissionsComplete;
    }

    /**
     *
     * @param permission
     * @param requestCode
     */
    private void requestPermission(final String permission, final int requestCode){
        if (this.shouldShowRequestPermissionRationale(permission)) {
            final Activity activity = this;
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
            alertBuilder.setCancelable(true);
            switch(permission){
                case REQUEST_WRITE_EXTERNAL_STORAGE:{
                    alertBuilder.setTitle("External Storage permission necessary");
                    alertBuilder.setMessage("Permission needed to saveimportant application data");
                    break;
                }
                case REQUEST_READ_EXTERNAL_STORAGE:{
                    alertBuilder.setTitle("External Storage permission necessary");
                    alertBuilder.setMessage("Permission needed to read important application data");
                    break;
                }
                case REQUEST_INTERNET:{
                    alertBuilder.setTitle("Internet permission necessary");
                    alertBuilder.setMessage("Permission needed to connect with device server");
                    break;
                }
                case REQUEST_ACCESS_NETWORK_STATE:{
                    alertBuilder.setTitle("External Storage permission necessary");
                    alertBuilder.setMessage("Permission needed to connect with device");
                    break;
                }
                default:{
                    Log.i("Main", "Unexpected Permission Request");
                    return;
                }
            }
            alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    activity.requestPermissions(new String[]{permission}, requestCode);
                }
            });
            alertBuilder.setNegativeButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (permission){
                        case REQUEST_WRITE_EXTERNAL_STORAGE:{
                            writeComplete = false;
                            permissionsComplete = false;
                        }
                        case REQUEST_READ_EXTERNAL_STORAGE:{
                            readComplete = false;
                            permissionsComplete = false;
                        }
                        case REQUEST_INTERNET:{
                            internetComplete = false;
                            permissionsComplete = false;
                        }
                        case REQUEST_ACCESS_NETWORK_STATE:{
                            networkComplete = false;
                        }
                    }
                }
            });

            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else {
            this.requestPermissions(new String[]{permission}, requestCode);
        }
    }

    /**
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    writeComplete = true;
                } else {
                    permissionsComplete = false;
                    writeComplete = false;
                }
                return;
            }
            case READ_EXTERNAL_STORAGE_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readComplete = true;
                } else {
                    permissionsComplete = false;
                    readComplete = false;
                }
                break;
            }
            case INTERNET_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    internetComplete = true;
                } else {
                    permissionsComplete = false;
                    internetComplete = false;
                }
                break;
            }
            case ACCESS_NETWORK_STATE_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    networkComplete = true;
                } else {
                     permissionsComplete = false;
                     networkComplete = false;
                }
                break;
            }
            default:{
                Log.i("Main", "Unexpected Permission Request");
                break;
            }
        }

        if(networkComplete && internetComplete && readComplete && writeComplete){
            permissionsComplete = true;
        }
    }
}


