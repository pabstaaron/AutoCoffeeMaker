package com.example.ndonaldson.beanster;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class CoffeeBrew extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        overridePendingTransition(R.anim.slide_in, R.anim.slide_out);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coffee_brew);
    }
}
