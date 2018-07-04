package com.example.ndonaldson.beanster;

import android.content.Intent;
import android.os.Parcelable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.net.HttpURLConnection;

public class CoffeeBrew extends AppCompatActivity {

    private WifiRunner.ConnectStatus mConnectStatus;
    private EditText textToSend;
    private CheckBox sendBoolean;
    private SeekBar integerToSend;
    private Button sendButton;
    private String connectedIP;
    private String connectedMac;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coffee_brew);
        mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;

        textToSend = (EditText) findViewById(R.id.editTextTest);
        sendBoolean = (CheckBox) findViewById(R.id.checkBoxTest);
        integerToSend = (SeekBar) findViewById(R.id.seekBar2Test);
        sendButton = (Button) findViewById(R.id.buttonTest);

        if(!getIntent().hasExtra("address") && !getIntent().hasExtra("macAddress")){
            sendIntent("status");
            Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
            startActivity(deviceIntent);
        }
        else{
            connectedIP = getIntent().getStringExtra("address");
            connectedMac = connectedIP = getIntent().getStringExtra("macAddress");
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HttpClient httpClient = new DefaultHttpClient();

                try {
                    RequestData data = new RequestData(textToSend.getText().toString(), sendBoolean.isChecked(), integerToSend.getProgress());
                    HttpPost request = new HttpPost("http://" + connectedIP + ":5000/coffee/" + connectedMac);
                    Gson gson = new Gson();
                    String json = gson.toJson(data);
                    StringEntity params =new StringEntity(json);
                    request.setEntity(params);
                    request.addHeader("Accept","application/json");
                    request.addHeader("content-type", "application/json");
                    HttpResponse response = httpClient.execute(request);
                    String responseJSON = EntityUtils.toString(response.getEntity(), "UTF-8");
                    Log.i("Brew", "Received " + response.toString());
                }catch (Exception e) {
                    e.printStackTrace();
                    Log.i("Brew", e.getLocalizedMessage());
                } finally {
                    httpClient.getConnectionManager().shutdown();
                }

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
}
