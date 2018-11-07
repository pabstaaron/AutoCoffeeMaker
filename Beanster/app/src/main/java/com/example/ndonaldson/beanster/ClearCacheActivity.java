package com.example.ndonaldson.beanster;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.HashMap;

public class ClearCacheActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

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
    }

}
