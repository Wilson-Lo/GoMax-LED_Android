package com.gomax.led.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.gomax.led.R;
import com.gomax.led.http.CmdHelper;
import com.gomax.led.http.OKHttpHelper;

import com.gomax.led.main.MainActivity;
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;


public class FragmentSystem extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "FragmentSystem";
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;
    private Toolbar toolbar;
    private static ColorPreferenceCompat textColorPreference, backgroundColorPreference;
    private static boolean isTextColorInitial = true;
    private static boolean isBackgroundColorInitial = true;

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

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.fragment_system_preference_screeen, rootKey);

        textColorPreference = (ColorPreferenceCompat) findPreference(getString(R.string.preference_key_text_color));
        textColorPreference.setOnPreferenceChangeListener(this);

        backgroundColorPreference = (ColorPreferenceCompat) findPreference(getString(R.string.preference_key_background_color));
        backgroundColorPreference.setOnPreferenceChangeListener(this);

    }


    @Override
    public void onStart() {
        setSystemLanguage("en");
        isTextColorInitial = true;
        isBackgroundColorInitial = true;
        new Thread() {
            @Override
            public void run() {
                new OKHttpHelper(pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, ""), "api/led/all", FragmentHelper.FRAGMENT_EVENT_GET_ALL_SYSTEM).methodGet();
            }
        }.start();
        super.onStart();
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
                        if (jsonObject.has(CmdHelper.JSON_KEY_RESULT)) {
                            Toast.makeText(MainActivity.mActivity.get(), "Get system info failed !", Toast.LENGTH_LONG).show();
                        } else {


                            //set background rgb
                            if (jsonObject.has(CmdHelper.JSON_KEY_BACKGROUND_RGB)) {
                                JSONObject jsonObjectBackgroundRGB = jsonObject.getJSONObject(CmdHelper.JSON_KEY_BACKGROUND_RGB);
                                String hex = String.format("#%02x%02x%02x", jsonObjectBackgroundRGB.getInt(CmdHelper.JSON_KEY_R),
                                        jsonObjectBackgroundRGB.getInt(CmdHelper.JSON_KEY_G), jsonObjectBackgroundRGB.getInt(CmdHelper.JSON_KEY_B));
                                int colorInt = Color.parseColor(hex);
                                backgroundColorPreference.saveValue(colorInt);
                            }

                            //set text rgb
                            if (jsonObject.has(CmdHelper.JSON_KEY_TEXT_RGB)) {
                                JSONObject jsonObjectBackgroundRGB = jsonObject.getJSONObject(CmdHelper.JSON_KEY_TEXT_RGB);
                                String hex = String.format("#%02x%02x%02x", jsonObjectBackgroundRGB.getInt(CmdHelper.JSON_KEY_R),
                                        jsonObjectBackgroundRGB.getInt(CmdHelper.JSON_KEY_G), jsonObjectBackgroundRGB.getInt(CmdHelper.JSON_KEY_B));
                                int colorInt = Color.parseColor(hex);
                                textColorPreference.saveValue(colorInt);
                            }


                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case FragmentHelper.FRAGMENT_EVENT_SET_BACKGROUND_RGB:
                    Log.d(TAG, "FRAGMENT_EVENT_SET_BACKGROUND_RGB " + (String) msg.obj);
                    try {
                        JSONObject jsonObject = new JSONObject((String) msg.obj);
                        if (jsonObject.getString(CmdHelper.JSON_KEY_RESULT).equals("ok")) {
                            Toast.makeText(MainActivity.mActivity.get(), "Set background color successful !", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.mActivity.get(), "Set background color failed !", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case FragmentHelper.FRAGMENT_EVENT_SET_TEXT_RGB:
                    Log.d(TAG, "FRAGMENT_EVENT_SET_TEXT_RGB " + (String) msg.obj);
                    try {
                        JSONObject jsonObject = new JSONObject((String) msg.obj);
                        if (jsonObject.getString(CmdHelper.JSON_KEY_RESULT).equals("ok")) {
                            Toast.makeText(MainActivity.mActivity.get(), "Set text color successful !", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(MainActivity.mActivity.get(), "Set text color failed !", Toast.LENGTH_LONG).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }

            super.handleMessage(msg);
        }
    };


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newDefaultColor = Integer.toHexString((int) newValue);

        switch (preference.getKey()) {

            case "BACKGROUND_COLOR":
                Log.d(TAG, "BACKGROUND_COLOR : #" + newDefaultColor);

                if (isBackgroundColorInitial) {
                    isBackgroundColorInitial = false;
                } else {
                    int b_color = (int) Long.parseLong("" + newDefaultColor, 16);
                    int b_r = (b_color >> 16) & 0xFF;
                    int b_g = (b_color >> 8) & 0xFF;
                    int b_b = (b_color >> 0) & 0xFF;

                    Log.d(TAG, "BACKGROUND_COLOR : R = " + b_r + " G = " + b_g + " B = " + b_b);

                    final String jsonBody_background = "{\n" +
                            "  \"r\": " + b_r + ",\n" +
                            "  \"g\": " + b_g + ",\n" +
                            "  \"b\": " + b_b + "\n" +
                            "}";
                    new Thread() {
                        @Override
                        public void run() {
                            new OKHttpHelper(pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, ""), "api/led/background_rgb",
                                    FragmentHelper.FRAGMENT_EVENT_SET_BACKGROUND_RGB).methodPost(jsonBody_background);
                        }
                    }.start();
                }
                break;

            case "TEXT_COLOR":
                Log.d(TAG, "TEXT_COLOR : #" + newDefaultColor);

                if (isTextColorInitial) {
                    isTextColorInitial = false;
                } else {
                    int t_color = (int) Long.parseLong("" + newDefaultColor, 16);
                    int t_r = (t_color >> 16) & 0xFF;
                    int t_g = (t_color >> 8) & 0xFF;
                    int t_b = (t_color >> 0) & 0xFF;

                    Log.d(TAG, "TEXT_COLOR : R = " + t_r + " G = " + t_g + " B = " + t_b);

                    final String jsonBody_text = "{\n" +
                            "  \"r\": " + t_r + ",\n" +
                            "  \"g\": " + t_g + ",\n" +
                            "  \"b\": " + t_b + "\n" +
                            "}";

                    new Thread() {
                        @Override
                        public void run() {
                            new OKHttpHelper(pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, ""), "api/led/text_rgb",
                                    FragmentHelper.FRAGMENT_EVENT_SET_TEXT_RGB).methodPost(jsonBody_text);
                        }
                    }.start();
                }
                break;
        }
        return true;
    }

    /**
     * Set System Language
     * @param country : country code. ex: USA = en
     */
    private void setSystemLanguage(String country){
        Locale locale = new Locale(country);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getContext().getResources().updateConfiguration(config, getContext().getResources().getDisplayMetrics());
    }


}

