package com.example.ndonaldson.beanster;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by ndonaldson on 7/31/18.
 */

public class MySpinnerAdapter extends ArrayAdapter<String> {

    private String[] Syrups;
    private Context context;
    private int textViewResourceId;

    public MySpinnerAdapter(Context context, int textViewResourceId,
                           String[] Syrups) {
        super(context, textViewResourceId, Syrups);
        this.Syrups = Syrups;
        this.context = context;
        this.textViewResourceId = textViewResourceId;
    }

    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    public View getCustomView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater=LayoutInflater.from(context);
        View row=inflater.inflate(R.layout.row, parent, false);
        TextView label=(TextView)row.findViewById(R.id.syrup);
        label.setText(Syrups[position]);

        if(position == 0){
            label.setTextColor(0xFFF00000);
        }

        return row;
    }
}