package com.gomax.led.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.gomax.led.BuildConfig;
import com.gomax.led.R;
import com.gomax.led.adapter.DeviceListAdapter;
import com.gomax.led.dialog.DialogDeviceList;
import com.gomax.led.http.CmdHelper;
import com.gomax.led.http.OKHttpHelper;
import com.gomax.led.main.MainActivity;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static android.content.Context.MODE_PRIVATE;

public class FragmentSettings extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentSettings";
    private Thread receiveUPDThread;
    private DatagramSocket clientReceiveSocket = null;
    private String UPD_COMMAND = "led_ip";
    private Toolbar toolbar;
    private static SweetAlertDialog pDialog;
    private static ArrayList<String> deviceObjectList;
    private DeviceListAdapter deviceListAdapter;
    private static DialogDeviceList dialogDeviceList;
    private static TextInputEditText textInputEditTextIP, textInputEditTextHostname;
    private ImageButton imageButtonHostname;
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;

    public static FragmentSettings newInstance(int index) {
        FragmentSettings fm = new FragmentSettings();
        Bundle args = new Bundle();
        args.putInt("index", index);
        fm.setArguments(args);
        return fm;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "FragmentSetting-onCreate");
        pref = getContext().getSharedPreferences(CmdHelper.SHAREDPREFERENCE_DB_NAME, MODE_PRIVATE);
        editor = pref.edit();
        receiveUPDThread = new Thread(new ReceiveUDPTask());
        receiveUPDThread.start();
        deviceObjectList = new ArrayList<>();
        deviceListAdapter = new DeviceListAdapter(getContext(), deviceObjectList);
        pDialog = new SweetAlertDialog(getContext(), SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setCancelable(false);
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_settings_layout, container, false);
        AppCompatActivity mActivity = (AppCompatActivity) getActivity();
        textInputEditTextIP = (TextInputEditText) rootView.findViewById(R.id.textinput_ip);
        textInputEditTextHostname = (TextInputEditText) rootView.findViewById(R.id.textinput_hostname);
        imageButtonHostname = (ImageButton) rootView.findViewById(R.id.image_bt_hostname_apply);
        imageButtonHostname.setOnClickListener(this);
        toolbar = (Toolbar) rootView.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.toolbar_menu);
        mActivity.setSupportActionBar(toolbar);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.toolbar_menu, menu);
        return;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_scan:
                pDialog.setTitleText("scan...");
                pDialog.show();
                deviceObjectList.clear();
                new Thread(new SendUDPTask(UPD_COMMAND.getBytes(Charset.forName("UTF-8")))).start();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (deviceObjectList.size() > 0) {
                            dialogDeviceList = new DialogDeviceList(getContext(), deviceObjectList);
                            dialogDeviceList.show();
                        } else {
                            Toast.makeText(getContext(), "Can't find any devices !", Toast.LENGTH_LONG).show();
                        }
                        pDialog.dismiss();
                    }
                }, 3000);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.image_bt_hostname_apply:
                Log.d(TAG, "image_bt_hostname_apply " + pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, ""));
                if (pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, "").equals("")) {
                    if (textInputEditTextHostname.getText().length() > 0) {
                        final String jsonBody = "{\"hostname\":\"" + textInputEditTextHostname.getText() + "\"}";
                        new Thread() {
                            @Override
                            public void run() {
                                new OKHttpHelper(pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, ""),
                                        "api/led/hostname", FragmentHelper.FRAGMENT_EVENT_POST_HOSTNAME).methodPost(jsonBody);
                            }
                        }.start();
                    } else {
                        Toast.makeText(getContext(), "Hostname can't be empty !", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Please, scan device first !", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void onPause() {
        Log.d(TAG, "FragmentSetting-onPause");
        if (clientReceiveSocket != null)
            clientReceiveSocket.close();
        if (receiveUPDThread != null)
            receiveUPDThread.interrupt();
        if (dialogDeviceList != null)
            dialogDeviceList = null;
        super.onPause();
    }

    /**
     * Send UDP cmd to device
     */
    private class SendUDPTask implements Runnable {

        byte[] data;

        public SendUDPTask(byte[] readyToSend) {
            this.data = readyToSend;
        }

        //private int ticket=10;
        public void run() {
            DatagramSocket clientSocket;

            try {
                clientSocket = new DatagramSocket();

                clientSocket.setBroadcast(true);

                DatagramPacket sendPacket;
                try {
                    sendPacket = new DatagramPacket(data, data.length, InetAddress.getByName("255.255.255.255"), 5002);
                    clientSocket.send(sendPacket);
                } catch (UnknownHostException e) {
                    // TODO Auto-generated catch block
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "\nFailed 1 = " + e.getMessage());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    if (BuildConfig.DEBUG)
                        Log.d(TAG, "\nFailed 2 = " + e.getMessage());
                }

                clientSocket.close();
            } catch (SocketException se) {
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "Failed to create socket due to SocketException: " + se.getMessage());
            }

        }
    }

    /**
     * Receive UDP feedback
     */
    private class ReceiveUDPTask implements Runnable {

        @Override
        public void run() {

            byte[] receiveData = new byte[48];
            DatagramPacket receivePacket = null;
            receivePacket = new DatagramPacket(receiveData, receiveData.length);

            try {
                clientReceiveSocket = new DatagramSocket(65088);
                Log.d(TAG, "clientReceiveSocket create successfully");
                while (true) {
                    try {
                        clientReceiveSocket.receive(receivePacket);
                        try {
                            String IP = (new String(receiveData, "UTF-8")).replaceAll("\u0000.*", "");
                            deviceObjectList.add(IP);
                        } catch (Exception e) {
                            Log.d(TAG, "udp listen error1 : " + e.getMessage());
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        clientReceiveSocket.close();
                        Log.d(TAG, "udp listen error2 : " + e.getMessage());
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                        break;
                    }
                }
            } catch (SocketException e) {
                e.printStackTrace();
                if (BuildConfig.DEBUG)
                    Log.d(TAG, "clientReceiveSocket create fail");
                if (clientReceiveSocket != null)
                    clientReceiveSocket.close();
                if (receiveUPDThread != null)
                    receiveUPDThread.interrupt();
                receiveUPDThread = new Thread(new ReceiveUDPTask());
                receiveUPDThread.start();
            }
        }
    }

    //handle command result
    public static Handler commandHandler = new Handler() {

        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(@NonNull Message msg) {

            switch (msg.what) {

                case FragmentHelper.FRAGMENT_EVENT_UDP_IP:
                    Log.d(TAG, "FRAGMENT_EVENT_UDP_IP");
                    //int numOfBytesReceived = msg.arg1;
                    final String deviceIP = (String) msg.obj;
                    Log.d(TAG, "Selected IP = " + deviceIP);
                    editor.putString(CmdHelper.SHAREDPREFERENCE_KEY_IP, deviceIP).apply();
                    editor.apply();
                    textInputEditTextIP.setText(deviceIP);
                    new Thread() {
                        @Override
                        public void run() {
                            new OKHttpHelper(deviceIP, "api/led/all", FragmentHelper.FRAGMENT_EVENT_GET_ALL_SETTINGS).methodGet();
                        }
                    }.start();
                    dialogDeviceList.cancel();
                    break;


                case FragmentHelper.FRAGMENT_EVENT_GET_ALL_SETTINGS:
                    Log.d(TAG, "FRAGMENT_EVENT_GET_SETTINGS " + (String) msg.obj);
                    try {
                        JSONObject jsonObject = new JSONObject((String) msg.obj);
                        if (jsonObject.has(CmdHelper.JSON_KEY_RESULT)) {

                        } else {
                            textInputEditTextHostname.setText(jsonObject.getString("hostname"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;

                case FragmentHelper.FRAGMENT_EVENT_POST_HOSTNAME:
                    Log.d(TAG, "FRAGMENT_EVENT_POST_HOSTNAME " + (String) msg.obj);
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

