package com.example.ndonaldson.beanster;

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
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by ndonaldson on 10/8/18.
 */

public class FavoritesFragment extends Fragment implements WifiViewHolder.OnItemSelectedListener{

    private RequestData requestData;
    private String requestDataName;
    private HashMap<String, UserData> userData;
    private UserData currentUserData;
    private SharedPreferences sharedPreferences;
    private Button cancelButton;
    private Button okayButton;
    private String user;
    private RecyclerView recyclerView;
    private WifiAdapter adapter;

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
        if(!savedInstanceState.keySet().contains("user")){
            sendBack(null);
            return;
        }
        else{
            user = savedInstanceState.getString("user");
        }
        sharedPreferences = this.getActivity().getSharedPreferences("beanster", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("userData", "");
        try {
            userData = gson.fromJson(json, new TypeToken<HashMap<String, UserData>>() {
            }.getType());
        } catch(Exception e){
            Log.i("LoginFragment", e.getLocalizedMessage());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);


        recyclerView = (RecyclerView) view.findViewById(R.id.favorites);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        currentUserData = userData.get(user);

        List<WifiSelectItem> selectableItems = generateItems();
        adapter = new WifiAdapter(this, selectableItems, false);
        recyclerView.setAdapter(adapter);

        cancelButton = (Button) view.findViewById(R.id.fragmentCancelButton2);
        okayButton = (Button) view.findViewById(R.id.fragmentOkayButton);


        okayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBack(currentUserData.getFavorites().get(requestDataName));
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBack(null);
            }
        });

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
            okayButton.setEnabled(false);
            requestDataName = "";
        }
        else {
            okayButton.setEnabled(true);
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
        selectableItems.clear();
        for(String r: currentUserData.getFavorites().keySet()){
            selectableItems.add(new WifiSelectItem(r));
        }

        return selectableItems;
    }
}
