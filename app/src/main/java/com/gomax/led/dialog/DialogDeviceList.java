package com.gomax.led.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gomax.led.R;
import com.gomax.led.adapter.DeviceListAdapter;
import com.gomax.led.data.DeviceObject;
import com.gomax.led.object.UDPDeviceObject;

import java.util.ArrayList;
import java.util.HashMap;

public class DialogDeviceList {

    private Dialog dialog;
    private Context mContext;
    private Window window;
    private HashMap<String, UDPDeviceObject> deviceObjectList;
    private DeviceListAdapter deviceListAdapter;
    private RecyclerView sourceListRecyclerView;
    private ArrayList<String> udpDeviceMACIndexList;

    public DialogDeviceList(Context context, HashMap<String, UDPDeviceObject> deviceObjectList, ArrayList<String> udpDeviceMACIndexList) {
        this.mContext = context;
        this.deviceObjectList = deviceObjectList;
        this.udpDeviceMACIndexList = udpDeviceMACIndexList;
        setupUI();
    }

    //setup UI
    private void setupUI() {
        this.deviceListAdapter = new DeviceListAdapter(this.mContext, this.deviceObjectList, this.udpDeviceMACIndexList);
        this.dialog = new Dialog(this.mContext);
        this.window = dialog.getWindow();
        this.dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.dialog.setContentView(R.layout.dialog_change_source);
        this.dialog.setCanceledOnTouchOutside(true);
        //this.dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        this.sourceListRecyclerView = (RecyclerView)dialog.findViewById(R.id.recyclerView_source_list);
        this.sourceListRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        this.sourceListRecyclerView.setAdapter(deviceListAdapter);
        this.deviceListAdapter.notifyDataSetChanged();
        window = dialog.getWindow();
        window.setLayout((int) ((int) mContext.getResources().getDimension(R.dimen.dp_250)), (int) mContext.getResources().getDimension(R.dimen.dp_200));
        WindowManager.LayoutParams wmlp = dialog.getWindow().getAttributes();
        wmlp.gravity = Gravity.CENTER;
    }

    /**
     * show dialog
     */
    public void show() {
        this.dialog.show();
    }

    /**
     * close dialog
     */
    public void cancel() {
        this.dialog.cancel();
    }
}