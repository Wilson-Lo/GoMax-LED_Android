package com.gomax.led.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.gomax.led.http.CmdHelper;
import com.gomax.led.http.OKHttpHelper;
import com.gomax.led.main.MainActivity;

import org.json.JSONException;
import org.json.JSONObject;

import static android.content.Context.MODE_PRIVATE;


public class FragmentSystem extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentSystem";
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;

    public static FragmentSystem newInstance(int index) {
        FragmentSystem fm = new FragmentSystem();
        Bundle args = new Bundle();
        args.putInt("index", index);
        fm.setArguments(args);
        return fm;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "FragmentSystem-onCreate");
        pref = getContext().getSharedPreferences(CmdHelper.SHAREDPREFERENCE_DB_NAME, MODE_PRIVATE);
        editor = pref.edit();
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        new Thread() {
            @Override
            public void run() {
                new OKHttpHelper(pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, ""), "api/led/all", FragmentHelper.FRAGMENT_EVENT_GET_ALL_SYSTEM).methodGet();
            }
        }.start();
        super.onStart();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

        }

    }

    //handle command result
    public static Handler commandHandler = new Handler() {

        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(@NonNull Message msg) {

            switch (msg.what) {

                case FragmentHelper.FRAGMENT_EVENT_GET_ALL_SYSTEM:
                    Log.d(TAG, "FRAGMENT_EVENT_GET_ALL_SYSTEM " + (String) msg.obj);
                    try {
                        JSONObject jsonObject = new JSONObject((String) msg.obj);
                        if (jsonObject.getString(CmdHelper.JSON_KEY_RESULT).equals("ok")) {
                            Toast.makeText(MainActivity.mActivity.get(), "Set hostname successful !", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.mActivity.get(), "Set hostname failed !", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }

            super.handleMessage(msg);
        }
    };
}

