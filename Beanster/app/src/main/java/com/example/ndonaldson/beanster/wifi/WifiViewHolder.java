package com.example.ndonaldson.beanster.wifi;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.CheckedTextView;

import com.example.ndonaldson.beanster.R;

/**
 * The display for each card in the recyclerview
 * Created by ndonaldson on 5/21/18.
 */

public class WifiViewHolder extends RecyclerView.ViewHolder {

    public static final int MULTI_SELECTION = 2;
    public static final int SINGLE_SELECTION = 1;
    CheckedTextView textView;
    SelectableWifi mItem;
    OnItemSelectedListener itemSelectedListener;


    /**
     * Sets onClickListener for the card.
     * @param view
     * @param listener
     */
    public WifiViewHolder(View view, OnItemSelectedListener listener) {
        super(view);
        itemSelectedListener = listener;
        textView = (CheckedTextView) view.findViewById(R.id.checked_text_item);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                if (mItem.isSelected() && getItemViewType() == MULTI_SELECTION) {
                    setChecked(false);
                } else if(mItem.isSelected()) {
                    setChecked(false);
                } else
                    setChecked(true);
                itemSelectedListener.onItemSelected(mItem);

            }
        });
    }

    /**
     * Sets color scheme and selection of card
     * @param value
     */
    public void setChecked(boolean value) {

        int color = -1;
        Drawable background = textView.getBackground();
        if (background instanceof ColorDrawable)
            color = ((ColorDrawable) background).getColor();
        if(color == Color.LTGRAY) {
            textView.setBackgroundColor(Color.DKGRAY);
            Log.i("WifiViewHolder", "Changing to DKGRAY!");
        }
        else if(color == Color.DKGRAY) {
            textView.setBackgroundColor(Color.LTGRAY);
            Log.i("WifiViewHolder", "Changing to LTGRAY!");
        }
        else {
            textView.setBackgroundColor(Color.LTGRAY);
            Log.i("WifiViewHolder", "Changing to DEFAULT LTGRAY!");
        }

        mItem.setSelected(value);
        textView.setChecked(value);
    }

    public void setColor(int color){
        textView.setBackgroundColor(color);
    }

    /**
     * Used in DeviceSelection to decide what to do when selecting item.
     */
    public interface OnItemSelectedListener {

        void onItemSelected(SelectableWifi item);
    }

}