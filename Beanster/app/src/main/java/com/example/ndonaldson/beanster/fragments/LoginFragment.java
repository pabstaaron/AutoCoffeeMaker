package com.example.ndonaldson.beanster.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.ndonaldson.beanster.R;
import com.example.ndonaldson.beanster.data.UserData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;


/**
 */
public class LoginFragment extends Fragment {

    private Button cancelButton;
    private Button loginButton;
    private Button createButton;
    private EditText usernameText;
    private EditText passwordText;
    private HashMap<String, UserData> userData;

    private SharedPreferences sharedPreferences;

    private OnFragmentInteractionListener mListener;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnFragmentInteractionListener) activity;
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
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        cancelButton = (Button) view.findViewById(R.id.fragmentCancelButton);
        loginButton = (Button) view.findViewById(R.id.fragmentLoginButton);
        createButton = (Button) view.findViewById(R.id.fragmentCreateButton);
        usernameText = (EditText) view.findViewById(R.id.fragmentUserName);
        passwordText = (EditText) view.findViewById(R.id.fragmentPassword);

        usernameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().trim().length() == 10){
                    usernameText.setError("Maximum length reached...");
                } else{
                    usernameText.setError(null);
                }
            }
        });

        passwordText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().trim().length() == 10){
                    passwordText.setError("Maximum length reached...");
                } else{
                    passwordText.setError(null);
                }
            }
        });

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
            try {
                if (!user.isEmpty() && !pass.isEmpty()) {
                    if (userData.containsKey(user)) {
                        UserData cachedUser = userData.get(user);
                        if (cachedUser.getPassword().equals(pass)) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            if (!sharedPreferences.contains("currentUser")) {
                                editor.putString("currentUser", cachedUser.getUsername()).commit();
                            } else {
                                editor.putString("currentUser", cachedUser.getUsername()).apply();
                            }
                            sendBack(cachedUser.getUsername());
                        }
                        else{
                            Toast.makeText(getActivity(), "Incorrect password...", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "User does not exist...", Toast.LENGTH_LONG).show();
                    }
                }

            } catch(Exception e){
                Log.i("LoginFragment", e.getLocalizedMessage());
            }
            }
        });
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            String user = usernameText.getText().toString();
            String pass = passwordText.getText().toString();

            if(userData.containsKey(user)){
                Toast.makeText(getActivity(), "User already exists...", Toast.LENGTH_LONG ).show();
            }
            else if(user.length() < 8 || pass.length() < 16){
                userData.put(user, new UserData(user, pass));
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Gson gson = new Gson();
                String json = gson.toJson(userData);
                editor.putString("userData", json).apply();
                if (!sharedPreferences.contains("currentUser")) {
                    editor.putString("currentUser", user).commit();
                } else {
                    editor.putString("currentUser", user).apply();
                }
                Toast.makeText(getActivity(), "User successfully created...", Toast.LENGTH_LONG ).show();
                sendBack(user);
            }
            }
        });

        createButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
        createButton.setTextColor(Color.rgb(204, 204, 204));
        createButton.setEnabled(false);

        loginButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyledisable));
        loginButton.setTextColor(Color.rgb(204, 204, 204));
        loginButton.setEnabled(false);

        usernameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() > 0){
                    if(passwordText.length() > 0){
                        createButton.setEnabled(true);
                        createButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyle));
                        createButton.setTextColor(Color.rgb(255, 239, 204));

                        loginButton.setEnabled(true);
                        loginButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyle));
                        loginButton.setTextColor(Color.rgb(255, 239, 204));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        passwordText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if(charSequence.length() > 0){
                    if(usernameText.length() > 0){
                        createButton.setEnabled(true);
                        createButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyle));
                        createButton.setTextColor(Color.rgb(255, 239, 204));

                        loginButton.setEnabled(true);
                        loginButton.setBackground(getActivity().getDrawable(R.drawable.buttonstyle));
                        loginButton.setTextColor(Color.rgb(255, 239, 204));
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        usernameText.requestFocus();

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
