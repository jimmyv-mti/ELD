package com.android.eldbox.ble_ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import com.hjq.toast.ToastUtils;

import com.android.eldbox.MApplication;
import com.android.eldbox.R;
import com.android.eldbox.View.BleListAdapter;
import com.android.eldbox.View.ConnectingDialog;

import java.util.ArrayList;
import java.util.Objects;


public class BleConnectActivity extends AppCompatActivity {
    public BleListAdapter bleListAdapter;
    public ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private BluetoothManager bluetoothManager;
    public ProgressBar scanDeviceProgress;

    public ListView         mLvDeviceList;
    public ConnectingDialog connectingDialog;
    private ImageView       mIvStartDiscovery, mIvCancelDiscovery;

    //Debug log
    public static final String TAG   = "BleConnectActivity";
    private boolean   Debug = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_connect);

        initControl();

        //register Broadcast receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BleUIMainActivity.ACTION_CONNECT_ERROR);
        intentFilter.addAction(BleUIMainActivity.ACTION_CONNECT_TIMEOUT);
        intentFilter.addAction(BleUIMainActivity.ACTION_GATT_CONNECTED);
        registerReceiver(receiver, intentFilter);

        connectingDialog = new ConnectingDialog(BleConnectActivity.this);

        //bluetooth
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager.getAdapter().isDiscovering())
            bluetoothManager.getAdapter().cancelDiscovery();
        bluetoothManager.getAdapter().startDiscovery();
    }

    /**
     * @method initControl
     * @description 初始化控件 Initialize control
     * @author: HJH
     * @param
     * @return * @return: void
     */
    private void initControl() {
        scanDeviceProgress = findViewById(R.id.pb_scanning_device);
        mIvStartDiscovery = findViewById(R.id.iv_start_discovery);
        mIvCancelDiscovery = findViewById(R.id.iv_cancel_discovery);

        mIvStartDiscovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bluetoothManager.getAdapter().isEnabled()) {
                    bluetoothManager.getAdapter().enable();
                }
                bluetoothManager.getAdapter().startDiscovery();
            }
        });
        mIvCancelDiscovery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetoothManager.getAdapter().isDiscovering()) {
                    bluetoothManager.getAdapter().cancelDiscovery();
                }
                finish();
            }
        });

        mLvDeviceList = findViewById(R.id.lv_blueooth_device_list);
        bleListAdapter = new BleListAdapter(this, devices);
        mLvDeviceList.setAdapter(bleListAdapter);

        mLvDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ((MApplication) getApplication()).getService().connectDevice(devices.get(position));
                if (bluetoothManager.getAdapter().isDiscovering()) {
                    bluetoothManager.getAdapter().cancelDiscovery();
                }
                if (!connectingDialog.isShowing()) {
                    connectingDialog.show();
                    connectingDialog.setCanceledOnTouchOutside(false);
                }
            }
        });
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED: {
                    devices.clear();
                    bleListAdapter.notifyDataSetChanged();
                    scanDeviceProgress.setVisibility(View.VISIBLE);
                    if (Debug)
                        Log.w(TAG,"start search bluetooth device");
                }
                break;
                case BluetoothDevice.ACTION_FOUND: {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (null == device) return;
                    if (devices.contains(device)) return;
                    if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) return;
                    devices.add(device);
                    bleListAdapter.notifyDataSetChanged();
                    if (Debug)
                        Log.w(TAG,"found device ,name：" +
                                (device.getName() == null ? device.getName() : "null")
                                + "address: " + device.getAddress());
                }
                break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED: {
                    if (Debug)
                        Log.w(TAG,"discover finished");
                    scanDeviceProgress.setVisibility(View.INVISIBLE);
                }
                break;
                case BleUIMainActivity.ACTION_GATT_CONNECTED: {
                    if (connectingDialog.isShowing()) {
                        connectingDialog.dismiss();
                    }
                    finish();
                    break;
                }
                case BleUIMainActivity.ACTION_CONNECT_TIMEOUT: {
                    if (connectingDialog.isShowing()) {
                        connectingDialog.dismiss();
                    }

                    ToastUtils.show("connect timeout");
                }
                break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();

        ToastUtils.cancel();
    }

}
