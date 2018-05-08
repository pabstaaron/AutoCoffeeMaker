package com.example.ndonaldson.beanster;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class main extends AppCompatActivity {

    private static ThreadManager tm;
    private static WifiRunner wr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tm = new ThreadManager();
        wr = new WifiRunner(this.getApplicationContext());
        tm.runInBackground(wr);
    }
}


