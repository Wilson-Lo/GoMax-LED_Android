package com.gomax.led.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.Log;

import com.gomax.led.R;
import com.gomax.led.fragment.FragmentHelper;
import com.gomax.led.fragment.FragmentSettings;
import com.gomax.led.fragment.FragmentSystem;
import com.gomax.led.http.OKHttpHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.lang.ref.WeakReference;

import me.majiajie.pagerbottomtabstrip.NavigationController;
import me.majiajie.pagerbottomtabstrip.PageNavigationView;
import me.majiajie.pagerbottomtabstrip.listener.OnTabItemSelectedListener;

public class MainActivity extends AppCompatActivity {

    public static WeakReference<MainActivity> mActivity = null;
    private static String TAG = "MainActivity";

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
}
