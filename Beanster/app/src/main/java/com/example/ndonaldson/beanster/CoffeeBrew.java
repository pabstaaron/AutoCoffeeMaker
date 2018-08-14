package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.*;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.google.gson.Gson;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.IndicatorType;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;
import com.warkiz.widget.TickMarkType;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;

public class CoffeeBrew extends AppCompatActivity {

    private WifiRunner.ConnectStatus mConnectStatus;
    //Main Buttons
    private Button brewButton;
    private Button disconnectButton;
    private Button basicButton;
    private Button advancedButton;

    //BasicButtons
    private Button amountSmallButton;
    private Button amountMediumButton;
    private Button amountLargeButton;
    private Button strengthMildButton;
    private Button strengthRegularButton;
    private Button strengthStrongButton;
    private Button frothFrothyButton;
    private Button frothFrothierButton;
    private Button frothFrothiestButton;

    //AdvancedButtons
    private Button waterButton;
    private Button milkButton;
    private Button frothButton;
    private Button coffeeButton;
    private Button syrupButton;

    //Labels
    private EditText label1;
    private EditText label2;
    private EditText label3;

    //Seekbars
    private IndicatorSeekBar tempSeekbar;
    private IndicatorSeekBar pressSeekbar;
    private IndicatorSeekBar dispSeekbar;

    //GridLayouts
    private android.support.v7.widget.GridLayout basicGridLayout;

    //Spinner
    private Spinner mySpinner;
    private String[] syrups = {"Syrup1", "Syrup2"};

    //Data
    private String connectedIP;
    private String connectedSn;
    private AdvancedState advancedState;
    private BasicState basicState;
    private RequestData requestData;
    private ActiveState activeState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coffee_brew);

        /**
         * DATA SETUP
         */
        activeState = ActiveState.BASIC;
        advancedState = new AdvancedState();
        basicState = new BasicState();

        requestData = new RequestData();

        mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;
        if(!getIntent().hasExtra("address") && !getIntent().hasExtra("sN")){
            sendIntent("status");
            Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
            startActivity(deviceIntent);
        }
        else{
            connectedIP = getIntent().getStringExtra("address");
            connectedSn = getIntent().getStringExtra("sN");
        }

        LocalBroadcastManager.getInstance(this.getApplicationContext()).registerReceiver(wifiStatusReceiver,
                new IntentFilter("com.android.activity.WIFI_DATA_OUT"));

        /**
         * MAIN BUTTONS
         */
        brewButton = (Button) findViewById(R.id.brewButton);
        brewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestData data = new RequestData(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
                new SendPost().execute(data);
            }
        });

        disconnectButton = (Button) findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                startActivity(deviceIntent);
                finish();
            }
        });

        basicButton = (Button) findViewById(R.id.basicButton);
        basicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make basicButtons visible and active, hide sliders, advancedButtons, and spinner
            }
        });

        advancedButton = (Button) findViewById(R.id.advancedButton);
        advancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        /**
         * BASIC BUTTONS
         */
        amountSmallButton = (Button) findViewById(R.id.basicAmountButton);
        amountSmallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        amountMediumButton = (Button) findViewById(R.id.basicAmountButton);
        amountMediumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        amountLargeButton = (Button) findViewById(R.id.basicAmountButton);
        amountLargeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        strengthMildButton = (Button) findViewById(R.id.basicStrengthButton);
        strengthMildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        strengthRegularButton = (Button) findViewById(R.id.basicStrengthButton2);
        strengthRegularButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        strengthStrongButton = (Button) findViewById(R.id.basicStrengthButton3);
        strengthStrongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        frothFrothyButton = (Button) findViewById(R.id.basicFrothButton);
        frothFrothyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        frothFrothierButton = (Button) findViewById(R.id.basicFrothButton2);
        frothFrothierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        frothFrothiestButton = (Button) findViewById(R.id.basicFrothButton3);
        frothFrothiestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        /**
         * ADVANCED BUTTONS
         */
        waterButton = (Button) findViewById(R.id.waterButton);
        waterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        milkButton = (Button) findViewById(R.id.milkButton);
        milkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        frothButton = (Button) findViewById(R.id.frothButton);
        frothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        coffeeButton = (Button) findViewById(R.id.coffeeButton);
        coffeeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });

        syrupButton = (Button) findViewById(R.id.syrupButton);
        syrupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Make advancedButtons, sliders, or spinner visible, hide basicButtons and disable.
            }
        });


        /**
         * LABELS
         */
        label1 = (EditText) findViewById(R.id.label1);
        label2 = (EditText) findViewById(R.id.label2);
        label3 = (EditText) findViewById(R.id.label3);

        /**
         * SEEKBARS
         */
        tempSeekbar = (IndicatorSeekBar) this.findViewById(R.id.tempSlider);
        tempSeekbar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {

            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
            }
        });

        pressSeekbar = (IndicatorSeekBar) this.findViewById(R.id.pressSlider);
        pressSeekbar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
            }
        });

        dispSeekbar = (IndicatorSeekBar) this.findViewById(R.id.dispSlider);
        dispSeekbar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
            }
        });

        /**
         * SPINNER
         */
        mySpinner = (Spinner)findViewById(R.id.syrupSpinner);
        mySpinner.setAdapter(new MySpinnerAdapter(getApplicationContext(), R.layout.row, syrups));
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

        public RequestData(){
            this.waterTemp = 70;
            this.milkTemp = 70;
            this.waterPress = 70;
            this.frothPress = 70;
            this.waterDisp = 70;
            this.milkDisp = 70;
            this.frothDisp = 70;
            this.coffeeDisp = 70;
            this.syrupDisp = 70;
            this.syrup = 70;
        }

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

        public void setWithBasic(BasicState basicState){
            
        }

        public void setWithAdvance(AdvancedState advancedState){

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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiStatusReceiver);
    }


    /**
     * DATA STRUCTURES
     */
    private static class AdvancedState{

        public ActiveSection activeSection;
        public WaterState waterState;
        public MilkState milkState;
        public FrothState frothState;
        public CoffeeState coffeeState;
        public SyrupState syrupState;

        public AdvancedState(){
            activeSection = ActiveSection.WATER;
            waterState = new WaterState();
            milkState = new MilkState();
            frothState = new FrothState();
            coffeeState = new CoffeeState();
            syrupState = new SyrupState();
        }

        public enum ActiveSection{
            WATER,
            MILK,
            FROTH,
            COFFEE,
            SYRUP
        }
    }

    private static class BasicState{
        public int amount;
        public int strength;
        public int froth;

        public BasicState(){
            amount = 0;
            strength = 0;
            froth = 0;
        }
    }

    private static class WaterState{
        public int temp;
        public int disp;
        public int press;

        public WaterState(){
            temp = 70;
            disp = 70;
            press = 70;
        }
    }

    private static class MilkState{
        public int temp;
        public int disp;
        public int press;

        public MilkState(){
            temp = 70;
            disp = 70;
            press = 70;
        }
    }

    private static class FrothState{
        public int temp;
        public int disp;
        public int press;

        public FrothState(){
            temp = 70;
            disp = 70;
            press = 70;
        }
    }

    private static class CoffeeState{
        public int temp;
        public int disp;
        public int press;

        public CoffeeState(){
            temp = 70;
            disp = 70;
            press = 70;
        }
    }

    private static class SyrupState{
        public int type;
        public int disp;

        public SyrupState(){
            disp = 70;
            type = 0;
        }
    }

    public enum ActiveState{
        ADVANCED,
        BASIC
    }
}
