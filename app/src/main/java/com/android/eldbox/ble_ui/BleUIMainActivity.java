package com.android.eldbox.ble_ui;

import static com.android.eldbox.tools.Utils.createLogFile;
import static com.android.eldbox.tools.Utils.writeToLogFile;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;

import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.eldbox.CustomItem.DoubleCircle;
import com.android.eldbox.DataBase.DBUtils;
import com.android.eldbox.DataBase.ELD_DATA;
import com.android.eldbox.DataBase.StaticMessage;
import com.android.eldbox.MApplication;
import com.android.eldbox.R;
import com.android.eldbox.StringUtil;
import com.android.eldbox.factory.WriteSNActivity;
import com.android.eldbox.tools.NetUtils;
import com.android.eldbox_api.Data;
import com.android.eldbox_api.DataTypeNotSync;
import com.android.eldbox_api.EldboxCallBack;
import com.android.eldbox_api.Response;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.hjq.toast.ToastUtils;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
/**
  *
  * @ProjectName:
  * @Package:        com.android.eldbox
  * @ClassName:      BleUIMainActivity
  * @Description:     主入口 main entrance
  * @Author:         HGY
  * @UpdateUser:     HJH
  * @UpdateDate:
  * @UpdateRemark:
  * @Version:        1.0
 */
public class BleUIMainActivity extends AppCompatActivity {
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
    private static final String TAG = "BleUIMainActivity";

    //control
    private ImageView    mIvConnectStatus;//Bluetooth Connect status
    private ImageView    mIvDetail;
    private TextView     mTvEngineHours;//Engine Hours
    private TextView     mTvOdometer;//Odometer
    private TextView     mTvTripDistance;
    private TextView     mTvIgnition;
    private TextView     mTvVin;
    private TextView     mTvTimeStamp;
    //两个圈，因为显示rpm和vss，所以直接用这个命名
    //Two circles, because rpm and vss are displayed, so use this name directly
    private DoubleCircle mDoubleCirCleForRpmVss;

    private static int          preOdometer = 0;
    private String              preVin      = "";
    static boolean              inAsync     = false;
    public static boolean       isConnect   = false;
    long    lastClick            = 0;
    boolean hasSyncTime          = false;//Whether the time has been synchronized
    boolean WhetherSyncTime      = false;//whether the time should be synchronize
    boolean needOpenWifiSettings = false;//Whether to return from Settings

    Thread queryOtherInfoThread = null;

    //List AlertDialog
    AlertDialog alertDialog;
    View                 view;
    List<String>         data = new ArrayList<>();
    ListView             listView;
    ArrayAdapter<String> adapter;
    StringBuffer sb = new StringBuffer();
    boolean isShowDetail = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter=new ArrayAdapter<String>(BleUIMainActivity.this,android.R.layout.simple_list_item_1,data);

        if(MApplication.isBleELD)
            openBluetooth();//for ble_ui
        else
            openSerialPort();//for serial_ui

        requestPermission();
        init();
    }

    private void init() {
        mTvTripDistance = findViewById(R.id.tv_trip_distance);
        mTvIgnition = findViewById(R.id.tv_ignition);
        mDoubleCirCleForRpmVss = findViewById(R.id.double_circle_for_rpm_vss);
        mTvVin = findViewById(R.id.tv_vin);
        mTvOdometer = findViewById(R.id.tv_odometer);
        mTvTimeStamp = findViewById(R.id.tv_show_timestamp);
        ImageView gotoSet = findViewById(R.id.iv_goto_settings);
        ImageView gotoConn = findViewById(R.id.iv_goto_bluetooth_connect);
        mIvConnectStatus = findViewById(R.id.iv_bluetooth_connect_status);
        mTvEngineHours = findViewById(R.id.tv_engine_hours);
        mIvDetail = findViewById(R.id.iv_goto_detail);

        refreshFile(createLogFile());

        if (!MApplication.isBleELD) {
            //remove bluetooth icon
            gotoConn.setVisibility(View.GONE);
            mIvConnectStatus.setVisibility(View.GONE);
        } else {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_GATT_CONNECTED);
            intentFilter.addAction(ACTION_GATT_DISCONNECTED);
            intentFilter.addAction(ACTION_DATA_AVAILABLE);
            registerReceiver(receiver, intentFilter);
        }

        gotoSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inAsync) {
                    ToastUtils.show(R.string.toast_wait_when_read_data);
                    return;
                }
                startActivity(new Intent(BleUIMainActivity.this, BleSettingsActivity.class));
            }
        });

        gotoSet.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startActivity(new Intent(BleUIMainActivity.this, WriteSNActivity.class));
                return false;
            }
        });

        gotoConn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (inAsync) {
                    ToastUtils.show(R.string.toast_wait_when_read_data);
                    return;
                }
                startActivity(new Intent(BleUIMainActivity.this, BleConnectActivity.class));
            }
        });
		
		mIvDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShowDetail = true;
                view = getLayoutInflater().inflate(R.layout.controller_volume, null);
                alertDialog = new AlertDialog.Builder(BleUIMainActivity.this).setTitle(R.string.listview_dialog_title)
                        .setIcon(R.mipmap.ic_launcher)
                        .setView(view)
                        .setPositiveButton(R.string._return, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface paramAnonymousDialogInterface,
                                                int paramAnonymousInt) {
                                isShowDetail = false;
                                data.clear();
                            }
                        }).create();
                listView=view.findViewById(R.id.listView);
                listView.setAdapter(adapter);

                alertDialog.show();
            }
        });

        gotoConn.postDelayed(new Runnable() {
            @Override
            public void run() {
                ((MApplication) getApplication()).getService().setCallBack(mEldboxCallBack);
            }
        }, 1000);

        if (((MApplication) getApplication()).getService().getState() == BluetoothProfile.STATE_CONNECTED) {
            queryOtherInfoThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (true) {
                        if (queryOtherInfoThread == null || queryOtherInfoThread.isInterrupted())
                            break;
                        try {
                            /*It cost 1 second to query FuelLevel when use J1939.
                            And 10 second will cost to query FuelLevel when use J1587.
                            At the same time, other ECU parameters cannot be queried
                            */
                            ((MApplication) getApplication()).bleService.requestFuelLevel();
                            Thread.sleep(30000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.e(TAG, "queryFuelLevelThread exit");
                            break;
                        }
                    }
                }
            });
            queryOtherInfoThread.start();
        }
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
                        if (isAll) {
                            /* use local time from tablet to sync time*/
                            WhetherSyncTime = true;
                            SyncTimeFromTablet();

                            /* use ntp server to sync time
                            needOpenWifiSettings = true;//when app first run
                            WhetherSyncTime = true;
                            SyncTimeWhenConnectNetwork();
                            */
                        }else {
                            ToastUtils.show(R.string.toast_request_some_failed);
                        }
                    }

                    @Override
                    public void noPermission(List<String> denied, boolean quick) {
                        if(quick) {
                            ToastUtils.show(R.string.toast_request_denied);
                            //If it is permanently rejected, jump to the application permission in system Settings page
                            XXPermissions.gotoPermissionSettings(BleUIMainActivity.this);
                        }else {
                            ToastUtils.show(R.string.toast_request_failed);
                        }
                    }
                });
    }

    /**
     * @method SyncTimeFromTablet
     * @description 把平板时间同步到设备上
     * @date: 9/29/2019 7:47 PM
     * @author: HJH
     * @param
     * @return * @return: void
     */
    private void SyncTimeFromTablet(){
        //for Bluetooth ELD. Do not sync time when the device is not connected
        if(MApplication.isBleELD && ((MApplication) getApplication()).getService().getState() == BluetoothProfile.STATE_DISCONNECTED)
            return;

        if(!WhetherSyncTime || hasSyncTime)
            return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                ((MApplication) getApplication()).getService().requestTimeSync(System.currentTimeMillis());
                try {
                    Thread.sleep(100);
                }catch (InterruptedException e){

                }
                ((MApplication) getApplication()).getService().requestVersion();
                hasSyncTime = true;
                ToastUtils.show(R.string.toast_time_sync_success);
                Looper.loop();
            }
        }).start();

        WhetherSyncTime = true;

    }

    /***
     * @method SyncTimeWhenConnectNetwork
     * @description 同步NTP时间到设备上
     * @date: 9/21/2019 7:43 PM
     * @author: HJH
     * @param
     * @return * @return: void
     */
    private void SyncTimeWhenConnectNetwork() {
        if (!NetUtils.isConnected(BleUIMainActivity.this)) {//no network
            if(needOpenWifiSettings && !hasSyncTime) {
                needOpenWifiSettings = false;//hjh 20190921 for user click cancel
                openSetting(BleUIMainActivity.this);
            }
        } else {//has network
            needOpenWifiSettings = false;
            if(WhetherSyncTime && !hasSyncTime){
                WhetherSyncTime = false;//now has a thread,should not create new thread
                NTPThread ntpThread = new NTPThread();
                ntpThread.start();
            }
        }
    }

    /**
     * Open the network settings interface
     */
    public void openSetting(final Activity activity) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(activity.getString(R.string.open_wifi_setting_title));
        builder.setMessage(activity.getString(R.string.open_wifi_setting_content));
        builder.setPositiveButton(activity.getString(R.string._ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (android.os.Build.VERSION.SDK_INT > 10) {
                            // 3.0以上打开设置界面，也可以直接用ACTION_WIRELESS_SETTINGS打开到wifi界面
                            activity.startActivity(new Intent(
                                    android.provider.Settings.ACTION_WIFI_SETTINGS));
                        } else {
                            activity.startActivity(new Intent(
                                    android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                        }
                        needOpenWifiSettings = true;//hjh 20190921 for user click cancel
                        dialog.dismiss();
                        dialog = null;
                    }
                });

        builder.setNegativeButton(activity.getString(R.string._cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        dialog = null;

                    }
                });
        builder.show();
    }

    /**
     * @method
     * @description NTP 同步线程
     * @author: HJH
     * @param * @Param null:
     * @return * @return: null
     */
    class NTPThread extends Thread{

        NTPThread() {
        }

        @Override
        public void run() {
            super.run();
            long waitToSyncTime = getNTPTime();
            ((MApplication) getApplication()).bleService.requestTimeSync(waitToSyncTime);
            Log.w(TAG, "getNTPTime " + waitToSyncTime);
            switch ((int) waitToSyncTime) {
                case 0:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.show(R.string.toast_time_sync_fail);
                        }
                    });
                    break;
                case -1:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.show(R.string.toast_time_sync_fail);
                        }
                    });
                    break;
                case -2:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.show(R.string.toast_time_sync_retry);
                        }
                    });
                    break;
                case -3:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.show(R.string.toast_time_sync_fail);
                        }
                    });
                    break;
                default:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.show(R.string.toast_time_sync_success);
                            Log.i(TAG, getString(R.string.toast_time_sync_success));
                            hasSyncTime = true;//sync time success
                        }
                    });
                    break;
            }
            WhetherSyncTime = true;//thread has died,can create new thread for sync time
        }
        /**
         * @method getNTPTime
         * @description get NTP time
         * @date: 8/31/2019 5:18 PM
         * @author: HJH
         * @param
         * @return * @return: long UTC timeStamp
         */
        private long getNTPTime() {

            String[] hosts = new String[] { "ntp02.oal.ul.pt", "ntp04.oal.ul.pt",
                    "ntp.xs4all.nl", "time.foo.com", "time.nist.gov" };

            NTPUDPClient client = new NTPUDPClient();
            // We want to timeout if a response takes longer than 5 seconds
            client.setDefaultTimeout(2000);
            for (String host : hosts) {
                try {
                    InetAddress hostAddr = InetAddress.getByName(host);
                    TimeInfo timeInfo = client.getTime(hostAddr);
                    return timeInfo.getMessage().getTransmitTimeStamp().getTime();
                }catch (UnknownHostException e1)
                {
                    e1.printStackTrace();
                    client.close();
                    return -1;
                }catch (SocketTimeoutException e2)
                {
                    e2.printStackTrace();
                    client.close();
                    return -2;
                }
                catch (IOException e3) {
                    e3.printStackTrace();
                    client.close();
                    return -3;
                }
            }
            client.close();
            return 0;
        }
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
                case BleUIMainActivity.ACTION_GATT_CONNECTED: {
                    writeToLogFile(getString(R.string._connect));
                    mIvConnectStatus.setImageResource(R.drawable.ic_bluetooth_connected);
                    break;
                }
                case ACTION_GATT_DISCONNECTED: {
                    writeToLogFile(getString(R.string._disconnect));
                    mIvConnectStatus.setImageResource(R.drawable.ic_bluetooth_disabled);
                    mDoubleCirCleForRpmVss.setRpm(0);
                    mDoubleCirCleForRpmVss.setVss(0);
                    mTvIgnition.setText(getString(R.string.ignition_off));
                    mTvEngineHours.setText(StringUtil.sZero);
                    mTvTripDistance.setText(StringUtil.sZero);
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
            //vin 码可以通过J1939和OBD读取到,非实时数据，无需频繁保留。
            //The vin code can be read by J1939 and OBD, non-real time data, without frequent reservation.
            if(vin.equals(preVin))
                return;
            preVin = vin;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvVin.setText(vin);
                }
            });
            DBUtils.insert_VIN_inSMB(getApplication(), vin, BleSettingsActivity.sTimestamp_inSMB);
        }

        /**
         * @method onReceiveEngineHours
         * @description API回调 接收引擎启动时间  API callback for receive EngineHours
         * 2019-12-04 更新engine hours为double类型，需要将从API库获取的值除以20.
         * 2019-12-04 Updating engine hours to be of type double,it requires dividing the value obtained from the API library by 20.
         * @author: HJH
         * @param * @Param timeStamp: 时间戳 Timestamp
         * @Param time: EngineHours
         *       J1939 unit 0.05h
         *       OBD   unit 0.05s
         * @return * @return: void
         */
        @Override
        public void onReceiveEngineHours(long timeStamp, long time) {
            double enginehours = time/(double)20;
            final String eS = String.valueOf(enginehours);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvEngineHours.setText(eS);
                }
            });
            DBUtils.insertEngineHours(getApplication(), enginehours, timeStamp);

            if(!isShowDetail)
                return;
            //sb.append(StringUtil.sEngine).append(StringUtil.sColon).append(enginehours).append(StringUtil.hourUnit).append(StringUtil.sEnter);
            sb.append(StringUtil.sEngine).append(StringUtil.sColon).append(enginehours).append(StringUtil.sEnter);
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
            if (-1 == distance) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvTripDistance.setText(String.valueOf(distance));
                }
            });
            DBUtils.insertTripDistance(getApplication(), distance, timeStamp);

            if(!isShowDetail)
                return;
            sb.append(StringUtil.sTripDistance).append(StringUtil.sColon).append(distance).append(StringUtil.kilometerUnit).append(StringUtil.sEnter);
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
            if (-1 == rpm) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDoubleCirCleForRpmVss.setRpm(rpm);
                    mTvIgnition.setText(rpm == 0?getString(R.string.ignition_off):getString(R.string.ignition_on));
                }
            });
            DBUtils.insertRPM(getApplication(), rpm, timeStamp);

            if(!isShowDetail)
                return;
            //show Detail
            sb.append(StringUtil.sRpm).append(StringUtil.sColon).append(rpm).append(StringUtil.rpmUnit).append(StringUtil.sEnter);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(data.size() < 100) {
                        data.add(sb.toString());
                    }else {
                        data.clear();
                    }
                    adapter.notifyDataSetChanged();
                }
            });

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
            if (-1 == Vss) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mDoubleCirCleForRpmVss.setVss(Vss);
                    mTvTimeStamp.setText(new SimpleDateFormat(StringUtil.sSimpleDateFormat, Locale.getDefault()).format(new Date(((long) timeStamp) * 1000)));
                }
            });
            DBUtils.insertVSS(getApplication(), Vss, timeStamp);

            if(!isShowDetail)
                return;

            sb.setLength(0);
            sb.append(new SimpleDateFormat(StringUtil.sSimpleDateFormat, Locale.getDefault()).format(timeStamp*1000)).append(StringUtil.sEnter)
                    .append(StringUtil.sVin).append(StringUtil.sColon).append(preVin).append(StringUtil.sEnter)
                    .append(StringUtil.sVss).append(StringUtil.sColon).append(Vss).append(StringUtil.kilometerPerMinuteUnit).append(StringUtil.sEnter);
            if(((MApplication) getApplication()).showAsJ1939)
                sb.append(StringUtil.sFuelLevel).append(StringUtil.sColon).append(((MApplication) getApplication()).mAppData.getFuelLevel()).append(StringUtil.percentUnit).append(StringUtil.sEnter);
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
            ((MApplication) getApplication()).showAsJ1939 = Version.contains("-J");
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
            ((MApplication) getApplication()).mAppData.setSleepDelay(sleepDelay);
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
            DBUtils.insertOdometer(getApplication(), odometer, timeStamp);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(odometer != 0) {
                        preOdometer = odometer;
                        mTvOdometer.setText(String.valueOf(odometer));
                    }
                }
            });

            if(!isShowDetail)
                return;
            sb.append(StringUtil.sOdometer).append(StringUtil.sColon).append(odometer).append(StringUtil.kilometerUnit).append(StringUtil.sEnter);
            }

        @Override
        public void onReceiveDTC(long timeStamp, byte[] dtc) {
            ((MApplication) getApplication()).mAppData.setDtcList(dtc);
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
            DBUtils.insertHighPrecisionOdometer(getApplication(), highPrecisionOdometer, timeStamp);
            if(highPrecisionOdometer != 0) {
                Intent intent = new Intent(BleSettingsActivity.sHighPrecisionOdometerIntent);
                intent.putExtra(BleSettingsActivity.sHighPrecisionOdometerIntentData, highPrecisionOdometer);
                sendBroadcast(intent);
            }

            if(!isShowDetail)
                return;
            sb.append(StringUtil.sHighPrecisionOdometer).append(StringUtil.sColon).append(highPrecisionOdometer).append(StringUtil.meterUnit).append(StringUtil.sEnter);
        }

        /**
         * @method onReceiveOtherParameter
         * @description API回调 API callback for receive other para
         * ① 当ParaType等于0xD009，接收燃料液面（仅J1939 J1587）
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
            switch (paraType) {
                case DataTypeNotSync.FuelLevel:
                    ((MApplication) getApplication()).mAppData.setFuelLevel((int)paraValue);
                    break;
                default:
                    break;
            }
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
            Intent intent = new Intent(BleSettingsActivity.sResponseIntent);
            intent.putExtra(BleSettingsActivity.sResponseIntentType,responseType);
            intent.putExtra(BleSettingsActivity.sResponseIntentData, response);
            sendBroadcast(intent);

            Intent gpioIntent = new Intent(BleGpioActivity.sGpioDataIntent);
            gpioIntent.putExtra(BleGpioActivity.sGpioDataIntentType, responseType);
            gpioIntent.putExtra(BleGpioActivity.sGpioDataIntentData, response);
            sendBroadcast(gpioIntent);

            if(response == null)
                return;
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
                        ((MApplication)getApplication()).isWaitDataSync = false;
                        ToastUtils.show(R.string.toast_request_data_sync_fail);
                    }else if(reslut == 0xAA){
                        //数据同步结束 Data synchronization ends
                        ((MApplication)getApplication()).isWaitDataSync = false;
                        ToastUtils.show(R.string.toast_data_sync_success);
                    }else {
                        //请求同步成功 Request synchronization successfully
                        ((MApplication)getApplication()).isWaitDataSync = true;
                        ToastUtils.show(R.string.toast_request_data_sync_success);
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
                        ToastUtils.show(R.string.toast_send_to_eld_device_invalid);
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
                case Response.DeviceSNResponse:
                    break;
                case Response.BusProtocolResponse:
                    if (reslut == 0) {
                        //0x00 J1587 Protocol
                    } else {
                        //0xFF J1939 Protocol
                    }
                    break;
                case Response.WorkModeSetResultResponse:
                    if (reslut == 0) {
                        //0x00 设置工作模式失败 Failed to set work mode
                    } else {
                        //0xFF 设置工作模式成功 Set work mode successfully
                    }
                    break;
                case Response.DeviceSNSetResultResponse:
                    if (reslut == 0) {
                        //0x00 设置SN码失败 Failed to set device sn
                    } else {
                        //0xFF 设置SN码成功 Set device sn successfully
                    }
                    break;
                case Response.BusProtocolSetResultResponse:
                    if (reslut == 0) {
                        //0x00 设置总线协议失败 Failed to set bus protocol
                    } else {
                        //0xFF 设置总线协议成功 Set bus protocol successfully
                    }
                    break;
                case Response.SetGpioOutHighSetResultResponse:
                case Response.SetGpioOutLowSetResultResponse:
                case Response.GetGpioValResponse:
                case Response.GetAdcValResponse:
                case Response.SetPwrOutSetResultResponse:
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
            if (((MApplication) getApplication()).bleService.getState() == BluetoothProfile.STATE_CONNECTED)
                ((MApplication) getApplication()).bleService.sendMessage(response);
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
            ELD_DATA eld_data = new ELD_DATA();
            eld_data.ENGINE_Hours = data.enginehours/(double)20;
            eld_data.Trip_Distance = data.tripDistance;
            eld_data.RPM = data.rpm;
            eld_data.TIME_STAMP = data.timeStamp;
            eld_data.Odometer = data.odometer;
            eld_data.VSS = data.vss;
            eld_data.High_Precision_Odometer = data.highPrecisionOdometer;
            DBUtils.insert(getApplication(), eld_data);
            //Log.i(TAG,getString(R.string._sync));
        }

        @Override
        public void onReceiveCmdData(byte[] data) {
            Intent intent = new Intent(CmdModeActivity.sCmdDataIntent);
            intent.putExtra(CmdModeActivity.sCmdDataIntentData, new String(data));
            sendBroadcast(intent);
        }

    };

    @Override
    protected void onResume() {
        Log.i(TAG,getString(R.string._onResume));
        if(MApplication.isBleELD )
            ((MApplication) getApplication()).getService().setCallBack(mEldboxCallBack);

        if(!isConnect) {//断开时总里程置0  Odometer set zero when bluetooh disconnected
            preOdometer = 0;
            mTvOdometer.setText(StringUtil.sZero);
        }
        //SyncTimeWhenConnectNetwork();//hjh 20190921 add for sync time
        SyncTimeFromTablet();

        DBUtils.queryLastOneData(getApplication(), new DBUtils.OnGetLastOneListener() {
            @Override
            public void onGetLastOne(final List<ELD_DATA> list) {
                // TODO: 8/21/2019 从数据库读取总里程  Read odometer from the database
                if (list.size() > 0) {
                        runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //直接读取1939的总里程 Read the odometer of 1939 directly
                            if(list.get(0).Odometer != 0) {
                                preOdometer = list.get(0).Odometer;
                                mTvOdometer.setText(preOdometer +"");
                            }
                        }
                    });
                }
            }
        });


        DBUtils.queryLastOneData_inSMB(getApplication(), new DBUtils.OnGetSMBLastOneListener() {
            @Override
            public void onGetLastOne(final List<StaticMessage> list) {
                // TODO: 8/21/2019 如果数据库读取的总里程为空，使用用户输入的总里程。
                // If the odometer read from the database is empty, use the odometer entered by the user.
                if (list.size() > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //避免1939读取的总里程被覆盖,OBD的总里程手动写入
                            // Avoid the odometer read by 1939 is overwritten, if value is zeor,the odometer of OBD is manually written
                            if(mTvOdometer.getText().equals("0"))
                                mTvOdometer.setText(String.valueOf(list.get(0).Odometer_For_OBD));

                            mTvVin.setText(String.valueOf(list.get(0).VIN));
                            //hjh 2019-08-30 remove because view visbility set gone
                            //mTvEngineNumber.setText(String.valueOf(list.get(0).Engine_Number));
                        }
                    });
                }
            }
        });
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
        if ((System.currentTimeMillis() - lastClick) > 2000) {
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
        if(queryOtherInfoThread != null) {
            queryOtherInfoThread.interrupt();
            queryOtherInfoThread = null;
        }

        if(MApplication.isBleELD) {
            unregisterReceiver(receiver);
			((MApplication) getApplication()).getService().disConnect();
        	((MApplication) getApplication()).unBindAndStopService();
		}
        else {
            ((MApplication) getApplication()).disConnectSerial();
        }

        ToastUtils.cancel();
        System.exit(0);

        super.onDestroy();
    }
}
