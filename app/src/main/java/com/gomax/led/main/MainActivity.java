package com.gomax.led.main;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.gomax.led.R;
import com.gomax.led.fragment.FragmentHelper;
import com.gomax.led.fragment.FragmentSettings;
import com.gomax.led.fragment.FragmentSystem;
import com.gomax.led.http.OKHttpHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Locale;

import me.majiajie.pagerbottomtabstrip.NavigationController;
import me.majiajie.pagerbottomtabstrip.PageNavigationView;
import me.majiajie.pagerbottomtabstrip.listener.OnTabItemSelectedListener;

public class MainActivity extends AppCompatActivity {

    private static final String MARKET_URL = "https://play.google.com/store/apps/details?id=com.gomax.led";
    public static WeakReference<MainActivity> mActivity = null;
    private static String TAG = "MainActivity";
    private static final int UPDATE_APP_EVENT = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = new WeakReference<MainActivity>(this);

        PageNavigationView tab = (PageNavigationView) findViewById(R.id.tab);

        NavigationController navigationController = tab.material()
                .addItem(R.drawable.info, "System")
                .addItem(R.drawable.settings, "Settings")
                .build();

        navigationController.addTabItemSelectedListener(new OnTabItemSelectedListener() {
            @Override
            public void onSelected(int index, int old) {

                Log.d(TAG, "index = " + index);

                switch(index){

                    case FragmentHelper.FRAGMENT_SYSTEM:
                        replaceFragments(FragmentSystem.class);
                        break;

                    case FragmentHelper.FRAGMENT_SETTING:
                        replaceFragments(FragmentSettings.class);
                        break;
                }
            }

            @Override
            public void onRepeat(int index) {

            }
        });
        replaceFragments(FragmentSystem.class);
    }


    @Override
    protected void onStart() {
        Locale locale = new Locale("en");
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        if (isNetworkConnected()) {
            isAPPNeedToUpdate();
        }
        super.onStart();
    }

    /**
     * Replace container fragment
     *
     * @param fragmentClass : new fragment class to change to.
     */
    public static void replaceFragments(Class fragmentClass) {
        Fragment fragment = null;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            //     MainActivity.uiHandler.obtainMessage(CmdHelper._12_ui_show_toast, 0, -1, MainActivity.mActivity.get().getResources().getString(R.string.error_message_type1)).sendToTarget();
        }
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = mActivity.get().getSupportFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.frameLayout_container, fragment).
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
    }

    /**
     * Check mobile internet status.
     *
     * @return : true: has internet, false: non
     */
    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Check market has newest version or not.
     */
    private void isAPPNeedToUpdate() {
        Thread checkMarketVersionTheard = new Thread() {
            public void run() {
                Document doc;
                String lastVersion = "";
                String currentVersion = "";
                try {
                    currentVersion = getVersionName();
                    //It retrieves the latest version by scraping the content of current version from play store at runtime
                    doc = Jsoup.connect(MARKET_URL).get();
                    lastVersion = doc.getElementsByClass("htlgb").get(6).text();
                    Log.d(TAG, "Your current version = " + currentVersion);
                    Log.d(TAG, "get version from market  = " + lastVersion);
                } catch (IOException e) {
                    Log.d(TAG, "test error = " + e.getMessage());
                    // ConnectTypeActivity.uiHandler.obtainMessage(ERROR_INTERNET_EVENT, 0, -1, "Please check your's mobile internet connection and reopen App again !").sendToTarget();
                    e.printStackTrace();
                }
                if ((lastVersion != null) && (!lastVersion.isEmpty())) {
                    Log.d(TAG, "GetLatestVersion 11111111");
                    if (!currentVersion.equalsIgnoreCase(lastVersion)) {
                        Log.d(TAG, "Need to update");
                        MainActivity.uiHandler.obtainMessage(UPDATE_APP_EVENT, 0, -1, "").sendToTarget();
                    } else {
                        Log.d(TAG, "Your apk version is newest");
                    }
                }
            }
        };
        checkMarketVersionTheard.start();
    }

    /**
     * Handler server message
     */
    public static Handler uiHandler = new Handler() {

        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(@NonNull Message msg) {

            switch (msg.what) {

                case UPDATE_APP_EVENT:
                    Log.d(TAG, "UPDATE_APP_EVENT");
                    showDialogUpdate();
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /**
     * get app version name
     *
     * @return : version name
     */
    public static String getVersionName() {
        PackageManager packageManager = mActivity.get().getPackageManager();
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(mActivity.get().getPackageName(), 0);
            Log.d(TAG, "" + packInfo.versionCode);
            Log.d(TAG, "" + packInfo.versionName);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Show update app version dialog
     */
    private static void showDialogUpdate() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity.get());
        // setup dialog title
        builder.setTitle("Notice").
                setCancelable(false).
                setIcon(R.mipmap.ic_launcher).//setup dialog icon
                setMessage("The APP has the newest version !").
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final String appPackageName = mActivity.get().getPackageName(); // getPackageName() from Context or Activity object
                        mActivity.get().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URL)));
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.d(TAG, "click cancel");
                // new CreateCommThreadTask().execute();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
