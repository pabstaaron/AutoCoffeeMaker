package com.example.ndonaldson.beanster;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.HashSet;


/**
 */
public class LoginFragment extends Fragment {

    private Button cancelButton;
    private Button loginButton;
    private Button createButton;
    private EditText usernameText;
    private EditText passwordText;
    private Context mContext;
    private HashMap<String, UserData> userData;

    private SharedPreferences sharedPreferences;

    private OnFragmentInteractionListener mListener;

    public LoginFragment() {
        // Required empty public constructor
    }

    /**
     * @return A new instance of fragment LoginFragment.
     */
    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = this.getActivity().getSharedPreferences("beanster", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString("userData", "");
        userData = gson.fromJson(json, HashMap.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        cancelButton = (Button) view.findViewById(R.id.fragmentCancelButton);
        loginButton = (Button) view.findViewById(R.id.fragmentLoginButton);
        createButton = (Button) view.findViewById(R.id.fragmentCreateButton);
        usernameText = (EditText) view.findViewById(R.id.fragmentUserName);
        passwordText = (EditText) view.findViewById(R.id.fragmentPassword);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBack("");
            }
        });
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameText.getText().toString();
                String pass = passwordText.getText().toString();
                if(!user.isEmpty() && !pass.isEmpty()){
                    if(userData.containsKey(user)){
                        UserData cachedUser = userData.get(user);
                        if(cachedUser.getPassword().equals(pass)){
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            Gson gson = new Gson();
                            String json = gson.toJson(cachedUser);
                            if(!sharedPreferences.contains("currentUser")){
                                editor.putString("currentUser", json).commit();
                            }
                            else{
                                editor.putString("currentUser", json).apply();
                            }
                            sendBack(cachedUser.getUsername());
                        }
                    }
                }
            }
        });
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameText.getText().toString();
                String pass = passwordText.getText().toString();
                if(!user.isEmpty() && !pass.isEmpty()){
                    if(userData.containsKey(user)){
                        Toast toast = new Toast(getActivity().getApplicationContext());
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.setText("User already exists...");
                        toast.show();
                    }
                }
                else{
                    userData.put(user, new UserData(user, pass));
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    Gson gson = new Gson();
                    String json = gson.toJson(userData);
                    editor.putString("userData", json).apply();
                    sendBack(user);
                }
            }
        });

        return view;
    }

    public void sendBack(String sendBackUsername) {
        if (mListener != null) {
            mListener.onFragmentInteraction(sendBackUsername);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String sendBackUsername);
    }
}
