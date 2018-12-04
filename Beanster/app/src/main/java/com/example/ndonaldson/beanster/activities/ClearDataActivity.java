package com.example.ndonaldson.beanster.activities;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.example.ndonaldson.beanster.R;

import java.io.File;

/**
 * This class is used to overwrite the clear data option under application management.
 * It gives the user the option to keep user information.
 */

public class ClearDataActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private static final String  DEVICES_LOCATION = Environment.getExternalStorageDirectory().getAbsolutePath() + "/deviceIds.txt";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_cache);

        sharedPreferences = getSharedPreferences("beanster", MODE_PRIVATE);
        this.setFinishOnTouchOutside(false);

        AlertDialog.Builder alert = new AlertDialog.Builder(this.getApplicationContext());
        alert.setTitle("Would you like to clear user data? You will lose all of your saved information.");

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearData(true);
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearData(false);
            }
        });

    }

    /**
     * Clears data entirely or partially depending on the user's response to dialog.
     * @param clear
     */
    private void clearData(Boolean clear){
        if(clear){
            sharedPreferences.edit().clear().commit();
        } else{
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for(String s : sharedPreferences.getAll().keySet()){
                if(s.equals("currentUser") || s.equals("userData")) continue;
                else{
                    editor.remove(s).commit();
                }
            }
        }
        File file = new File(DEVICES_LOCATION);
        if(file.exists()){
            file.delete();
        }
    }
}
