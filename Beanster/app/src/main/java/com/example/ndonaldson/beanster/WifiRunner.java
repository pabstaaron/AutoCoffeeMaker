package com.example.ndonaldson.beanster;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.util.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WifiRunner implements Runnable {

    private static final long EXECUTION_TIMER = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);
    private static List<Pair<Float,Short>> networks;
    private WifiManager wifiManager;
    private boolean connected;
    private static long timeSinceRun;


    public WifiRunner(Context context){
        wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        timeSinceRun = 0L;
        connected = false;
    }

    /**
     * Check wifi connection every second to make sure that we are still connected.
     * If no connection kick out to main screen.
     * Keep checking response from raspberry PI.
     */
    @Override
    public void run() {
        if(timeSinceRun == 0L)
            timeSinceRun = System.currentTimeMillis();
        if (System.currentTimeMillis() -  timeSinceRun <= EXECUTION_TIMER)
            return;
        else{
            timeSinceRun = System.currentTimeMillis();
        }
    }


}
