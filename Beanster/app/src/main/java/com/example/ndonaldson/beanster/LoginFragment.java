package com.example.ndonaldson.beanster;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;


/**
 */
public class LoginFragment extends Fragment {

    private Button cancelButton;
    private Button loginButton;
    private Button createButton;
    private EditText usernameText;
    private EditText passwordText;

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
        sharedPreferences = this.getActivity().getSharedPreferences("pref", Context.MODE_PRIVATE);
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

            }
        });
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

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
