package com.android.eldbox.factory;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;

import com.android.eldbox.MApplication;
import com.android.eldbox.R;
import com.android.eldbox_api.Data;
import com.android.eldbox_api.EldboxCallBack;
import com.android.eldbox_api.Response;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.ToastUtils;

import java.io.File;
import java.util.List;
import java.util.Objects;

import static com.android.eldbox.tools.Utils.createLogFile;
import static com.android.eldbox.tools.Utils.writeToLogFile;

/**
  *
  * @ProjectName:
  * @Package:        com.android.eldbox
  * @ClassName:      WriteSNActivity
  * @Description:     主入口 main entrance
  * @Author:         HJH
  * @UpdateUser:
  * @UpdateDate:
  * @UpdateRemark:
  * @Version:        1.0 完成串口写入SN码，蓝牙版本暂未完成 Complete serial write SN code, Bluetooth version is not completed
 *                   1.1 蓝牙版本写SN码，在Settings界面 Write Sn code for Bluetooth version in settings interface
 */
//should set MApplication.isfactory = true;
public class WriteSNActivity extends AppCompatActivity {
    //Intent
    //GATT 是generic Attributes协议，是Ble的通信协议
    // GATT is the generic Attributes protocol, which is Ble's communication protocol.
    public static final String ACTION_CONNECT_TIMEOUT = ".ACTION_CONNECT_TIMEOUT";
    public static final String ACTION_CONNECT_ERROR = ".ACTION_CONNECT_ERROR";
    public static final String ACTION_GATT_CONNECTED = ".ACTION_GATT_CONNECTED";
    public static final String ACTION_GATT_DISCONNECTED = ".ACTION_GATT_DISCONNECTED";
    public static final String ACTION_DATA_AVAILABLE = ".ACTION_DATA_AVAILABLE";//Received data
    public static final String EXTRA_DATA = ".EXTRA_DATA";

    //Debug
    private static final String TAG = "WriteSNActivity";

    //other
    public static boolean       isConnect   = false;
    long    lastClick            = 0;
    private static final String nullString  = "";

    //control
    EditText mETDeviceSN;
    EditText mETConfirm;

    RelativeLayout mRLWriteSN;

    Button mBtnChange;
    Button mBtnClear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_sn);

        if(MApplication.isBleELD) {
            openBluetooth();//for ble_ui
            ((MApplication) getApplication()).getService().setCallBack(mEldboxCallBack);
        }
        else
            openSerialPort();//for serial_ui

        requestPermission();
        init();
    }

    private void init() {
        mETDeviceSN = (EditText) findViewById(R.id.et_device_sn_from_barcode);
        mETConfirm = (EditText) findViewById(R.id.et_confirm_barcode);

        mRLWriteSN = (RelativeLayout) findViewById(R.id.rl_writesn);

        //focus
        mETDeviceSN.requestFocus();

        mBtnChange = (Button) findViewById(R.id.btn_change);
        mBtnClear = (Button) findViewById(R.id.btn_clear);

        mBtnClear.setOnClickListener(new mBtnOnClick());
        mBtnChange.setOnClickListener(new mBtnOnClick());


        refreshFile(createLogFile());

        if(!MApplication.isBleELD) {
            //remove bluetooth icon
            //gotoConn.setVisibility(View.GONE);
            //mIvConnectStatus.setVisibility(View.GONE);
        }else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_GATT_CONNECTED);
            intentFilter.addAction(ACTION_GATT_DISCONNECTED);
            intentFilter.addAction(ACTION_DATA_AVAILABLE);
            registerReceiver(receiver, intentFilter);
        }
    }

    /**
     * @param * @Param null:
     * @method
     * @description a
     * @date: 9/11/2019 9:18 AM
     * @author: HJH
     * @return * @return: null
     */
    class mBtnOnClick implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.btn_change:
                    mBtnChange.setEnabled(false);

                    String mSN = mETDeviceSN.getText().toString();

                    boolean result = false;
                    if (MApplication.isBleELD)
                        result = ((MApplication) getApplication()).getService().writeDeviceSN(mSN.getBytes());
                    else {
                        try {
                            result = ((MApplication) getApplication()).serialAgent.writeDeviceSN(mSN.getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if(!result)
                        ToastUtils.show(R.string.toast_sn_error);
                    break;
                case R.id.btn_clear:
                    clearInput();
                    mBtnChange.setEnabled(true);
                    break;
                default:
                    break;
            }
        }
    }

    private void clearInput() {
        mRLWriteSN.setBackgroundColor(Color.WHITE);
        mETDeviceSN.setText(nullString);
        mETConfirm.setText(nullString);
        mETDeviceSN.requestFocus();
        mBtnChange.setText(R.string._change);
        setTitle(R.string.app_name_writesn);
        mBtnChange.setEnabled(true);
    }


    /**
     * @method requestPermission
     * @description requestPermission
     * unable to find device,if not location permission
     * @date: 9/21/2019 3:45 PM
     * @author: HJH
     * @param
     * @return * @return: void
     */
    public void requestPermission() {
        XXPermissions.with(this)
                //You can set up a rejected application to continue request permission until the user is authorized or permanently rejected
                .constantRequest()
                .permission(Manifest.permission.BLUETOOTH_ADMIN)
                .permission(Manifest.permission.ACCESS_NETWORK_STATE,Manifest.permission.INTERNET)
                .permission(Permission.Group.LOCATION,Permission.Group.STORAGE)
                .request(new OnPermission() {

                    @Override
                    public void hasPermission(List<String> granted, boolean isAll) {
                        if (!isAll) {
                            ToastUtils.show(R.string.toast_request_some_failed);
                        }
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        if(quick) {
                            ToastUtils.show(R.string.toast_request_denied);
                            //If it is permanently rejected, jump to the application permission in system Settings page
                            XXPermissions.gotoPermissionSettings(WriteSNActivity.this);
                        }else {
                            ToastUtils.show(R.string.toast_request_failed);
                        }
                    }
                });
    }

    /**
     * @description Broadcast Receive
     * @author: HJH
     */
    BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (Objects.requireNonNull(intent.getAction())) {
                case ACTION_DATA_AVAILABLE: {
                    writeToLogFile(getString(R.string._receive));
                    break;
                }
                case WriteSNActivity.ACTION_GATT_CONNECTED: {
                    writeToLogFile(getString(R.string._connect));
                    //mIvConnectStatus.setImageResource(R.drawable.ic_bluetooth_connected);
                    break;
                }
                case ACTION_GATT_DISCONNECTED: {
                    writeToLogFile(getString(R.string._disconnect));
                    //mIvConnectStatus.setImageResource(R.drawable.ic_bluetooth_disabled);
                }
                break;
            }
        }
    };

    /**
     * @description 回调函数 CallBack
     * @author: HJH
     */
    EldboxCallBack mEldboxCallBack = new EldboxCallBack() {
        /*********    Bluetooth CallBack   *********/
        @Override
        public void onConnected() {
            sendBroadcast(new Intent(ACTION_GATT_CONNECTED));
        }

        @Override
        public void onDisconnected() {
            sendBroadcast(new Intent(ACTION_GATT_DISCONNECTED));
        }

        /*********    ELD Data CallBack   *********/

        /**
         * @method onReceiveVin
         * @description API回调 接收Vin码 API callback for receive Vin code
         * //ELD通信协议所使用时间戳为Unix时间戳，解释如下
         * //unix时间戳是从1970年1月1日（UTC/GMT的午夜）开始所经过的秒数，不考虑闰秒
         * //vin数据不是经常需要变动的数据，因而按天储存到Static Message Book中
         *The timestamp used by the ELD communication protocol is a Unix timestamp, explained as follow:
          The Unix timestamp is the number of seconds elapsed since January 1,1970 (the midnight of UTC/GMT),
          regardless of leap seconds.Vin data is not often required to change data, so it is stored in the Static
          Message Book by day
         * @author: HJH
         * @param * @Param timeStamp: 时间戳  timestamp
         * @Param vin: vin code
         * @return * @return: void
         */
        @Override
        public void onReceiveVin(long timeStamp, final String vin) {

        }

        /**
         * @method onReceiveEngineHours
         * @description API回调 接收引擎启动时间  API callback for receive EngineHours
         * 2019-12-04 更新engine hours为double类型，需要将从API库获取的值除以20.
         * 2019-12-04 Updating engine hours to be of type double,it requires dividing the value obtained from the API library by 20.
         * @author: HJH
         * @param * @Param timeStamp: 时间戳 Timestamp
         * @Param time: EngineHours
         * @return * @return: void
         */
        @Override
        public void onReceiveEngineHours(long timeStamp, long time) {

        }

        /**
         * @method onReceiveTripDistance
         * @description API回调 接收短里程  API callback for eceive this tripDistance
         * @author: HJH
         * @param * @Param timeStamp: 时间戳 Timestamp
         * @Param tripDistance: 短里程 trip Distance
         * @return * @return: void
         */
        @Override
        public void onReceiveTripDistance(long timeStamp, final int distance) {

        }

        /**
         * @method onReceiveRpm
         * @description API回调 接收RPM发动机转速 API callback for receiving RPM engine speed
         * @author: HJH
         * @param * @Param timeStamp: 时间戳 timeStamp
         * @Param rpm: RPM发动机转速 RPM engine speed
         * @return * @return: void
         */
        @Override
        public void onReceiveRpm(long timeStamp, final int rpm) {

        }

        /**
         * @method onReceiveVss
         * @description API回调 接收VSS车速 API callback for eceive VSS speed
         * @author: HJH
         * @param * @Param timeStamp: 时间戳 Timestamp
         * @Param Vss:车速 VSS  vehicle speed
         * @return * @return: void
         */
        @Override
        public void onReceiveVss(final long timeStamp, final int Vss) {

        }

        /**
         * @method onReceiveVersion
         * @description API回调 接收单片机版本号  API callback for receive the MCU firmware version
         * @author: HJH
         * @param  * @param Version :单片机版本  the MCU firmware version
         * @return * @return: void
         */
        @Override
        public void onReceiveVersion(String Version) {
            ((MApplication) getApplication()).mAppData.setVersion(Version);
        }

        /**
         * @method onReceiveSleepDelay
         * @description API回调 接收睡眠延时。如果转速为零，达到延时时间，盒子将会睡眠。软件可以设置时间（分钟计）。
         * API callback for receive sleep delay time. If the vehicle speed is zero and the delay time is reached,
         * the box will sleep.  We can set the delay time (in minutes).
         * @author: HJH
         * @param sleepDelay 睡眠延时 Sleep delay time
         * @return * @return: void
         */
        @Override
        public void onReceiveSleepDelay(int sleepDelay) {

        }

        /**
         * @method onReceiveOdometer
         * @description API回调 接收总里程 API callback for receive odometer
         * @author: HJH
         * @param timeStamp 时间戳 Timestamp
         * @param odometer 总里程 odometer
         * @return * @return: void
         */
        @Override
        public void onReceiveOdometer(long timeStamp, final int odometer) {

        }

        @Override
        public void onReceiveDTC(long timeStamp, byte[] dtc) {

        }

        /**
         * @method onReceiveHighPrecisionOdometer
         * @description API回调 接收高精度总里程（仅J1939） API callback for receive highPrecisionOdometer
         * @author: HJH
         * @param * @Param timeStamp: 时间戳 Timestamp
         * @Param highPrecisionOdometer: 高精度总里程
         * @return * @return: void
         */
        @Override
        public void onReceiveHighPrecisionOdometer(long timeStamp, final long highPrecisionOdometer) {

        }

        /**
         * @method onReceiveOtherParameter
         * @description API回调 API callback for receive other para
         * ①当ParaType等于0xD009，接收燃料液面（仅J1939 J1587）
         * when ParaType is DataTypeNotSync.FuelLevel (0xD009) ,it receive Fuel Level
         * J1939取值范围：0~100，单位：%   (J1939 in the range: 0 ~ 100, unit: %)
         * J1587取值范围：0~127.5，单位：%   (J1939 in the range: 0 ~ 127.5, unit: %) 这边取值先取整数，即0~127
         * OBD无此参数 (OBD does not have this parameter)
         * @date: 4/9/2020 8:35 AM
         * @author: HJH
         * @param * @Param paraType:
         * @Param timeStamp:
         * @Param paraValue:
         * @return * @return: void
         */
        @Override
        public void onReceiveOtherParameter(int paraType, long timeStamp, long paraValue) {

        }

        /**
         * @method onReceiveResponse
         * @description API回调 接收盒子设备的应答 API callback for receive response from eldbox device
         * @author: HJH
         * @param * @Param responseType:应答类型 response type
         * @Param response:应答数据 response data
         * @return * @return: void
         */
        @Override
        public void onReceiveResponse(int responseType, byte[] response) {
            int reslut = response[0] & 0xff;
            switch (responseType) {
                case Response.TimeSyncResponse:
                    if(reslut == 0)
                    {
                        //设置同步时间失败 Failed to sync time
                    }else{
                        //设置同步时间成功 sync time successfully
                    }
                    break;
                case Response.DataSyncResponse:
                    if(reslut == 0)
                    {
                        //请求同步失败 Request synchronization failed
                    }else if(reslut == 0xAA){
                        //数据同步结束 Data synchronization ends
                    }else {
                        //请求同步成功 Request synchronization successfully
                    }
                    break;
                case Response.SleepDelayResponse:
                    if(reslut == 0)
                    {
                        //设置休眠时间失败 Failed to set sleep time
                    }else{
                        //设置休眠时间成功 Set sleep time successfully
                    }
                    break;
                case Response.VerifyResponse:
                    if(reslut == 0)
                    {
                        //接收到的数据校验失败 Received data validation failed
                    }else{
                        //接收到的数据校验正确 The received data check is correct
                    }
                    break;
                case Response.WorkModeResponse:
                    if(reslut == 0){
                        //当前工作模式为ELD  Current work mode is Eld mode
                    }else {
                        //当前工作模式为CMD  Current work mode is cmd mode
                    }
                    break;
                case Response.DeviceSNSetResultResponse:
                    if(reslut == 0){
                        //设置SN码失败 //fail to set SN
                        ToastUtils.show(R.string.toast_set_device_sn_failed);
                    }else{
                        if (MApplication.isBleELD)
                            ((MApplication) getApplication()).getService().requestDeviceSN();
                        else {
                            try {
                                ((MApplication) getApplication()).serialAgent.requestDeviceSN();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case Response.DeviceSNResponse:
                    String readSN = new String(response);
                    if(readSN.equals(mETDeviceSN.getText().toString())) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mRLWriteSN.setBackgroundColor(Color.GREEN);
                                setTitle(R.string.success_title);
                                mBtnChange.setText(R.string._pass);
                                //set SN success
                                ToastUtils.show(R.string.toast_set_device_sn_success);
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mRLWriteSN.setBackgroundColor(Color.RED);
                                setTitle(R.string.fail_title);
                                mBtnChange.setText(R.string._error);
                                //fail to set SN
                                ToastUtils.show(R.string.toast_set_device_sn_failed);
                            }
                        });
                    }
                    break;
                default:
                    break;
            }
        }

        /**
         * @method onResponseNeedToSend
         * @description 将response发往设备，response是机器给设备的应答
         * @author: HJH
         * @param * @Param response:
         * @return * @return: void
         */
        @Override
        public void onResponseNeedToSend(byte[] response) {
            if (MApplication.isBleELD)
                ((MApplication) getApplication()).getService().sendMessage(response);
            else {
                try {
                    ((MApplication) getApplication()).serialAgent.writeData(response);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * @method onReceiveHistoryData
         * @description 同步选定时间段的数据（8*24小时） Synchronize data for the selected time period (8*24 hours)
         * 2019-12-04 更新engine hours为double类型，需要将从API库获取的值除以20.
         * 2019-12-04 Updating engine hours to be of type double,it requires dividing the value obtained from the API library by 20.
         * @author: HJH
         * @param data ELD 数据  ELD data
         * @return * @return: void
         */
        @Override
        public void onReceiveHistoryData(Data data) {

        }

        @Override
        public void onReceiveCmdData(byte[] data) {

        }

    };

    @Override
    protected void onResume() {
        Log.i(TAG,"onResume");
        super.onResume();
    }

    /**
     * @method refreshFile
     * @description 刷新文件列表，使文件能够在文件管理器等被看到。
     * Refresh the file list so that the file can be seen in the file manager, etc.
     * @author: HJH
     * @param filePath 文件路径 file path
     * @return * @return: void
     */
    private void refreshFile(String filePath) {
        Uri localUri = Uri.fromFile(new File(filePath));
        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
        sendBroadcast(localIntent);
    }

    /**
     * @method openBluetooth
     * @description 打开蓝牙 Turn on Bluetooth
     * @author: HJH
     * @return * @return: void
     */
    private void openBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (bluetoothManager == null) return;
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    /***
     * @method openSerialPort
     * @description 打开串口 Open serial port
     * @date: 9/30/2019 11:32 AM
     * @author: HJH
     * @param
     * @return * @return: void
     */
    private void openSerialPort(){
        if (((MApplication) getApplication()).serialAgent == null) {
            ((MApplication) getApplication()).createSerialAgent().setmEldboxCallBack(mEldboxCallBack);
            try {
                ((MApplication) getApplication()).serialAgent.openSerialPort().startListening();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (!MApplication.isBleELD && (System.currentTimeMillis() - lastClick) > 2000) {
            ToastUtils.show(R.string.toast_exit_app);
            lastClick = System.currentTimeMillis();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if(MApplication.isBleELD) {
            unregisterReceiver(receiver);
			//((MApplication) getApplication()).getService().disConnect();
        	//((MApplication) getApplication()).unBindAndStopService();
		}
        else {
            ((MApplication) getApplication()).disConnectSerial();
            System.exit(0);
        }

        ToastUtils.cancel();

        super.onDestroy();
    }
}
