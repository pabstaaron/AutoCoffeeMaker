package com.example.ndonaldson.beanster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.View;
import android.widget.*;

import com.google.gson.Gson;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.OnSeekChangeListener;
import com.warkiz.widget.SeekParams;

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
    private Button frothNoneButton;
    private Button frothFrothyButton;
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
    private int prevDisp = 70;
    private int prevPress = 70;
    private int prevTemp = 70;
    private float dispDiff = 70;
    private float pressDiff = 70;
    private float tempDiff = 70;

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
                if(activeState == ActiveState.ADVANCED) requestData.setWithAdvance(advancedState);
                else requestData.setWithBasic(basicState);
                new SendPost().execute(requestData);
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
                activeState = ActiveState.BASIC;
                hideAdvanced();
                showBasic();
            }
        });

        advancedButton = (Button) findViewById(R.id.advancedButton);
        advancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeState = ActiveState.ADVANCED;
                hideBasic();
                showAdvanced();
            }
        });

        /**
         * BASIC BUTTONS
         */
        amountSmallButton = (Button) findViewById(R.id.basicAmountButton);
        amountSmallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.amount = BasicState.State.FIRST;
                amountSmallButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                amountSmallButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(0, amountSmallButton);
            }
        });

        amountMediumButton = (Button) findViewById(R.id.basicAmountButton);
        amountMediumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.amount = BasicState.State.SECOND;
                amountMediumButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                amountMediumButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(0, amountMediumButton);
            }
        });

        amountLargeButton = (Button) findViewById(R.id.basicAmountButton);
        amountLargeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.amount = BasicState.State.THIRD;
                amountLargeButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                amountLargeButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(0, amountLargeButton);
            }
        });

        strengthMildButton = (Button) findViewById(R.id.basicStrengthButton);
        strengthMildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.strength = BasicState.State.FIRST;
                strengthMildButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                strengthMildButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(1, strengthMildButton);
            }
        });

        strengthRegularButton = (Button) findViewById(R.id.basicStrengthButton2);
        strengthRegularButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.strength = BasicState.State.SECOND;
                strengthRegularButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                strengthRegularButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(1, strengthRegularButton);
            }
        });

        strengthStrongButton = (Button) findViewById(R.id.basicStrengthButton3);
        strengthStrongButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.strength = BasicState.State.THIRD;
                strengthStrongButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                strengthStrongButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(1, strengthStrongButton);
            }
        });

        frothNoneButton = (Button) findViewById(R.id.basicFrothButton);
        frothNoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.froth = BasicState.State.FIRST;
                frothNoneButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                frothNoneButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(2, frothNoneButton);
            }
        });

        frothFrothyButton = (Button) findViewById(R.id.basicFrothButton2);
        frothFrothyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.froth = BasicState.State.SECOND;
                frothFrothyButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                frothFrothyButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(2, frothFrothyButton);
            }
        });

        frothFrothiestButton = (Button) findViewById(R.id.basicFrothButton3);
        frothFrothiestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                basicState.froth = BasicState.State.THIRD;
                frothFrothiestButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                frothFrothiestButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(2, frothFrothiestButton);
            }
        });

        /**
         * ADVANCED BUTTONS
         */
        waterButton = (Button) findViewById(R.id.waterButton);
        waterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSliders(advancedState.activeSection, AdvancedState.ActiveSection.WATER);
                advancedState.activeSection = AdvancedState.ActiveSection.WATER;
                waterButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                waterButton.setTextColor(Color.parseColor("#664400"));
                selectAdvancedButton(advancedState.activeSection);
            }
        });

        milkButton = (Button) findViewById(R.id.milkButton);
        milkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSliders(advancedState.activeSection, AdvancedState.ActiveSection.MILK);
                advancedState.activeSection = AdvancedState.ActiveSection.MILK;
                milkButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                milkButton.setTextColor(Color.parseColor("#664400"));
                selectAdvancedButton(advancedState.activeSection);
            }
        });

        frothButton = (Button) findViewById(R.id.frothButton);
        frothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSliders(advancedState.activeSection, AdvancedState.ActiveSection.FROTH);
                advancedState.activeSection = AdvancedState.ActiveSection.FROTH;
                frothButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                frothButton.setTextColor(Color.parseColor("#664400"));
                selectAdvancedButton(advancedState.activeSection);
            }
        });

        coffeeButton = (Button) findViewById(R.id.coffeeButton);
        coffeeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSliders(advancedState.activeSection, AdvancedState.ActiveSection.COFFEE);
                advancedState.activeSection = AdvancedState.ActiveSection.COFFEE;
                coffeeButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                coffeeButton.setTextColor(Color.parseColor("#664400"));
                selectAdvancedButton(advancedState.activeSection);
            }
        });

        syrupButton = (Button) findViewById(R.id.syrupButton);
        syrupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSliders(advancedState.activeSection, AdvancedState.ActiveSection.SYRUP);
                advancedState.activeSection = AdvancedState.ActiveSection.SYRUP;
                syrupButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                syrupButton.setTextColor(Color.parseColor("#664400"));
                selectAdvancedButton(advancedState.activeSection);
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
                setSeekBarValue(advancedState.activeSection, seekParams.progress, 0);
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
                setSeekBarValue(advancedState.activeSection, seekParams.progress, 1);
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
                setSeekBarValue(advancedState.activeSection, seekParams.progress, 2);
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
        mySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                advancedState.syrupState.type = position;
            }
        });

        /**
         * GRID LAYOUT
         */
        basicGridLayout = (GridLayout) findViewById(R.id.gridLayout2);
        basicGridLayout.setVisibility(View.INVISIBLE);

        hideAdvanced();
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


        //TODO: NEED DEFAULT VALUES
        public void setWithBasic(BasicState basicState){
            switch (basicState.amount){
                case FIRST:{

                }
                case SECOND:{

                }
                case THIRD:{

                }
            }

            switch (basicState.froth){
                case FIRST:{

                }
                case SECOND:{

                }
                case THIRD:{

                }
            }

            switch (basicState.strength){
                case FIRST:{

                }
                case SECOND:{

                }
                case THIRD:{

                }
            }
        }

        public void setWithAdvance(AdvancedState advancedState){
            this.waterTemp = advancedState.waterState.temp;
            this.waterPress = advancedState.waterState.press;
            this.waterDisp = advancedState.waterState.disp;
            this.milkTemp = advancedState.milkState.temp;
            this.milkDisp = advancedState.milkState.disp;
            this.frothPress = advancedState.frothState.press;
            this.frothDisp = advancedState.frothState.disp;
            this.coffeeDisp = advancedState.coffeeState.disp;
            this.syrupDisp = advancedState.syrupState.disp;
            this.syrup = advancedState.syrupState.type;
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
        public State amount;
        public State strength;
        public State froth;

        public enum State{
            FIRST,
            SECOND,
            THIRD
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

        public MilkState(){
            temp = 70;
            disp = 70;
        }
    }

    private static class FrothState{
        public int disp;
        public int press;

        public FrothState(){
            disp = 70;
            press = 70;
        }
    }

    private static class CoffeeState{
        public int disp;

        public CoffeeState(){
            disp = 70;
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

    public void hideAdvanced(){

        waterButton.setVisibility(View.INVISIBLE);
        waterButton.setEnabled(false);

        milkButton.setVisibility(View.INVISIBLE);
        milkButton.setEnabled(false);

        coffeeButton.setVisibility(View.INVISIBLE);
        coffeeButton.setEnabled(false);

        frothButton.setVisibility(View.INVISIBLE);
        frothButton.setEnabled(false);

        syrupButton.setVisibility(View.INVISIBLE);
        syrupButton.setEnabled(false);

        if(advancedState.activeSection == AdvancedState.ActiveSection.SYRUP) {
            mySpinner.setVisibility(View.INVISIBLE);
            mySpinner.setEnabled(false);

            dispSeekbar.setVisibility(View.INVISIBLE);
            dispSeekbar.setEnabled(false);
        }
        else {
            tempSeekbar.setVisibility(View.INVISIBLE);
            tempSeekbar.setEnabled(false);

            dispSeekbar.setVisibility(View.INVISIBLE);
            dispSeekbar.setEnabled(false);

            pressSeekbar.setVisibility(View.INVISIBLE);
            pressSeekbar.setEnabled(false);
        }
    }

    public void showAdvanced(){

        waterButton.setEnabled(true);
        waterButton.setVisibility(View.VISIBLE);

        milkButton.setEnabled(true);
        milkButton.setVisibility(View.VISIBLE);

        coffeeButton.setEnabled(true);
        coffeeButton.setVisibility(View.VISIBLE);

        frothButton.setEnabled(true);
        frothButton.setVisibility(View.VISIBLE);

        syrupButton.setEnabled(true);
        syrupButton.setVisibility(View.VISIBLE);


        switch(advancedState.activeSection){
            case WATER:{
                tempSeekbar.setEnabled(true);
                tempSeekbar.setVisibility(View.VISIBLE);

                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                pressSeekbar.setEnabled(true);
                pressSeekbar.setVisibility(View.VISIBLE);

                label1.setText("Temperature(F):");
                label2.setText("Pressure(PSI):");
                label3.setText("Dispense(Oz):");
            }
            case MILK:{
                tempSeekbar.setEnabled(true);
                tempSeekbar.setVisibility(View.VISIBLE);

                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                label1.setText("Temperature(F):");
                label2.setText("");
                label3.setText("Dispense(Oz):");
            }
            case FROTH:{
                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                pressSeekbar.setEnabled(true);
                pressSeekbar.setVisibility(View.VISIBLE);

                label1.setText("");
                label2.setText("Pressure(PSI):");
                label3.setText("Dispense(Oz):");
            }
            case SYRUP:{
                mySpinner.setEnabled(true);
                mySpinner.setVisibility(View.VISIBLE);

                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                label1.setText("Syrup:");
                label2.setText("");
                label3.setText("Dispense(Oz):");
            }
            case COFFEE:{
                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                label1.setText("");
                label2.setText("");
                label3.setText("Dispense(Oz):");
            }
        }
    }

    public void hideBasic(){
        amountSmallButton.setVisibility(View.GONE);
        amountSmallButton.setEnabled(false);

        amountMediumButton.setVisibility(View.GONE);
        amountMediumButton.setEnabled(false);

        amountLargeButton.setVisibility(View.GONE);
        amountLargeButton.setEnabled(false);

        strengthMildButton.setVisibility(View.GONE);
        strengthMildButton.setEnabled(false);

        strengthRegularButton.setVisibility(View.GONE);
        strengthRegularButton.setEnabled(false);

        strengthStrongButton.setVisibility(View.GONE);
        strengthStrongButton.setEnabled(false);

        frothNoneButton.setVisibility(View.GONE);
        frothNoneButton.setEnabled(false);

        frothFrothyButton.setVisibility(View.GONE);
        frothFrothyButton.setEnabled(false);

        frothFrothiestButton.setVisibility(View.GONE);
        frothFrothiestButton.setEnabled(false);
    }

    public void showBasic(){
        amountSmallButton.setEnabled(true);
        amountSmallButton.setVisibility(View.VISIBLE);

        amountMediumButton.setEnabled(true);
        amountMediumButton.setVisibility(View.VISIBLE);

        amountLargeButton.setEnabled(true);
        amountLargeButton.setVisibility(View.VISIBLE);

        strengthMildButton.setEnabled(true);
        strengthMildButton.setVisibility(View.VISIBLE);

        strengthRegularButton.setEnabled(true);
        strengthRegularButton.setVisibility(View.VISIBLE);

        strengthStrongButton.setEnabled(true);
        strengthStrongButton.setVisibility(View.VISIBLE);

        frothNoneButton.setEnabled(true);
        frothNoneButton.setVisibility(View.VISIBLE);

        frothFrothyButton.setEnabled(true);
        frothFrothyButton.setVisibility(View.VISIBLE);

        frothFrothiestButton.setEnabled(true);
        frothFrothiestButton.setVisibility(View.VISIBLE);

        label1.setText("Amount:");
        label2.setText("Strength:");
        label3.setText("Froth:");
    }

    public void playSliders(AdvancedState.ActiveSection prevState, AdvancedState.ActiveSection currState){

        switch(prevState){
            case WATER:{
                prevDisp = advancedState.waterState.disp;
                prevPress = advancedState.waterState.press;
                prevTemp = advancedState.waterState.temp;
            }
            case MILK:{
                prevDisp = advancedState.milkState.disp;
                prevTemp = advancedState.milkState.temp;
            }
            case COFFEE:{
                prevDisp = advancedState.coffeeState.disp;
            }
            case SYRUP:{
                prevDisp = advancedState.syrupState.disp;
                prevPress = 70;
                prevTemp = 70;
            }
            case FROTH:{
                prevDisp = advancedState.frothState.disp;
                prevPress = advancedState.frothState.press;
            }
        }

        switch(currState){
            case WATER:{
                dispDiff = prevDisp - advancedState.milkState.disp;
                tempDiff = prevTemp - advancedState.milkState.temp;
            }
            case MILK:{
                dispDiff = prevDisp - advancedState.milkState.disp;
                tempDiff = prevTemp - advancedState.milkState.temp;
            }
            case COFFEE:{
                dispDiff = prevDisp - advancedState.milkState.disp;
                tempDiff = prevTemp - advancedState.milkState.temp;
            }
            case SYRUP:{
                dispDiff = prevDisp - advancedState.milkState.disp;
            }
            case FROTH:{
                dispDiff = prevDisp - advancedState.milkState.disp;
                tempDiff = prevTemp - advancedState.milkState.temp;
            }
        }

        if(advancedState.activeSection == AdvancedState.ActiveSection.SYRUP){
            new Runnable() {
                float diffDispOverTime = Math.abs(dispDiff)/1000;
                @Override
                public void run() {
                    if(dispDiff < 0){
                        long currentTime = System.currentTimeMillis();
                        while(dispDiff < prevDisp){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                dispDiff = dispDiff + diffDispOverTime;
                                dispSeekbar.setProgress(dispDiff);
                            }
                        }
                    }
                    else{
                        long currentTime = System.currentTimeMillis();
                        while(dispDiff > prevDisp){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                dispDiff = dispDiff - diffDispOverTime;
                                dispSeekbar.setProgress(dispDiff);
                            }
                        }
                    }
                }
            }.run();
        }

        else {
            new Runnable() {
                float diffDispOverTime = Math.abs(dispDiff)/1000;
                @Override
                public void run() {
                    if(dispDiff < 0){
                        long currentTime = System.currentTimeMillis();
                        while(dispDiff < prevDisp){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                dispDiff = dispDiff + diffDispOverTime;
                                dispSeekbar.setProgress(dispDiff);
                            }
                        }
                    }
                    else{
                        long currentTime = System.currentTimeMillis();
                        while(dispDiff > prevDisp){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                dispDiff = dispDiff - diffDispOverTime;
                                dispSeekbar.setProgress(dispDiff);
                            }
                        }
                    }
                }
            }.run();
            new Runnable() {
                float diffTempOverTime = Math.abs(tempDiff)/1000;
                @Override
                public void run() {
                    if(tempDiff < 0){
                        long currentTime = System.currentTimeMillis();
                        while(tempDiff < prevTemp){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                tempDiff = tempDiff + diffTempOverTime;
                                tempSeekbar.setProgress(tempDiff);
                            }
                        }
                    }
                    else{
                        long currentTime = System.currentTimeMillis();
                        while(tempDiff > prevTemp){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                tempDiff = tempDiff - diffTempOverTime;
                                tempSeekbar.setProgress(tempDiff);
                            }
                        }
                    }
                }
            }.run();
            new Runnable() {
                float diffPressOverTime = Math.abs(pressDiff)/1000;
                @Override
                public void run() {
                    if(pressDiff < 0){
                        long currentTime = System.currentTimeMillis();
                        while(pressDiff < prevPress){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                pressDiff = pressDiff + diffPressOverTime;
                                pressSeekbar.setProgress(pressDiff);
                            }
                        }
                    }
                    else{
                        long currentTime = System.currentTimeMillis();
                        while(pressDiff > prevPress){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                pressDiff = pressDiff - diffPressOverTime;
                                pressSeekbar.setProgress(pressDiff);
                            }
                        }
                    }
                }
            }.run();
        }
    }

    private void selectAdvancedButton(AdvancedState.ActiveSection state){

        if(state == AdvancedState.ActiveSection.FROTH) {

            waterButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            label1.setText("");
            label2.setText("Pressure(PSI):");
            label3.setText("Dispense(Oz):");

            tempSeekbar.setEnabled(false);
            pressSeekbar.setEnabled(true);
            dispSeekbar.setEnabled(true);
        }

        if(state == AdvancedState.ActiveSection.WATER) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            tempSeekbar.setEnabled(true);
            dispSeekbar.setEnabled(true);
            pressSeekbar.setEnabled(true);

            label1.setText("Temperature(F):");
            label2.setText("Pressure(PSI):");
            label3.setText("Dispense(Oz):");
        }

        if(state == AdvancedState.ActiveSection.MILK) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            waterButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            tempSeekbar.setEnabled(true);
            dispSeekbar.setEnabled(true);
            pressSeekbar.setEnabled(false);

            label1.setText("Temperature(F):");
            label2.setText("");
            label3.setText("Dispense(Oz):");
        }

        if(state == AdvancedState.ActiveSection.COFFEE) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            waterButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            tempSeekbar.setEnabled(false);
            dispSeekbar.setEnabled(true);
            pressSeekbar.setEnabled(false);

            label1.setText("");
            label2.setText("");
            label3.setText("Dispense(Oz):");
        }

        if(state == AdvancedState.ActiveSection.SYRUP) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            waterButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(true);
            mySpinner.setVisibility(View.VISIBLE);

            tempSeekbar.setEnabled(false);
            dispSeekbar.setEnabled(true);
            pressSeekbar.setEnabled(false);

            label1.setText("Syrup:");
            label2.setText("");
            label3.setText("Dispense(Oz):");
        }
    }

    private void selectBasicButton(int row, Button button){
        switch(row){
            case 0:{
                if(button.getId() != R.id.basicAmountButton){
                    amountSmallButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    amountSmallButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                else if(button.getId() != R.id.basicAmountButton2){
                    amountMediumButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    amountMediumButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                else{
                    amountLargeButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    amountLargeButton.setTextColor(Color.parseColor("#ffefcc"));
                }
            }
            case 1:{
                if(button.getId() != R.id.basicStrengthButton){
                    strengthMildButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    strengthMildButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                else if(button.getId() != R.id.basicStrengthButton2){
                    strengthRegularButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    strengthRegularButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                else{
                    strengthStrongButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    strengthStrongButton.setTextColor(Color.parseColor("#ffefcc"));
                }
            }
            case 2:{
                if(button.getId() != R.id.basicFrothButton){
                    frothNoneButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    frothNoneButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                else if(button.getId() != R.id.basicFrothButton2){
                    frothFrothyButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    frothFrothyButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                else{
                    frothFrothiestButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    frothFrothiestButton.setTextColor(Color.parseColor("#ffefcc"));
                }
            }
        }
    }

    private void setSeekBarValue(AdvancedState.ActiveSection state, int val, int bar){
        switch(advancedState.activeSection){
            case WATER:{
                if(bar == 0){
                    advancedState.waterState.temp = val;
                }
                else if (bar == 1){
                    advancedState.waterState.press = val;
                }
                else{
                    advancedState.waterState.disp = val;
                }
            }
            case MILK:{
                if(bar == 0){
                    advancedState.milkState.temp = val;
                }
                else{
                    advancedState.milkState.disp = val;
                }
            }
            case COFFEE:{
                if(bar == 0){
                    advancedState.milkState.temp = val;
                }
                else{
                    advancedState.milkState.disp = val;
                }
            }
            case FROTH:{
                if (bar == 1){
                    advancedState.frothState.press = val;
                }
                else{
                    advancedState.frothState.disp = val;
                }
            }
            case SYRUP:{
                    advancedState.waterState.disp = val;
            }
        }
    }
}
