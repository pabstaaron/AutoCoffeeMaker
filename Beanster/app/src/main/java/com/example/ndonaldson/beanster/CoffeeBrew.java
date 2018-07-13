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
                RequestData data = new RequestData(textToSend.getText().toString(), sendBoolean.isChecked(), integerToSend.getProgress());
                new SendPost().execute(data);
        }
    });

    }

    /**
     * Return to device selection screen
     */
    @Override
    public void onBackPressed(){
        sendIntent("status");
        Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
        startActivity(deviceIntent);
        finish();
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

    private class RequestData{
        private String textToSend;
        private boolean sendBoolean;
        private int integerToSend;

        public RequestData(String textToSend, boolean sendBoolean, int integerToSend){
            this.textToSend = textToSend;
            this.sendBoolean = sendBoolean;
            this.integerToSend = integerToSend;
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
                    case WAITING_FOR_RESPONSE: {
                        sendIntent("status");
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
                        sendIntent("status");
                        Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                        startActivity(deviceIntent);
                        finish();
                        break;
                    }
                }
            }
        }
    };
}
