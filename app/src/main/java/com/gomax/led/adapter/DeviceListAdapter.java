package com.gomax.led.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.gomax.led.R;
import com.gomax.led.fragment.FragmentHelper;
import com.gomax.led.fragment.FragmentSettings;
import com.gomax.led.object.UDPDeviceObject;

import java.util.ArrayList;
import java.util.HashMap;


public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceListViewHolder> {

    private ArrayList<String> udpDeviceMACIndexList;
    public HashMap<String, UDPDeviceObject> deviceObjectList;
    private final String TAG = "DeviceListAdapter";
    private Context mContext;


    public static class DeviceListViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewDeviceIP, textViewDeviceName;

        public DeviceListViewHolder(View v) {
            super(v);
            textViewDeviceIP = (TextView) v.findViewById(R.id.textView_device_ip);
            textViewDeviceName = (TextView) v.findViewById(R.id.textView_device_name);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public DeviceListAdapter(Context context, HashMap<String, UDPDeviceObject> deviceObjectList, ArrayList<String> udpDeviceMACIndexList) {
        this.deviceObjectList = deviceObjectList;
        this.udpDeviceMACIndexList = udpDeviceMACIndexList;
        this.mContext = context;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public DeviceListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_view_source_item, parent, false);
        // create a new view
        DeviceListViewHolder vh = new DeviceListViewHolder(itemView);
        return vh;
    }


    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(DeviceListViewHolder holder, final int position) {
        String macIndex = this.udpDeviceMACIndexList.get(position);
        holder.textViewDeviceIP.setText(deviceObjectList.get(macIndex).getIP());
        holder.textViewDeviceName.setText("LED");
        final String ip = holder.textViewDeviceIP.getText().toString();
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Selected IP = " + ip);
                //connect to device
                FragmentSettings.commandHandler.obtainMessage(FragmentHelper.FRAGMENT_EVENT_UDP_IP,
                        0, -1, ip).sendToTarget();
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return deviceObjectList.size();
    }

}