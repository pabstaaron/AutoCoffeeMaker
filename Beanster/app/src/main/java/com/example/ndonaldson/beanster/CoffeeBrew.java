package com.example.ndonaldson.beanster;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.warkiz.widget.IndicatorSeekBar;
import com.warkiz.widget.IndicatorStayLayout;
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;

public class CoffeeBrew extends AppCompatActivity implements LoginFragment.OnFragmentInteractionListener, FavoritesFragment.OnFragmentInteractionListener {

    private WifiRunner.ConnectStatus mConnectStatus;

    //Main Buttons
    private Button brewButton;
    private Button backButton;
    private Button basicButton;
    private Button advancedButton;
    private Button loginButton;
    private Button favoritesButton;

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
    private android.support.v7.widget.GridLayout basicGridLayout2;

    //IndicatorLayouts
    private IndicatorStayLayout indicatorLayout1;
    private IndicatorStayLayout indicatorLayout2;
    private IndicatorStayLayout indicatorLayout3;


    //Spinner
    private Spinner mySpinner;
    private String[] syrups = {"Disabled", "Enabled"};

    //Data
    private String connectedPassword;
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
    private Context mContext;
    private String userName;
    private FrameLayout fragmentContainer;
    private boolean favoritedBrew = false;
    private UserData currentUserData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coffee_brew);

        Thread.currentThread().setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
                Log.i("CoffeeBrew", ex.getLocalizedMessage());
                Intent mStartActivity = new Intent(getApplicationContext(), main.class);
                mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, mPendingIntent);
                System.exit(0);
            }
        });

        /**
         * DATA SETUP
         */
        mContext = this;
        activeState = ActiveState.BASIC;
        advancedState = new AdvancedState();
        basicState = new BasicState();
        fragmentContainer = (FrameLayout) findViewById(R.id.fragmentContainer3);
        requestData = new RequestData();

        mConnectStatus = WifiRunner.ConnectStatus.WAITING_FOR_USER;
        if(!getIntent().hasExtra("passWord")){
            sendIntent("status");
            Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
            startActivity(deviceIntent);
            finish();
        }
        else{
            connectedPassword = getIntent().getStringExtra("passWord");
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
                if (activeState == ActiveState.ADVANCED)
                    requestData.setWithAdvance(advancedState);
                else
                    requestData.setWithBasic(basicState);
                new SendPost().execute(requestData);
            }
        });

        backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                deviceIntent.putExtra("connected", true);
                startActivity(deviceIntent);
                finish();
            }
        });

        basicButton = (Button) findViewById(R.id.basicButton);
        basicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeState = ActiveState.BASIC;
                basicButton.setBackground(getDrawable(R.drawable.leftroundedselected));
                basicButton.setTextColor(Color.parseColor("#664400"));
                advancedButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                advancedButton.setTextColor(Color.parseColor("#ffefcc"));
                hideAdvanced();
                showBasic();
            }
        });

        advancedButton = (Button) findViewById(R.id.advancedButton);
        advancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activeState = ActiveState.ADVANCED;
                advancedButton.setBackground(getDrawable(R.drawable.rightroundedselected));
                advancedButton.setTextColor(Color.parseColor("#664400"));
                basicButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                basicButton.setTextColor(Color.parseColor("#ffefcc"));
                hideBasic();
                showAdvanced();
            }
        });

        loginButton = (Button) findViewById(R.id.loginButton3);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!loginButton.getText().toString().equals("Login")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setCancelable(false);
                    builder.setMessage("Would you like to logout?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            loginButton.setText("Login");
                            favoritesButton.setVisibility(View.INVISIBLE);
                            favoritesButton.setEnabled(false);
                            SharedPreferences sharedPreferences = getSharedPreferences("beanster", MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("currentUser", "").apply();
                            loginButton.setText("Login");

                            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                            builder.setMessage("Would you like to login with a new user?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    openLoginFragment();
                                }
                            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();

                                }
                            }).show();
                        }
                    }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
                }
                else{
                    openLoginFragment();
                }
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("beanster", MODE_PRIVATE);
        if(sharedPreferences.contains("currentUser")){
            if(!sharedPreferences.getString("currentUser", "").isEmpty()){
                loginButton.setText(sharedPreferences.getString("currentUser", ""));
            }
        }

        favoritesButton = (Button) findViewById(R.id.favoritesButton);
        if (activeState == ActiveState.ADVANCED && !loginButton.getText().toString().equals("Login")){
            favoritesButton.setVisibility(View.VISIBLE);
            favoritesButton.setEnabled(true);
        }
        else{
            favoritesButton.setVisibility(View.INVISIBLE);
            favoritesButton.setEnabled(false);
        }
        favoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFavoritesFragment();
            }
        });

        /**
         * BASIC BUTTONS
         */
        amountSmallButton = (Button) findViewById(R.id.basicAmountButton);
        amountSmallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritedBrew = false;
                basicState.amount = BasicState.State.FIRST;
                amountSmallButton.setBackground(getDrawable(R.drawable.leftroundedselected));
                amountSmallButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(0, amountSmallButton);
            }
        });

        amountMediumButton = (Button) findViewById(R.id.basicAmountButton2);
        amountMediumButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritedBrew = false;
                basicState.amount = BasicState.State.SECOND;
                amountMediumButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                amountMediumButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(0, amountMediumButton);
            }
        });

        amountLargeButton = (Button) findViewById(R.id.basicAmountButton3);
        amountLargeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritedBrew = false;
                basicState.amount = BasicState.State.THIRD;
                amountLargeButton.setBackground(getDrawable(R.drawable.rightroundedselected));
                amountLargeButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(0, amountLargeButton);
            }
        });

        strengthMildButton = (Button) findViewById(R.id.basicStrengthButton);
        strengthMildButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritedBrew = false;
                basicState.strength = BasicState.State.FIRST;
                strengthMildButton.setBackground(getDrawable(R.drawable.leftroundedselected));
                strengthMildButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(1, strengthMildButton);
            }
        });

        strengthRegularButton = (Button) findViewById(R.id.basicStrengthButton2);
        strengthRegularButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritedBrew = false;
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
                favoritedBrew = false;
                basicState.strength = BasicState.State.THIRD;
                strengthStrongButton.setBackground(getDrawable(R.drawable.rightroundedselected));
                strengthStrongButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(1, strengthStrongButton);
            }
        });

        frothNoneButton = (Button) findViewById(R.id.basicFrothButton);
        frothNoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritedBrew = false;
                basicState.froth = BasicState.State.FIRST;
                frothNoneButton.setBackground(getDrawable(R.drawable.leftroundedselected));
                frothNoneButton.setTextColor(Color.parseColor("#664400"));
                selectBasicButton(2, frothNoneButton);
            }
        });

        frothFrothyButton = (Button) findViewById(R.id.basicFrothButton2);
        frothFrothyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoritedBrew = false;
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
                favoritedBrew = false;
                basicState.froth = BasicState.State.THIRD;
                frothFrothiestButton.setBackground(getDrawable(R.drawable.rightroundedselected));
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
                waterButton.setBackground(getDrawable(R.drawable.leftroundedselected));
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
                milkButton.setBackground(getDrawable(R.drawable.leftroundedselected));
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
                coffeeButton.setBackground(getDrawable(R.drawable.rightroundedselected));
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
                syrupButton.setBackground(getDrawable(R.drawable.rightroundedselected));
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
        tempSeekbar.setIndicatorTextFormat("${PROGRESS} " + (char) 0x00B0 + "F");
        tempSeekbar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                if(seekParams.progress % 1 == 0) {
                    tempSeekbar.setIndicatorTextFormat("${PROGRESS} " + (char) 0x00B0 + "F");
                }
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
                favoritedBrew = false;
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                setSeekBarValue(advancedState.activeSection, seekBar.getProgress(), 0);
            }
        });

        pressSeekbar = (IndicatorSeekBar) this.findViewById(R.id.pressSlider);
        pressSeekbar.setIndicatorTextFormat("${PROGRESS} psi");
        pressSeekbar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                if(seekParams.progress % 1 == 0) {
                    pressSeekbar.setIndicatorTextFormat("${PROGRESS} psi");
                }
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
                favoritedBrew = false;
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                setSeekBarValue(advancedState.activeSection, seekBar.getProgress(), 1);
            }
        });

        dispSeekbar = (IndicatorSeekBar) this.findViewById(R.id.dispSlider);
        dispSeekbar.setIndicatorTextFormat("${PROGRESS} oz");
        dispSeekbar.setOnSeekChangeListener(new OnSeekChangeListener() {
            @Override
            public void onSeeking(SeekParams seekParams) {
                if(seekParams.progress % 1 == 0) {
                    dispSeekbar.setIndicatorTextFormat("${PROGRESS} oz");
                }
            }

            @Override
            public void onStartTrackingTouch(IndicatorSeekBar seekBar) {
                favoritedBrew = false;
            }

            @Override
            public void onStopTrackingTouch(IndicatorSeekBar seekBar) {
                setSeekBarValue(advancedState.activeSection, seekBar.getProgress(), 2);
            }
        });

        /**
         * SPINNER
         */
        mySpinner = (Spinner)findViewById(R.id.syrupSpinner);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, R.layout.spinner_item, syrups);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_item);
        mySpinner.setAdapter(spinnerAdapter);
        mySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> arg0, View view, int position, long id) {
                advancedState.syrupState.type = position;

            }
            public void onNothingSelected(AdapterView<?> arg0) { }
        });

        /**
         * GRID LAYOUT
         */
        basicGridLayout = (android.support.v7.widget.GridLayout) findViewById(R.id.gridLayout);
        basicGridLayout2 = (android.support.v7.widget.GridLayout) findViewById(R.id.gridLayout2);
        basicGridLayout2.setVisibility(View.INVISIBLE);

        /**
         * INDICATOR LAYOUTS
         */
        indicatorLayout1 = (IndicatorStayLayout) findViewById(R.id.indicatorLayout1);
        indicatorLayout2 = (IndicatorStayLayout) findViewById(R.id.indicatorLayout2);
        indicatorLayout3 = (IndicatorStayLayout) findViewById(R.id.indicatorLayout3);

        hideAdvanced();
    }

    /**
     * Return to device selection screen
     */
    @Override
    public void onBackPressed(){
        Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
        deviceIntent.putExtra("connected", true);
        startActivity(deviceIntent);
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
                HttpPost request = new HttpPost(new URI("http://192.168.5.1:5000/coffee/" + connectedPassword));
                Log.i("CoffeeBrew", "URI: " + request.getURI());
                String json = new Gson().toJson(data);
                Log.i("CoffeeBrew", "JSON to send: " + json);
                StringEntity stuff =new StringEntity(json);
                request.setEntity(stuff);
                request.addHeader("Accept","application/json");
                request.addHeader("content-type", "application/json");
                HttpResponse response = httpClient.execute(request);
                int responseCode = response.getStatusLine().getStatusCode();

                Log.i("CoffeeBrew", "responsCode: " + responseCode);
                if(responseCode == HttpURLConnection.HTTP_CREATED){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast.makeText(mContext, "Your drink is being brewed...", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            onResponse();
                        }
                    });
                } else if(responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast.makeText(mContext, "There was a problem brewing your drink...", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                }
                else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast.makeText(mContext, "Data sent was corrupted...", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                }
                else if(responseCode == HttpURLConnection.HTTP_OK){
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast.makeText(mContext, "A drink is currently being brewed...", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    });
                }
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
                    case WAITING_FOR_USER:
                    case UNKNOWN: {
                        //Default state....don't know what to do with it.
                        break;
                    }
                    case NO_WIFI: {
                        Toast toast = Toast.makeText(context, "Lost connection to device.....", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        Intent deviceIntent = new Intent(getApplicationContext(), DeviceSelection.class);
                        intent.putExtra("connected", false);
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
    public static class AdvancedState{

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

    public static class BasicState{
        public State amount;
        public State strength;
        public State froth;

        public BasicState(){
            amount = State.FIRST;
            strength = State.FIRST;
            froth = State.FIRST;
        }

        public enum State{
            FIRST,
            SECOND,
            THIRD
        }
    }

    public static class WaterState{
        public int temp;
        public int disp;
       // public int press;

        public WaterState(){
            temp = 70;
            disp = 70;
           // press = 70;
        }
    }

    public static class MilkState{
        public int temp;
        public int disp;

        public MilkState(){
            temp = 70;
            disp = 70;
        }
    }

    public static class FrothState{
        public int disp;
        public int press;

        public FrothState(){
            disp = 70;
            press = 70;
        }
    }

    public static class CoffeeState{
        public int disp;

        public CoffeeState(){
            disp = 70;
        }
    }

    public static class SyrupState{
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

        mySpinner.setVisibility(View.GONE);
        mySpinner.setEnabled(false);

        dispSeekbar.setVisibility(View.INVISIBLE);
        dispSeekbar.setEnabled(false);

        tempSeekbar.setVisibility(View.INVISIBLE);
        tempSeekbar.setEnabled(false);

        dispSeekbar.setVisibility(View.INVISIBLE);
        dispSeekbar.setEnabled(false);

        pressSeekbar.setVisibility(View.INVISIBLE);
        pressSeekbar.setEnabled(false);

        indicatorLayout1.setVisibility(View.INVISIBLE);

        indicatorLayout2.setVisibility(View.INVISIBLE);

        indicatorLayout3.setVisibility(View.INVISIBLE);

        favoritesButton.setVisibility(View.INVISIBLE);
        favoritesButton.setEnabled(false);
    }

    public void showAdvanced(){

        basicGridLayout2.setEnabled(true);
        basicGridLayout2.setVisibility(View.VISIBLE);

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

        indicatorLayout1.setVisibility(View.VISIBLE);
        indicatorLayout2.setVisibility(View.VISIBLE);
        indicatorLayout3.setVisibility(View.VISIBLE);


        switch(advancedState.activeSection){
            case WATER:{
                waterButton.setBackground(getDrawable(R.drawable.leftroundedselected));
                waterButton.setTextColor(Color.parseColor("#664400"));

                milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                milkButton.setTextColor(Color.parseColor("#ffefcc"));

                coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

                frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                frothButton.setTextColor(Color.parseColor("#ffefcc"));

                syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                syrupButton.setTextColor(Color.parseColor("#ffefcc"));

                indicatorLayout1.setVisibility(View.VISIBLE);
                indicatorLayout1.setEnabled(true);
                tempSeekbar.setEnabled(true);
                tempSeekbar.setVisibility(View.VISIBLE);

                indicatorLayout3.setEnabled(true);
                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                indicatorLayout2.setEnabled(false);
                pressSeekbar.setVisibility(View.VISIBLE);
                pressSeekbar.setEnabled(false);

                label1.setText("Temperature(" + (char) 0x00B0 + "F):");
                label2.setText("");
                label3.setText("Dispense(oz):");
                break;
            }
            case MILK:{
                milkButton.setBackground(getDrawable(R.drawable.leftroundedselected));
                milkButton.setTextColor(Color.parseColor("#664400"));

                waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                waterButton.setTextColor(Color.parseColor("#ffefcc"));

                coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

                frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                frothButton.setTextColor(Color.parseColor("#ffefcc"));

                syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                syrupButton.setTextColor(Color.parseColor("#ffefcc"));

                indicatorLayout1.setVisibility(View.VISIBLE);
                indicatorLayout1.setEnabled(true);
                tempSeekbar.setEnabled(true);
                tempSeekbar.setVisibility(View.VISIBLE);

                indicatorLayout2.setEnabled(false);
                pressSeekbar.setVisibility(View.VISIBLE);
                pressSeekbar.setEnabled(false);

                indicatorLayout3.setEnabled(true);
                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                label1.setText("Temperature(" + (char) 0x00B0 + "F):");
                label2.setText("");
                label3.setText("Dispense(oz):");
                break;
            }
            case FROTH:{
                frothButton.setBackground(getDrawable(R.drawable.gridbuttonselected));
                frothButton.setTextColor(Color.parseColor("#664400"));

                milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                milkButton.setTextColor(Color.parseColor("#ffefcc"));

                coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

                waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                waterButton.setTextColor(Color.parseColor("#ffefcc"));

                syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                syrupButton.setTextColor(Color.parseColor("#ffefcc"));

                indicatorLayout3.setEnabled(true);
                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                indicatorLayout1.setVisibility(View.VISIBLE);
                indicatorLayout1.setEnabled(false);
                tempSeekbar.setVisibility(View.VISIBLE);
                tempSeekbar.setEnabled(false);

                indicatorLayout2.setEnabled(true);
                pressSeekbar.setEnabled(true);
                pressSeekbar.setVisibility(View.VISIBLE);

                label1.setText("");
                label2.setText("Pressure(psi):");
                label3.setText("Dispense(oz):");
                break;
            }
            case SYRUP:{
                syrupButton.setBackground(getDrawable(R.drawable.rightroundedselected));
                syrupButton.setTextColor(Color.parseColor("#664400"));

                milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                milkButton.setTextColor(Color.parseColor("#ffefcc"));

                coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

                frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                frothButton.setTextColor(Color.parseColor("#ffefcc"));

                waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                waterButton.setTextColor(Color.parseColor("#ffefcc"));

                mySpinner.setEnabled(true);
                mySpinner.setVisibility(View.VISIBLE);

                indicatorLayout1.setVisibility(View.INVISIBLE);
                indicatorLayout1.setEnabled(false);
                tempSeekbar.setVisibility(View.INVISIBLE);
                tempSeekbar.setEnabled(false);

                indicatorLayout2.setEnabled(false);
                pressSeekbar.setVisibility(View.VISIBLE);
                pressSeekbar.setEnabled(false);

                indicatorLayout3.setEnabled(true);
                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                label1.setText("Syrup:");
                label2.setText("");
                label3.setText("Dispense(oz):");
                break;
            }
            case COFFEE:{
                coffeeButton.setBackground(getDrawable(R.drawable.rightroundedselected));
                coffeeButton.setTextColor(Color.parseColor("#664400"));

                milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                milkButton.setTextColor(Color.parseColor("#ffefcc"));

                waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                waterButton.setTextColor(Color.parseColor("#ffefcc"));

                frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                frothButton.setTextColor(Color.parseColor("#ffefcc"));

                syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                syrupButton.setTextColor(Color.parseColor("#ffefcc"));


                indicatorLayout1.setVisibility(View.VISIBLE);
                indicatorLayout1.setEnabled(false);
                tempSeekbar.setVisibility(View.VISIBLE);
                tempSeekbar.setEnabled(false);

                indicatorLayout2.setEnabled(false);
                pressSeekbar.setVisibility(View.VISIBLE);
                pressSeekbar.setEnabled(false);

                indicatorLayout3.setEnabled(true);
                dispSeekbar.setEnabled(true);
                dispSeekbar.setVisibility(View.VISIBLE);

                indicatorLayout1.setEnabled(false);

                label1.setText("");
                label2.setText("");
                label3.setText("Dispense(oz):");
                break;
            }
        }

        if(!loginButton.getText().toString().equals("Login")) {
            favoritesButton.setEnabled(true);
            favoritesButton.setVisibility(View.VISIBLE);
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

        favoritesButton.setVisibility(View.INVISIBLE);
        favoritesButton.setEnabled(false);
    }

    public void playSliders(final AdvancedState.ActiveSection prevState, AdvancedState.ActiveSection currState){

        Log.i("CoffeeBrew", "prevState: " + prevState.name());
        Log.i("CoffeeBrew", "currentState: " + currState.name());

        switch(prevState){
            case WATER:{
                prevDisp = advancedState.waterState.disp;
                //prevPress = advancedState.waterState.press;
                prevTemp = advancedState.waterState.temp;
                break;
            }
            case MILK:{
                prevDisp = advancedState.milkState.disp;
                prevTemp = advancedState.milkState.temp;
                prevPress = 70;
                break;
            }
            case COFFEE:{
                prevDisp = advancedState.coffeeState.disp;
                prevPress = 70;
                prevTemp = 70;
                break;
            }
            case SYRUP:{
                prevDisp = advancedState.syrupState.disp;
                prevPress = 70;
                prevTemp = 70;
                break;
            }
            case FROTH:{
                prevDisp = advancedState.frothState.disp;
                prevPress = advancedState.frothState.press;
                prevTemp = 70;
                break;
            }
        }

        if(currState == prevState){
            advancedState.coffeeState.disp = requestData.coffeeDisp;
            advancedState.frothState.disp = requestData.frothDisp;
            advancedState.frothState.press = requestData.frothPress;
            advancedState.milkState.disp = requestData.milkDisp;
            advancedState.milkState.temp = requestData.milkTemp;
            advancedState.waterState.disp = requestData.waterDisp;
            advancedState.waterState.temp = requestData.waterTemp;
            advancedState.syrupState.disp = requestData.syrupDisp;
            advancedState.syrupState.disp = requestData.syrup;
        }

        switch(currState){
            case WATER:{
                dispDiff = prevDisp - advancedState.waterState.disp;
                tempDiff = prevTemp - advancedState.waterState.temp;
                //pressDiff = prevPress - advancedState.waterState.press;
                break;
            }
            case MILK:{
                dispDiff = prevDisp - advancedState.milkState.disp;
                tempDiff = prevTemp - advancedState.milkState.temp;
                pressDiff = prevTemp;
                break;
            }
            case COFFEE:{
                dispDiff = prevDisp - advancedState.coffeeState.disp;
                tempDiff = prevTemp;
                pressDiff = prevPress;
                break;
            }
            case SYRUP:{
                dispDiff = prevDisp - advancedState.syrupState.disp;
                tempDiff = prevTemp;
                pressDiff = prevPress;
                break;
            }
            case FROTH:{
                dispDiff = prevDisp - advancedState.frothState.disp;
                pressDiff = prevPress - advancedState.frothState.press;
                tempDiff = prevTemp;
                break;
            }
        }

        if(currState != AdvancedState.ActiveSection.SYRUP) {
            new Thread(new Runnable() {
                float diffDispOverTime = Math.abs(dispDiff)/1000;
                @Override
                public void run() {
                    if(dispDiff < 0){
                        float desiredValue = prevDisp + Math.abs(dispDiff);
                        float actualValue = prevDisp;
                        long currentTime = System.currentTimeMillis();
                        while(actualValue < desiredValue){
                            if(System.currentTimeMillis() > currentTime){
                            currentTime = System.currentTimeMillis();
                            actualValue = actualValue + diffDispOverTime;
                                final float finalActualValue = actualValue;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dispSeekbar.setProgress(finalActualValue);
                                    }
                                });
                            }
                        }
                    }
                    else{
                        float desiredValue = prevDisp - dispDiff;
                        float actualValue = prevDisp;
                        long currentTime = System.currentTimeMillis();
                        while(actualValue > desiredValue){
                            if(System.currentTimeMillis() > currentTime){
                                currentTime = System.currentTimeMillis();
                                actualValue = actualValue - diffDispOverTime;
                                final float finalActualValue = actualValue;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dispSeekbar.setProgress(finalActualValue);
                                    }
                                });
                            }
                        }
                    }
                }
            }).start();
        }

        new Thread(new Runnable() {
            float diffTempOverTime = Math.abs(tempDiff)/1000;
            @Override
            public void run() {
                if(tempDiff < 0){
                    float desiredValue = prevTemp + Math.abs(tempDiff);
                    float actualValue = prevTemp;
                    long currentTime = System.currentTimeMillis();
                    while(actualValue < desiredValue){
                        if(System.currentTimeMillis() > currentTime){
                            currentTime = System.currentTimeMillis();
                            actualValue = actualValue + diffTempOverTime;
                            final float finalActualValue = actualValue;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tempSeekbar.setProgress(finalActualValue);
                                }
                            });
                        }
                    }
                }
                else{
                    float desiredValue = prevTemp - tempDiff;
                    float actualValue = prevTemp;
                    long currentTime = System.currentTimeMillis();
                    while(actualValue > desiredValue){
                        if(System.currentTimeMillis() > currentTime){
                            currentTime = System.currentTimeMillis();
                            actualValue = actualValue - diffTempOverTime;
                            final float finalActualValue = actualValue;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    tempSeekbar.setProgress((int) finalActualValue);
                                }
                            });
                        }
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {
            float diffPressOverTime = Math.abs(pressDiff)/1000;
            @Override
            public void run() {
                if(pressDiff < 0){
                    float desiredValue = prevTemp + Math.abs(pressDiff);
                    float actualValue = prevTemp;
                    long currentTime = System.currentTimeMillis();
                    while(actualValue < desiredValue){
                        if(System.currentTimeMillis() > currentTime){
                            currentTime = System.currentTimeMillis();
                            actualValue = actualValue + diffPressOverTime;
                            final float finalActualValue = actualValue;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pressSeekbar.setProgress((int) finalActualValue);
                                }
                            });
                        }
                    }
                }
                else{
                    float desiredValue = prevTemp - pressDiff;
                    float actualValue = prevTemp;
                    long currentTime = System.currentTimeMillis();
                    while(actualValue > desiredValue){
                        if(System.currentTimeMillis() > currentTime){
                            currentTime = System.currentTimeMillis();
                            actualValue = actualValue - diffPressOverTime;
                            final float finalActualValue = actualValue;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pressSeekbar.setProgress((int) finalActualValue);
                                }
                            });
                        }
                    }
                }
            }
        }).start();

    }

    private void selectAdvancedButton(AdvancedState.ActiveSection state){

        if(state == AdvancedState.ActiveSection.FROTH) {

            waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            label1.setText("");
            label2.setText("Pressure(psi):");
            label3.setText("Dispense(Oz):");

            tempSeekbar.setVisibility(View.VISIBLE);
            tempSeekbar.setEnabled(false);
            indicatorLayout1.setVisibility(View.VISIBLE);
            indicatorLayout1.setEnabled(false);

            pressSeekbar.setVisibility(View.VISIBLE);
            pressSeekbar.setEnabled(true);
            indicatorLayout2.setEnabled(true);

            dispSeekbar.setVisibility(View.VISIBLE);
            dispSeekbar.setEnabled(true);
            indicatorLayout3.setEnabled(true);
        }

        if(state == AdvancedState.ActiveSection.WATER) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            tempSeekbar.setEnabled(true);
            tempSeekbar.setVisibility(View.VISIBLE);
            indicatorLayout1.setVisibility(View.VISIBLE);
            indicatorLayout1.setEnabled(true);

            dispSeekbar.setEnabled(true);
            dispSeekbar.setVisibility(View.VISIBLE);
            indicatorLayout3.setEnabled(true);

            pressSeekbar.setVisibility(View.VISIBLE);
            pressSeekbar.setEnabled(false);
            indicatorLayout2.setEnabled(false);

            label1.setText("Temperature(" + (char) 0x00B0 + "F):");
            label2.setText("");
            label3.setText("Dispense(oz):");
        }

        if(state == AdvancedState.ActiveSection.MILK) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            tempSeekbar.setEnabled(true);
            tempSeekbar.setVisibility(View.VISIBLE);
            indicatorLayout1.setVisibility(View.VISIBLE);
            indicatorLayout1.setEnabled(true);

            dispSeekbar.setEnabled(true);
            dispSeekbar.setVisibility(View.VISIBLE);
            indicatorLayout3.setEnabled(true);

            pressSeekbar.setEnabled(false);
            pressSeekbar.setVisibility(View.VISIBLE);
            indicatorLayout2.setEnabled(false);

            label1.setText("Temperature(" + (char) 0x00B0 + "F):");
            label2.setText("");
            label3.setText("Dispense(oz):");
        }

        if(state == AdvancedState.ActiveSection.COFFEE) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            syrupButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            syrupButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(false);
            mySpinner.setVisibility(View.INVISIBLE);

            tempSeekbar.setVisibility(View.VISIBLE);
            tempSeekbar.setEnabled(false);
            indicatorLayout1.setVisibility(View.VISIBLE);
            indicatorLayout1.setEnabled(false);

            dispSeekbar.setVisibility(View.VISIBLE);
            dispSeekbar.setEnabled(true);
            indicatorLayout3.setEnabled(true);

            pressSeekbar.setVisibility(View.VISIBLE);
            pressSeekbar.setEnabled(false);
            indicatorLayout2.setEnabled(false);

            label1.setText("");
            label2.setText("");
            label3.setText("Dispense(oz):");
        }

        if(state == AdvancedState.ActiveSection.SYRUP) {

            frothButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
            frothButton.setTextColor(Color.parseColor("#ffefcc"));

            waterButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            waterButton.setTextColor(Color.parseColor("#ffefcc"));

            milkButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
            milkButton.setTextColor(Color.parseColor("#ffefcc"));

            coffeeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
            coffeeButton.setTextColor(Color.parseColor("#ffefcc"));

            mySpinner.setEnabled(true);
            mySpinner.setVisibility(View.VISIBLE);

            tempSeekbar.setVisibility(View.INVISIBLE);
            tempSeekbar.setEnabled(false);
            indicatorLayout1.setVisibility(View.INVISIBLE);
            indicatorLayout1.setEnabled(false);

            dispSeekbar.setEnabled(true);
            dispSeekbar.setVisibility(View.VISIBLE);
            indicatorLayout3.setEnabled(true);

            pressSeekbar.setVisibility(View.VISIBLE);
            pressSeekbar.setEnabled(false);
            indicatorLayout2.setEnabled(false);

            label1.setText("Syrup:");
            label2.setText("");
            label3.setText("Dispense(oz):");
        }
    }

    private void selectBasicButton(int row, Button button){
        switch(row){
            case 0:{
                if(button.getId() != R.id.basicAmountButton){
                    amountSmallButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                    amountSmallButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                if(button.getId() != R.id.basicAmountButton2){
                    amountMediumButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    amountMediumButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                if(button.getId() != R.id.basicAmountButton3){
                    amountLargeButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                    amountLargeButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                break;
            }
            case 1:{
                if(button.getId() != R.id.basicStrengthButton){
                    strengthMildButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                    strengthMildButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                if(button.getId() != R.id.basicStrengthButton2){
                    strengthRegularButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    strengthRegularButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                if(button.getId() != R.id.basicStrengthButton3){
                    strengthStrongButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                    strengthStrongButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                break;
            }
            case 2:{
                if(button.getId() != R.id.basicFrothButton){
                    frothNoneButton.setBackground(getDrawable(R.drawable.leftroundedunselected));
                    frothNoneButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                if(button.getId() != R.id.basicFrothButton2){
                    frothFrothyButton.setBackground(getDrawable(R.drawable.gridbuttonunselected));
                    frothFrothyButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                if(button.getId() != R.id.basicFrothButton3){
                    frothFrothiestButton.setBackground(getDrawable(R.drawable.rightroundedunselected));
                    frothFrothiestButton.setTextColor(Color.parseColor("#ffefcc"));
                }
                break;
            }
        }
    }

    private void setSeekBarValue(AdvancedState.ActiveSection state, int val, int bar){
        switch(state){
            case WATER:{
                if(bar == 0){
                    advancedState.waterState.temp = val;
                }
                else if (bar == 1){
                    //advancedState.waterState.press = val;
                }
                else{
                    advancedState.waterState.disp = val;
                }
                break;
            }
            case MILK:{
                if(bar == 0){
                    advancedState.milkState.temp = val;
                }
                else{
                    advancedState.milkState.disp = val;
                }
                break;
            }
            case COFFEE:{
                advancedState.coffeeState.disp = val;
                break;
            }
            case FROTH:{
                if (bar == 1){
                    advancedState.frothState.press = val;
                }
                else{
                    advancedState.frothState.disp = val;
                }
                break;
            }
            case SYRUP:{
                    advancedState.waterState.disp = val;
                break;
            }
        }
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wifiStatusReceiver);
        onStartNewActivity();
    }

    protected void onStartNewActivity() {
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }

    private void openLoginFragment(){
        LoginFragment fragment = LoginFragment.newInstance();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_from_top, R.anim.slide_to_top, R.anim.slide_from_top, R.anim.slide_to_top);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.add(R.id.fragmentContainer3, fragment, "LOGIN_FRAGMENT").commit();
    }

    private void openFavoritesFragment(){
        FavoritesFragment fragment = FavoritesFragment.newInstance(loginButton.getText().toString());
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.slide_from_bottom, R.anim.slide_to_bottom, R.anim.slide_from_bottom, R.anim.slide_to_bottom);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.add(R.id.fragmentContainer3, fragment, "FAVORITES_FRAGMENT").commit();
    }

    @Override
    public void onFragmentInteraction(RequestData requestData) {
        if(requestData != null) {
            this.requestData = requestData;
            favoritedBrew = true;
            playSliders(advancedState.activeSection, advancedState.activeSection);
        }
        getSupportFragmentManager().popBackStack();
    }

    @Override
    public void onFragmentInteraction(String sendBackUsername) {
        if(!sendBackUsername.isEmpty()){
            loginButton.setText(sendBackUsername);
            if(activeState == ActiveState.ADVANCED) {
                favoritesButton.setEnabled(true);
                favoritesButton.setVisibility(View.VISIBLE);
            }
        }
        getSupportFragmentManager().popBackStack();
    }

    public void onResponse() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setCancelable(false);
            if (!favoritedBrew && activeState == ActiveState.ADVANCED) {
                builder.setMessage("Would you like to save this particular brew?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);

                        final EditText edittext = new EditText(mContext);
                        alert.setTitle("Please enter the name you would like to save it under: ");

                        alert.setView(edittext);

                        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.dismiss();
                                final String saveName = edittext.getText().toString();
                                if (saveName != null && !saveName.isEmpty() && saveName.length() < 12) {
                                    final SharedPreferences sharedPreferences = getSharedPreferences("beanster", MODE_PRIVATE);
                                    if (sharedPreferences.contains("currentUser")) {
                                        if (!sharedPreferences.getString("currentUser", "").isEmpty()) {
                                            String currentUser = sharedPreferences.getString("currentUser", "");
                                            final Gson gson = new Gson();
                                            String json = sharedPreferences.getString("userData", "");
                                            try {
                                                final HashMap<String, UserData> userData = gson.fromJson(json, new TypeToken<HashMap<String, UserData>>() {
                                                }.getType());
                                                currentUserData = userData.get(currentUser);
                                                if (currentUserData == null)
                                                    Log.i("CoffeeBrew", "currentUserData is null");
                                                else
                                                    Log.i("CoffeeBrew", "Trying to save " + saveName + " for user: " + currentUserData.getUsername());
                                                if (requestData == null)
                                                    Log.i("CoffeeBrew", "requestData is null!");
                                                boolean exists = false;
                                                Log.i("CoffeeBrew", "CurrentUserData favorites");
                                                for (final String s : currentUserData.getFavorites().keySet()) {
                                                    final RequestData r = currentUserData.getFavorites().get(s);
                                                    if (requestData.equals(r)) {
                                                        Log.i("CoffeeBrew", "Brew already existed");
                                                        Log.i("CoffeeBrew", "requestData for " + s + ":");
                                                        Log.i("CoffeeBrew", currentUserData.getFavorites().get(s).toString());
                                                        Log.i("CoffeeBrew", "current request data: \n" + requestData.toString());
                                                        exists = true;
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                                        builder.setCancelable(false);
                                                        builder.setMessage("These brew settings already exists, continue saving with new name?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                currentUserData.getFavorites().remove(s);
                                                                currentUserData.addFavorite(saveName, r);
                                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                                String json = gson.toJson(userData);
                                                                editor.putString("userData", json).apply();
                                                                Toast toast = Toast.makeText(mContext, String.format("Favorite %s saved...", saveName), Toast.LENGTH_LONG);
                                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                                toast.show();
                                                            }
                                                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                dialog.cancel();
                                                            }
                                                        }).show();
                                                    }
                                                    if(s.equals(saveName)){
                                                        exists = true;
                                                        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                                                        builder.setCancelable(false);
                                                        builder.setMessage("This favorite name already exists, would you like to overwrite the data?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                currentUserData.getFavorites().remove(s);
                                                                currentUserData.addFavorite(saveName, requestData);
                                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                                String json = gson.toJson(userData);
                                                                editor.putString("userData", json).apply();
                                                                Toast toast = Toast.makeText(mContext, String.format("Favorite %s saved...", saveName), Toast.LENGTH_LONG);
                                                                toast.setGravity(Gravity.CENTER, 0, 0);
                                                                toast.show();
                                                            }
                                                        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                dialog.cancel();
                                                            }
                                                        }).show();
                                                    }
                                                }
                                                if (!exists) {
                                                    currentUserData.addFavorite(saveName, requestData);
                                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                                    json = gson.toJson(userData);
                                                    editor.putString("userData", json).apply();
                                                    Toast toast = Toast.makeText(mContext, String.format("Favorite %s saved...", saveName), Toast.LENGTH_LONG);
                                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                                    toast.show();
                                                }


                                            } catch (Exception e) {
                                                Log.i("CoffeeBrew", e.getLocalizedMessage());
                                            }
                                        }
                                    }
                                } else if (saveName.length() > 12) {
                                    Toast toast = Toast.makeText(mContext, "Save name must be shorter than 12 characters...", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                } else {
                                    Toast toast = Toast.makeText(mContext, "Save name cannot be empty...", Toast.LENGTH_LONG);
                                    toast.setGravity(Gravity.CENTER, 0, 0);
                                    toast.show();
                                }
                                dialog.cancel();
                                imm.toggleSoftInput(InputMethodManager.RESULT_HIDDEN, 0);
                            }
                        });

                        alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                dialog.cancel();
                                imm.toggleSoftInput(InputMethodManager.RESULT_HIDDEN, 0);
                            }
                        });

                        alert.show();
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        favoritedBrew = false;
                        dialog.cancel();
                    }
                }).show();
            }
        } catch (Exception e) {
            Log.i("CoffeeBrew", e.getLocalizedMessage());
        }
    }
}
