package com.example.ndonaldson.beanster;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.victor.loading.newton.NewtonCradleLoading;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class CoffeeBrew extends AppCompatActivity {

    private WifiRunner.ConnectStatus mConnectStatus;
    private EditText textToSend;
    private CheckBox sendBoolean;
    private SeekBar integerToSend;
    private Button sendButton;
    private String connectedIP;
    private String connectedSn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coffee_brew);
        mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;

        textToSend = (EditText) findViewById(R.id.editTextTest);
        sendBoolean = (CheckBox) findViewById(R.id.checkBoxTest);
        integerToSend = (SeekBar) findViewById(R.id.seekBar2Test);
        sendButton = (Button) findViewById(R.id.buttonTest);
        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_DATA_OUT"));

        if(!getIntent().hasExtra("address") && !getIntent().hasExtra("sN")){
            sendIntent("status");
            Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
            startActivity(deviceIntent);
        }
        else{
            connectedIP = getIntent().getStringExtra("address");
            connectedSn = getIntent().getStringExtra("sN");
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestData data = new RequestData(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
                new SendPost().execute(data);
        }
    });

    }

    /**
     * Return to device selection screen
     */
    @Override
    public void onBackPressed(){
        Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
        startActivity(deviceIntent);
        finish();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiStatusReceiver);
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
        if(!intent.getExtras().isEmpty()) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }
    }


    /**
     • Brew water temperature; as a value in degress Farenheit.
     • Frothing pressure; as a value in PSI.
     • Brew water pressure, as a value in PSI.
     • Amount of water to dispense through the brewer, as a value in ounces.
     • Amount of milk dispensed through the frother, in ounces.
     • Temprature for milk to reach in frothing cycle, as a value in degrees Fahrenheit.
     • Amount of froth to produce; as a value from zero to 100.
       A value of zero will cause the milk to simply be warmed up (steamed), while a higher value will produce more foam.
     • What kind of syrup, along with how much; as two integer values. The first number will be a value between zero and four.
       The second number will be the amount of syrup to dispense; in ounces.
     • The amount of coffee to dispense; in kilograms.
     */
    private class RequestData{
        private int waterTemp;
        private int milkTemp;
        private int waterPress;
        private int frothPress;
        private int waterDisp;
        private int milkDisp;
        private int frothDisp;
        private int coffeeDisp;
        private int syrupDisp;
        private int syrup;


        public RequestData(int waterTemp, int milkTemp, int waterPress, int frothPress, int waterDisp,
                           int milkDisp, int frothDisp, int coffeeDisp, int syrupDisp, int syrup){
            this.waterTemp = waterTemp;
            this.milkTemp = milkTemp;
            this.waterPress = waterPress;
            this.frothPress = frothPress;
            this.waterDisp = waterDisp;
            this.milkDisp = milkDisp;
            this.frothDisp = frothDisp;
            this.coffeeDisp = coffeeDisp;
            this.syrupDisp = syrupDisp;
            this.syrup = syrup;
        }
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    class SendPost extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            HttpClient httpClient = new DefaultHttpClient();
            InputStream inputStream = null;
            RequestData data = (RequestData) params[0];
            String result = "";
            try {
                HttpPost request = new HttpPost(new URI("http://" + connectedIP + ":5000/coffee/" + connectedSn));
                Log.i("Brew", "URI: " + request.getURI());
                String json = new Gson().toJson(data);
                Log.i("Brew", "JSON to send: " + json);
                StringEntity stuff =new StringEntity(json);
                request.setEntity(stuff);
                request.addHeader("Accept","application/json");
                request.addHeader("content-type", "application/json");
                HttpResponse response = httpClient.execute(request);
                inputStream = response.getEntity().getContent();
                if(inputStream != null)
                    result = convertInputStreamToString(inputStream);
                else
                    result = "You Lose";
                Log.i("Brew", "Received " + result);
            }catch (Exception e) {
                e.printStackTrace();
                Log.i("Brew", e.getLocalizedMessage());
            } finally {
                httpClient.getConnectionManager().shutdown();
                return result;
            }
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
            if (intent.hasExtra("status")) {
                String status = intent.getStringExtra("status");
                mConnectStatus = WifiRunner.ConnectStatus.valueOf(status);
                switch (mConnectStatus) {
                    case WAITING_FOR_USER: {
                        Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                        startActivity(deviceIntent);
                        finish();
                        break;
                    }
                    case UNKNOWN: {
                        //Default state....don't know what to do with it.
                        break;
                    }
                    case NO_WIFI: {
                        Intent deviceIntent = new Intent(getApplicationContext(), MainMenu.class);
                        intent.putExtra("noWifi", true);
                        startActivity(deviceIntent);
                        finish();
                        break;
                    }
                }
            }
        }
    };

    @Override
    public void onDestroy(){
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiStatusReceiver);    }
}
