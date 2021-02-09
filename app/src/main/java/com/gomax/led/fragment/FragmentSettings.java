package com.gomax.led.fragment;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.gomax.led.BuildConfig;
import com.gomax.led.R;
import com.gomax.led.dialog.DialogDeviceList;
import com.gomax.led.http.CmdHelper;
import com.gomax.led.http.OKHttpHelper;
import com.gomax.led.main.MainActivity;
import com.gomax.led.object.UDPDeviceObject;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import cn.pedant.SweetAlert.SweetAlertDialog;

import static android.content.Context.MODE_PRIVATE;

public class FragmentSettings extends Fragment implements View.OnClickListener {

    private static final String TAG = "FragmentSettings";
    private Thread receiveUPDThread;
    private DatagramSocket clientReceiveSocket = null;
    private HashMap<String, UDPDeviceObject> udpHashMap;
    private String AESKey = "qzy159pkn333rty2";
    private byte[] cmd = {(byte) 0x00, (byte) 0x0b, (byte) 0x80, (byte) 0x00, (byte) 0x45,
            (byte) 0x54, (byte) 0x48, (byte) 0x5f, (byte) 0x52, (byte) 0x45, (byte) 0x51
            , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private Toolbar toolbar;
    private ArrayList<String> udpDeviceMACIndexList;
    private static SweetAlertDialog pDialog;
    private static DialogDeviceList dialogDeviceList;
    private static TextInputEditText textInputEditTextIP, textInputEditTextHostname;
    private ImageButton imageButtonHostname;
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;
    private TextView txVer;

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
        udpDeviceMACIndexList = new ArrayList<>();
        udpHashMap = new HashMap<>();
        pDialog = new SweetAlertDialog(getContext(), SweetAlertDialog.PROGRESS_TYPE);
        pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
        pDialog.setCancelable(false);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {

        if (pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, "").length() > 0) {
            new Thread() {
                @Override
                public void run() {
                    new OKHttpHelper(pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, ""), "api/led/all", FragmentHelper.FRAGMENT_EVENT_GET_ALL_SETTINGS).methodGet();
                }
            }.start();
        } else {
            Toast.makeText(getContext(), "Please scan devices first !", Toast.LENGTH_LONG).show();
        }
        super.onStart();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_settings_layout, container, false);
        AppCompatActivity mActivity = (AppCompatActivity) getActivity();
        txVer = (TextView) rootView.findViewById(R.id.tx_ver);
        txVer.setText("APP Ver. " + getVersionName());
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
                udpHashMap.clear();
                udpDeviceMACIndexList.clear();
                try {
                    new Thread(new SendUDPTask(encrypt(AESKey.getBytes(), cmd))).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (udpHashMap.size() > 0) {
                            dialogDeviceList = new DialogDeviceList(getContext(), udpHashMap, udpDeviceMACIndexList);
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
                if (pref.getString(CmdHelper.SHAREDPREFERENCE_KEY_IP, "").length() > 0) {
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
                            byte[] data = decrypt(AESKey.getBytes(), receiveData);
                            if (data.length >= 38) {
                                String deviceName = "";
                                for (int index = 5; index <= 20; index++) {
                                    deviceName += hexStrArrayToASCII(byteToHexString(data[index]));
                                }

                                if (deviceName.replaceAll("\u0000.*", "").toLowerCase().contains("led")) {
                                    String mac = byteToHexString(data[21]) + "-" + byteToHexString(data[22])
                                            + "-" + byteToHexString(data[23]) + "-" + byteToHexString(data[24]) + "-" + byteToHexString(data[25]) + "-" + byteToHexString(data[26]);
                                    String ip = byte2Int(data[27]) + "." + byte2Int(data[28]) + "." + byte2Int(data[29]) + "." + byte2Int(data[30]);
                                    String mask = byte2Int(data[31]) + "." + byte2Int(data[32]) + "." + byte2Int(data[33]) + "." + byte2Int(data[34]);
                                    String gateway = byte2Int(data[35]) + "." + byte2Int(data[36]) + "." + byte2Int(data[37]) + "." + byte2Int(data[38]);
                                    Log.d(TAG, "recevive device..." + deviceName.replaceAll("\u0000.*", ""));
                                    if (udpHashMap.containsKey(mac)) {
                                    } else {
                                        udpDeviceMACIndexList.add(mac);
                                        udpHashMap.put(mac, new UDPDeviceObject(deviceName, mac, ip, gateway, mask));
                                    }
                                } else {
                                    Log.d(TAG, "not LED");
                                }
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "udp listen error1 : " + e.getMessage());
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        if(clientReceiveSocket != null){
                            clientReceiveSocket.close();
                        }
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
                            Toast.makeText(MainActivity.mActivity.get(), "Get system info failed ! \n\n Please to scan new devices.", Toast.LENGTH_LONG).show();
                        } else {
                            textInputEditTextHostname.setText(jsonObject.getString(CmdHelper.JSON_KEY_HOSTNAME));
                            textInputEditTextIP.setText(jsonObject.getString(CmdHelper.JSON_KEY_IP));
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

    /**
     * get app version name
     *
     * @return : version name
     */
    public static String getVersionName() {
        PackageManager packageManager = MainActivity.mActivity.get().getPackageManager();
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(MainActivity.mActivity.get().getPackageName(), 0);
            Log.d(TAG, "" + packInfo.versionCode);
            Log.d(TAG, "" + packInfo.versionName);
            return packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * AES encode
     *
     * @param raw   : AES Key
     * @param clear : want to encode data
     * @return : AES encode data
     * @throws Exception
     */
    private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }

    /**
     * AES decode
     *
     * @param raw       : AES Key
     * @param encrypted : want to decode data
     * @return : AES decode data
     * @throws Exception
     */
    private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    /**
     * Byte data to hex string
     *
     * @param byteData : Byte data
     * @return : hex string of byte data
     */
    private static String byteToHexString(Byte byteData) {
        return String.format("%02x", byteData).toUpperCase();
    }

    /**
     * byte (hex) to int
     *
     * @param data : Byte data (hex)
     * @return : int of byte data (hex)
     */
    private static int byte2Int(byte data) {
        return data & 0xFF;
    }

    /**
     * convert hex string to ASCII string
     *
     * @param hex_data : server feedback hex string
     * @return : convert to hex string
     */
    public static String hexStrArrayToASCII(String hex_data) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex_data.length(); i += 2) {
            String str = hex_data.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

}

