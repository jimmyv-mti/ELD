package com.android.eldbox.View;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.eldbox.R;

import java.util.ArrayList;

public class BleListAdapter extends BaseAdapter
{
    Context context;
    ArrayList<BluetoothDevice> devices;

    public BleListAdapter(Context context, ArrayList<BluetoothDevice> devices)
    {
        this.context = context;
        this.devices = devices;
    }

    @Override
    public int getCount()
    {
        return devices.size();
    }

    @Override
    public Object getItem(int position)
    {
        return devices.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.item_ble_listitem, null, false);
        BluetoothDevice device = (BluetoothDevice) getItem(position);
        //如果获取不到设备名称，就显示mac地址
        if (null == device.getName())
        {
            ((TextView) view.findViewById(R.id.tv_device_name)).setText(device.getAddress());
        } else
        {
            ((TextView) view.findViewById(R.id.tv_device_name)).setText(device.getName());
        }

        return view;
    }
}
