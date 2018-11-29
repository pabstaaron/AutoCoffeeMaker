package com.example.ndonaldson.beanster.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.example.ndonaldson.beanster.R;
import com.example.ndonaldson.beanster.data.RequestData;
import com.example.ndonaldson.beanster.wifi.SelectableWifi;
import com.example.ndonaldson.beanster.data.UserData;
import com.example.ndonaldson.beanster.wifi.WifiAdapter;
import com.example.ndonaldson.beanster.wifi.WifiSelectItem;
import com.example.ndonaldson.beanster.wifi.WifiViewHolder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ndonaldson on 10/8/18.
 */

public class FavoritesFragment extends Fragment implements WifiViewHolder.OnItemSelectedListener {

    private RequestData requestData;
    private String requestDataName;
    private HashMap<String, UserData> userData;
    private UserData currentUserData;
    private SharedPreferences sharedPreferences;
    private Button cancelButton;
    private Button okayButton;
    private Button clearButton;
    private String user;
    private RecyclerView recyclerView;
    private WifiAdapter adapter;
    private Context mContext;

    private FavoritesFragment.OnFragmentInteractionListener mListener;

    public FavoritesFragment() {
        // Required empty public constructor
    }


    /**
     * @return A new instance of fragment LoginFragment.
     */
    public static FavoritesFragment newInstance(String user) {
        FavoritesFragment fragment = new FavoritesFragment();
        Bundle args = new Bundle();
        args.putString("user", user);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (FavoritesFragment.OnFragmentInteractionListener) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("Favorites", "onCreate called!");
        if(getArguments() == null || !getArguments().containsKey("user")){
            Log.i("Favorites", "arguments are null!");
            sendBack(null);
            return;
        }
        else{
            if(getArguments().getString("user") == null || getArguments().getString("user").isEmpty()){
                Log.i("Favorites", "user is null or empty!");
                sendBack(null);
                return;
            } else {
                Log.i("Favorites", "setting user!");
                user = getArguments().getString("user");
            }
        }
        sharedPreferences = this.getActivity().getSharedPreferences("beanster", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("userData", "");
        try {
            userData = gson.fromJson(json, new TypeToken<HashMap<String, UserData>>() {
            }.getType());
        } catch(Exception e){
            Log.i("Favorites", e.getLocalizedMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        try {
            recyclerView = (RecyclerView) view.findViewById(R.id.favorites);
            recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

            currentUserData = userData.get(user);

            List<WifiSelectItem> selectableItems = generateItems();
            setAdapter(selectableItems);

            cancelButton = (Button) view.findViewById(R.id.fragmentCancelButton2);
            okayButton = (Button) view.findViewById(R.id.fragmentOkayButton);
            clearButton = (Button) view.findViewById(R.id.fragmentClearButton);

            clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentUserData.getFavorites().remove(requestDataName);
                    List<WifiSelectItem> selectableItems = generateItems();
                    setAdapter(selectableItems);
                    Gson gson = new Gson();
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    String json = gson.toJson(userData);
                    editor.putString("userData", json).commit();
                    okayButton.setEnabled(false);
                    clearButton.setEnabled(false);
                    Toast toast = Toast.makeText(getActivity(), "Favorite deleted...", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();

                    okayButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
                    okayButton.setTextColor(Color.rgb(204, 204, 204));
                    okayButton.setEnabled(false);

                    clearButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
                    clearButton.setTextColor(Color.rgb(204, 204, 204));
                    clearButton.setEnabled(false);
                }
            });
            okayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendBack(currentUserData.getFavorites().get(requestDataName));
                }
            });
            okayButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
            okayButton.setTextColor(Color.rgb(204, 204, 204));
            okayButton.setEnabled(false);

            clearButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
            clearButton.setTextColor(Color.rgb(204, 204, 204));
            clearButton.setEnabled(false);
            requestDataName = "";

            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendBack(null);
                }
            });

        } catch(Exception e){
            Log.i("Favorites", e.getLocalizedMessage());
        }

        return view;
    }

    public void sendBack(RequestData requestData) {
        if (mListener != null) {
            mListener.onFragmentInteraction(requestData);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemSelected(SelectableWifi item) {
        if(adapter.getSelectedItems().isEmpty()) {
            okayButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
            okayButton.setTextColor(Color.rgb(204, 204, 204));
            okayButton.setEnabled(false);

            clearButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
            clearButton.setTextColor(Color.rgb(204, 204, 204));
            clearButton.setEnabled(false);
            requestDataName = "";
        }
        else {
            okayButton.setEnabled(true);
            okayButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyle));
            okayButton.setTextColor(Color.rgb(255, 239, 204));

            clearButton.setEnabled(true);
            clearButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyle));
            clearButton.setTextColor(Color.rgb(255, 239, 204));
            requestDataName = item.getDeviceID();
        }
    }

    /**
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(RequestData requestData);
    }


    /**
     * Create all the items in the recyclerView based on the raspberry PI's on the LAN found by WifiRunner scan
     * @return
     */
    public List<WifiSelectItem> generateItems(){

        List<WifiSelectItem> selectableItems = new ArrayList<>();

        if(currentUserData.getFavorites() != null && currentUserData.getFavorites().keySet() != null && !currentUserData.getFavorites().keySet().isEmpty()) {

            for (String r : currentUserData.getFavorites().keySet()) {
                Log.i("Favorites", "Adding " + r + " to the list");
                selectableItems.add(new WifiSelectItem(r));
            }
        }

        return selectableItems;
    }

    public void setAdapter(List<WifiSelectItem> items){
        adapter = new WifiAdapter(this, items, false, null);
        recyclerView.setAdapter(adapter);
    }
}
