package com.android.eldbox.ble_ui;


import android.app.AlertDialog;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;

import com.android.eldbox.StringUtil;
import com.android.eldbox.tools.Utils;
import com.android.eldbox_api.CommandEnum;
import com.android.eldbox_api.Response;
import com.hjq.toast.ToastUtils;

import com.android.eldbox.MApplication;
import com.android.eldbox.DataBase.DBUtils;
import com.android.eldbox.DataBase.ELD_DATA;
import com.android.eldbox.DataBase.StaticMessage;
import com.android.eldbox.R;
import com.android.eldbox.View.ProgressDialog;
import com.android.eldbox.tools.NetUtils;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static com.android.eldbox.tools.Utils.writeToLogFile;

/**
  *
  * @ProjectName:
  * @Package:        com.android.eldbox
  * @ClassName:      BleSettingsActivity
  * @Description:     ELD Settings
  * @Author:         HGY
  * @UpdateUser:     HJH
  * @UpdateDate:
  * @UpdateRemark:
  * @Version:        1.0
 */
public class BleSettingsActivity extends AppCompatActivity {

    //Control
    Switch   mSwitchStoreMode;
    EditText mEtSleepDelay;
    Button   mBtnTimeSync;
    Button   mBtnExportData;
    Button   mBtnClearDatabase;
    Button   mBtnSleepDelay;
    //hjh 2019-08-30 remove because view visbility set gone
    //Button   mBtnEngineNumber;
    Button   mBtnOdometer;
    Button   mBtnShowDTC;
    Button   mBtnClearDTC;
    Button   mBtnDefaultPath;
    Button   mBtnSyncStart;
    Button         mBtnOTA;
    Button   mBtnCmdMode;
    Button   mBtnPTRMode;
    Button   mBtnBusProtocol;
    Button   mBtnEnterAdcAndGpio;

    TextView mTvHighPrecisionOdometerTitle;
    TextView mTvHighPrecisionOdometerUnit;
    TextView mTvOdometerTitle;
    TextView mTvBusProtocolTitle;
    TextView mTvBoxSN;
    TextView mTvBoxVersion;
    TextView mTvAppNameVersion;
    TextView mTvIgnition;
    EditText mEtOdometer;
    TextView mTvDefaultPath;
    TextView mTvHighPrecisionOdometer;
    TextView mTvAdcAndGpioTitle;

    //Data sync select radiobtn
    RadioButton mRBtn_30min,mRBtn_1day,mRBtn_3day, mRBtn_8day;
	//Bus protocol select radiobtn
    RadioButton mRBtn_J1587,mRBtn_J1939;

    ProgressDialog mProgressDialog;

    //other
    int syncStartMinute = 0;
    int syncEndMinute = 0;
    int syncRadioBtnSelect = 0;//what the radio button select for sync data
    int busProtocolSelect = 0;//what the radio button select for bus protocol,default is J1939

    //0 ELD 255 CMD 221 PTR
    int mCurrentWorkMode = 0;
    int mNextWorkMode = 0;

    private static final String TAG = "BleSettingsActivity";
    private static final String sTimePickViewSimpleDateFormat = "MM/dd HH:mm";

    //for save default path
    private String              defaultPath            = "sdcard/ELD_Log/";
    private static final String sSharedPreferencesName = "Setting";
    private static final String sSharedPreferences_Path = "path";

    //save ELD data
    private static final String sEldFileExportSimpleDateFormat = "yyyy-MM-dd-HH:mm:ss";
    private static final String sEldFileExportSuffix = ".csv";
    private static final String sEldFileTimeStamp = "yyyy-MM-dd HH:mm:ss";
    //save ELD static data
    private static final String sEldSMBFileName = "EldBoxMessage.csv";
    private static final String sEldSMBFileTimeStamp = "yyyy-MM-dd";
    public static int sTimestamp_inSMB = 0;//save staic data by id.


    //Intent
    public static final String sHighPrecisionOdometerIntent     = "HighPrecisionOdometer";
    public static final String sHighPrecisionOdometerIntentData = "highPrecisionOdometer";

    public static final String sResponseIntent = "Response";
    public static final String sResponseIntentType = "responseType";
    public static final String sResponseIntentData = "responseData";

    Thread queryBaseInfoThread = null;

    // 使用weakreference避免内存泄漏.
    // need weakreference  to avoid memory leaks.
    UiHandler uiHandler = new UiHandler(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initControl();
        init();
    }

    private void init() {
        //读取版本和休眠延时时间 Read version and sleep delay time
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(sHighPrecisionOdometerIntent);
        intentFilter.addAction(sResponseIntent);
        registerReceiver(receiver, intentFilter);
    }

    /**
     * @method initControl
     * @description 初始化控件  Initialize control
     * @author: HJH
     * @param
     * @return * @return: void
     */
    private void initControl(){
        //findView TextView
        mTvHighPrecisionOdometerTitle = findViewById(R.id.tv_high_precision_odometer_title);
        mTvHighPrecisionOdometerUnit = findViewById(R.id.tv_high_precision_odometer_unit);
        mTvOdometerTitle = findViewById(R.id.tv_odometer_title);
        mTvBusProtocolTitle = findViewById(R.id.tv_bus_protocol_title);
        mTvBoxSN = findViewById(R.id.tv_box_sn);
        mTvBoxVersion = findViewById(R.id.tv_box_version);
        mTvAppNameVersion = findViewById(R.id.tv_app_version_name);
        mTvIgnition = findViewById(R.id.et_engine_number);
        mEtOdometer = findViewById(R.id.et_odometer);
        mTvDefaultPath = findViewById(R.id.tv_default_path);
        mTvHighPrecisionOdometer = findViewById(R.id.tv_high_precision_odometer);
        mTvAdcAndGpioTitle = findViewById(R.id.tv_adc_and_gpio_title);

        //findView EditView
        mEtSleepDelay = findViewById(R.id.et_sleep_delay);

        //findView Button
        mBtnTimeSync = findViewById(R.id.btn_time_sync);
        mBtnExportData = findViewById(R.id.btn_export_data);
        mBtnClearDatabase = findViewById(R.id.btn_clear_database);
        mBtnSleepDelay = findViewById(R.id.btn_sleep_delay);
        //hjh 2019-08-30 remove because view visbility set gone
        //mBtnEngineNumber = findViewById(R.id.btn_engine_number);
        mBtnOdometer = findViewById(R.id.btn_odometer);
        mBtnShowDTC = findViewById(R.id.btn_dtc_show);
        mBtnClearDTC = findViewById(R.id.btn_dtc_clear);
        mBtnDefaultPath = findViewById(R.id.btn_default_path);
        mBtnSyncStart = findViewById(R.id.btn_sync_start);
        mBtnOTA = findViewById(R.id.btn_device_ota);
        mBtnCmdMode = findViewById(R.id.btn_cmd_mode);
        mBtnPTRMode = findViewById(R.id.btn_ptr_mode);
        mBtnBusProtocol = findViewById(R.id.btn_bus_protocol);
        mBtnEnterAdcAndGpio = findViewById(R.id.btn_enter_adc_and_gpio);

        //findView Switch
        mSwitchStoreMode = findViewById(R.id.switch_store_mode);

        //findView RadioButton
        mRBtn_30min = findViewById(R.id.rbtn_30min);
        mRBtn_1day = findViewById(R.id.rbtn_1day);
        mRBtn_3day = findViewById(R.id.rbtn_3day);
        mRBtn_8day = findViewById(R.id.rbtn_8day);
        mRBtn_J1587 = findViewById(R.id.rbtn_j1587);
        mRBtn_J1939 = findViewById(R.id.rbtn_j1939);

        showAsJ1939(((MApplication) getApplication()).showAsJ1939);

        //setOnclick
        mBtnTimeSync.setOnClickListener(new mELDSettingClick());
        mBtnExportData.setOnClickListener(new mELDSettingClick());
        mBtnClearDatabase.setOnClickListener(new mELDSettingClick());
        mBtnSleepDelay.setOnClickListener(new mELDSettingClick());
        //hjh 2019-08-30 remove because view visbility set gone
        //mBtnEngineNumber.setOnClickListener(new mELDSettingClick());
        mBtnOdometer.setOnClickListener(new mELDSettingClick());
        mBtnShowDTC.setOnClickListener(new mELDSettingClick());
        mBtnClearDTC.setOnClickListener(new mELDSettingClick());
        mBtnDefaultPath.setOnClickListener(new mELDSettingClick());
        mBtnOTA.setOnClickListener(new mELDSettingClick());
        mBtnSyncStart.setOnClickListener(new mELDSettingClick());
        mBtnCmdMode.setOnClickListener(new mELDSettingClick());
        mBtnPTRMode.setOnClickListener(new mELDSettingClick());
        mBtnBusProtocol.setOnClickListener(new mELDSettingClick());
        mBtnEnterAdcAndGpio.setOnClickListener(new mELDSettingClick());
        //Radio button for data sync
        mRBtn_30min.setOnClickListener(new mELDSettingClick());
        mRBtn_1day.setOnClickListener(new mELDSettingClick());
        mRBtn_3day.setOnClickListener(new mELDSettingClick());
        mRBtn_8day.setOnClickListener(new mELDSettingClick());
        //Radio button for bus protocol
        mRBtn_J1587.setOnClickListener(new mELDSettingClick());
        mRBtn_J1939.setOnClickListener(new mELDSettingClick());

        mSwitchStoreMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                ((MApplication) getApplication()).getService().setStorageMode(isChecked);
            }
        });
        mTvAppNameVersion.setText(Utils.getVerName(BleSettingsActivity.this));
    }

    /**
     * @interface
     * @description 按键监听接口  Button click implement interface
     * @author: HJH
     * @param * @Param null:
     * @return * @return: null
     */
    class mELDSettingClick implements View.OnClickListener{

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_time_sync:
                    if (!NetUtils.isConnected(BleSettingsActivity.this)) {//no network
                        ToastUtils.show(R.string.toast_time_sync_should_connect_network);
                        break;
                    }
                    //for Bluetooth ELD. Do not sync time when the device is not connected
                    if(MApplication.isBleELD && ((MApplication) getApplication()).getService().getState() == BluetoothProfile.STATE_DISCONNECTED) {
                        ToastUtils.show(R.string.toast_time_sync_should_connect_device);
                        break;
                    }
                    NTPThread ntpThread = new NTPThread();
                    ntpThread.start();
                    //sync time
                    break;
                case R.id.btn_export_data:
                    exportDataToFile();
                    break;
                case R.id.btn_clear_database:
                    AlertDialog.Builder builder = new AlertDialog.Builder(BleSettingsActivity.this);
                    builder.setTitle(R.string.clear_database_dialog_title)
                            .setMessage(R.string.clear_database_dialog_message)
                            .setPositiveButton(R.string._ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //DBUtils.deleteEldData(getApplication(),0, System.currentTimeMillis());
                                    DBUtils.deleteEldData(getApplication());
                                    ToastUtils.show(R.string.toast_clear_database_success);
                                    dialog.dismiss();
                                }
                            })
                            .setNegativeButton(R.string._cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).show();
                    break;
                case R.id.btn_sleep_delay:
                    int delay;
                    try {
                        delay = Integer.parseInt(mEtSleepDelay.getText().toString());
                    } catch (Exception e) {
                        delay = 20;
                    }
                    if (delay > 200) {
                        ToastUtils.show(R.string.toast_sleep_time_too_big);
                        return;
                    }
                    ((MApplication) getApplication()).getService().setSleepDelay(delay);
                    break;
                //hjh 2019-08-30 remove because view visbility set gone
                /*
                case R.id.btn_engine_number:
                    final String engineNumber = mTvEngineNumber.getText().toString();
                    if (!engineNumber.equals("")) {
                        DBUtils.queryLastOneData_inSMB(getApplication(), new DBUtils.OnGetSMBLastOneListener() {
                            @Override
                            public void onGetLastOne(List<StaticMessage> list) {
                                if (list.size() > 0) {
                                    list.get(0).Engine_Number = engineNumber;
                                    DBUtils.update_inSMB(getApplication(), list.get(0));
                                } else {
                                    DBUtils.insert_EngineNumber_inSMB(getApplication(), engineNumber, sTimestamp_inSMB);
                                }
                            }
                        });
                    } else {
                        ToastUtils.show(R.string.toast_input_correct);
                    }
                    break;
                    */
                case R.id.btn_odometer:
                    int odometerValue;
                    try {
                        odometerValue = Integer.parseInt(mEtOdometer.getText().toString());
                    } catch (Exception e) {
                        odometerValue = 0;
                    }
                    final int finalOdometerValue = odometerValue;
                    DBUtils.queryLastOneData_inSMB(getApplication(), new DBUtils.OnGetSMBLastOneListener() {
                        @Override
                        public void onGetLastOne(List<StaticMessage> list) {
                            if (list.size() > 0) {
                                list.get(0).Odometer_For_OBD = finalOdometerValue;
                                DBUtils.update_inSMB(getApplication(), list.get(0));
                            } else {
                                DBUtils.insert_OdometerForOBD_inSMB(getApplication(), finalOdometerValue, sTimestamp_inSMB);
                            }
                        }
                    });
                    break;
                case R.id.btn_dtc_show:
                    showDTC(BleSettingsActivity.this,((MApplication) getApplication()).mAppData.getDtcList());
                    break;
                case R.id.btn_dtc_clear:
                    mBtnShowDTC.setEnabled(false);
                    ((MApplication) getApplication()).getService().requestClearDTC();

                    //延时发送启用按钮的功能  Delay enable button function
                    uiHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mBtnShowDTC.setEnabled(true);
                                }
                            });
                        }
                    }, 10000);
                    break;
                case R.id.btn_default_path:
                    startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), 1);
                    break;
                case R.id.rbtn_30min:
                    syncRadioBtnSelect = 0;
                    break;
                case R.id.rbtn_1day:
                    syncRadioBtnSelect = 1;
                    break;
                case R.id.rbtn_3day:
                    syncRadioBtnSelect = 2;
                    break;
                case R.id.rbtn_8day:
                    syncRadioBtnSelect = 3;
                    break;
                case R.id.btn_sync_start:
                    switch (syncRadioBtnSelect) {
                        case 0://30 min
                            syncStartMinute = 30;
                            break;
                        case 1://1 day
                            syncStartMinute = 24 * 60;
                            break;
                        case 2://3 day
                            syncStartMinute = 3 * 24 * 60;
                            break;
                        case 3://8 day
                            syncStartMinute = 8 * 24 * 60;
                            break;
                        default:
                            break;
                    }
                    syncEndMinute = 0;
                    requestSync(syncStartMinute, syncEndMinute);
                    break;
                case R.id.btn_device_ota:
                    startActivity(new Intent(getApplicationContext(), BleOTAActivity.class));
                    break;
                case R.id.btn_cmd_mode:
                    if(queryBaseInfoThread != null) {
                        queryBaseInfoThread.interrupt();
                        queryBaseInfoThread = null;
                    }
                    mNextWorkMode = 255;
                    startActivity(new Intent(getApplicationContext(), CmdModeActivity.class));
                    break;
                case R.id.btn_ptr_mode:
                    if(mBtnPTRMode.getText().toString().equals(getString(R.string.btn_enter_ptr_mode))) {
                        AlertDialog.Builder builder2ptr = new AlertDialog.Builder(BleSettingsActivity.this);
                        builder2ptr.setTitle(R.string.enter_ptr_mode_dialog_title)
                                .setMessage(R.string.enter_ptr_mode_dialog_message)
                                .setPositiveButton(R.string._ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mNextWorkMode =221;
                                        mBtnPTRMode.setEnabled(false);
                                        ((MApplication) getApplication()).getService().setWorkMode(CommandEnum.WorkMode.PTR_MODE);
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(R.string._cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    }else {
                        AlertDialog.Builder builder2eld = new AlertDialog.Builder(BleSettingsActivity.this);
                        builder2eld.setTitle(R.string.enter_eld_mode_dialog_title)
                                .setMessage(R.string.enter_eld_mode_dialog_message)
                                .setPositiveButton(R.string._ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        mNextWorkMode = 0;
                                        mBtnPTRMode.setEnabled(false);
                                        ((MApplication) getApplication()).getService().setWorkMode(CommandEnum.WorkMode.ELD_MODE);
                                        dialog.dismiss();
                                    }
                                })
                                .setNegativeButton(R.string._cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
                    break;
                case R.id.rbtn_j1587:
                    busProtocolSelect = 1;
                    break;
                case R.id.rbtn_j1939:
                    busProtocolSelect = 0;
                    break;
                case R.id.btn_bus_protocol:
                    switch (busProtocolSelect)
                    {
                        case 0:
                            ((MApplication) getApplication()).getService().setBusProtocol(CommandEnum.BusProtocol.J1939_Protocol);
                            break;
                        case 1:
                            ((MApplication) getApplication()).getService().setBusProtocol(CommandEnum.BusProtocol.J1587_Protocol);
                            break;
                    }
                    break;
                case R.id.btn_enter_adc_and_gpio:
                    Intent enter_adc_intent = new Intent(BleSettingsActivity.this, BleGpioActivity.class);
                    startActivity(enter_adc_intent);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @method requestSync
     * @description 同步选定时间段的数据（8*24小时) Synchronize data for the selected time period  （8*24 hour)
     * @author: HJH
     * @param * @Param begin:begin minute,range 0~11520
     * @Param end:end minute,range range 0~11520, should small than beginMinutes
     * @return * @return: void
     */
    public void requestSync(int beginMinutes, int endMinutes) {
        if (endMinutes >= beginMinutes) {
            ToastUtils.show(R.string.toast_pick_time_error);
            return;
        }
        try {
            ((MApplication) getApplication()).bleService.requestDataSync(beginMinutes, endMinutes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @method requestSync
     * @description 同步选定时间段的数据（8*24小时) Synchronize data for the selected time period  （8*24 hour)
     * @author: HJH
     * @param * @Param begin: begin Date
     * @Param end:end Date
     * @return * @return: void
     */
    public void requestSync(Date begin, Date end) {
        long currentTime = System.currentTimeMillis();
        int beginMinutes = (int) ((currentTime - begin.getTime()) / (1000 * 60));
        int endMinutes = (int) ((currentTime - end.getTime()) / (1000 * 60));
        if (endMinutes > beginMinutes) {
            ToastUtils.show(R.string.toast_pick_time_error);
            return;
        }
        try {
            ((MApplication) getApplication()).bleService.requestDataSync(beginMinutes, endMinutes);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case sHighPrecisionOdometerIntent:
                    mTvHighPrecisionOdometer.setText(String.valueOf(intent.getLongExtra(sHighPrecisionOdometerIntentData,0)));
                    break;
                case sResponseIntent:
                    int responseType = intent.getIntExtra(sResponseIntentType,0);
                    byte[] responseData = intent.getByteArrayExtra(sResponseIntentData);

                    if(responseData == null)
                        break;
                    int reslut = responseData[0] & 0xff;
                    switch (responseType){
                        case Response.TimeSyncResponse:
                            if(reslut == 0)
                            {
                                //0x00 设置同步时间失败 Failed to sync time
                                ToastUtils.show(R.string.toast_time_sync_fail);
                            }else{
                                //0xFF 设置同步时间成功 sync time successfully
                                ToastUtils.show(R.string.toast_time_sync_success);
                            }
                            break;
                        case Response.DataSyncResponse:
                            if(reslut == 0)
                            {
                                //0x00 请求同步失败 Request synchronization failed
                                ((MApplication)getApplication()).isWaitDataSync = false;
                                dataSyncUI(false);
                                ToastUtils.show(R.string.toast_request_data_sync_fail);
                            }else if(reslut == 0xAA){
                                //0xAA 数据同步结束 Data synchronization ends
                                ((MApplication)getApplication()).isWaitDataSync = false;
                                dataSyncUI(false);
                                ToastUtils.show(R.string.toast_data_sync_success);
                            }else {
                                //0xFF请求同步成功 Request synchronization successfully
                                ((MApplication)getApplication()).isWaitDataSync = true;
                                dataSyncUI(true);
                                ToastUtils.show(R.string.toast_request_data_sync_success);
                            }
                            break;
                        case Response.SleepDelayResponse:
                            if(reslut == 0) {
                                //0x00 设置休眠时间失败 Failed to set sleep time
                                ToastUtils.show(R.string.toast_set_sleep_delay_failed);
                            }else {
                                //0xFF 设置休眠时间成功 Set sleep time successfully
                                ToastUtils.show(R.string.toast_set_sleep_delay_success);
                            }
                            break;
                        case Response.VerifyResponse:
                            //0x00 接收到的数据校验失败 Received data validation failed
                            //0xFF 接收到的数据校验正确 The received data check is correct
                            if(reslut == 0)
                            {
                                ToastUtils.show(R.string.toast_send_to_eld_device_invalid);
                            }
                            break;
                        case Response.WorkModeResponse:
                            //0x00 当前工作模式为ELD  Current work mode is Eld mode
                            //0xFF 当前工作模式为CMD  Current work mode is cmd mode
                            //0xDD 当前工作模式为PTR  Current work mode is PTR mode
                            int workmode = reslut;
                            mCurrentWorkMode = reslut;
                            mBtnCmdMode.setEnabled(workmode == 0);
                            if(workmode == 0xFF) {
                                //if current work mode is cmd mode,try change to eld mode
                                ((MApplication) getApplication()).bleService.setWorkMode(CommandEnum.WorkMode.ELD_MODE);
							}else if(workmode == 0xDD){
                                mBtnPTRMode.setEnabled(true);
                                mBtnPTRMode.setText(R.string.btn_enter_eld_mode);
                            }else if(workmode == 0x00)
                            {
                                mBtnPTRMode.setEnabled(true);
                                mBtnPTRMode.setText(R.string.btn_enter_ptr_mode);
                            }
                            break;
                        case Response.DeviceSNResponse:
                            mTvBoxSN.setText(new String(responseData));
                            break;
                        case Response.BusProtocolResponse:
                            if (reslut == 0) {
                                //0x00 J1587 Protocol
                                mRBtn_J1587.setChecked(true);
                            } else {
                                //0xFF J1939 Protocol
                                mRBtn_J1939.setChecked(true);
                            }
                            break;
                        case Response.WorkModeSetResultResponse:
                            if(mCurrentWorkMode == mNextWorkMode && mCurrentWorkMode == 255) {//if CMDModeActivity finish ,change to ELD auto.
                                mBtnCmdMode.setEnabled(true);
                                mNextWorkMode = 0;
                            }
                            if (reslut == 0) {
                                //0x00 设置工作模式失败 Failed to set work mode
                                ToastUtils.show(R.string.toast_set_work_mode_failed);
                                mNextWorkMode = mCurrentWorkMode;
                                mBtnCmdMode.setEnabled(mCurrentWorkMode == 0);
                                //启用PTR or ELD模式按钮  enable PTR or ELD mode button
                                switch (mCurrentWorkMode) {
                                    case 0://0x00 ELD
                                        mBtnPTRMode.setText(R.string.btn_enter_ptr_mode);
                                        break;
                                    case 221://0xDD PTR
                                        mBtnPTRMode.setText(R.string.btn_enter_eld_mode);
                                        break;
                                    case 255://0xFF CMD
                                        mBtnPTRMode.setText(R.string.btn_enter_ptr_mode);
                                        break;
                                    default:
                                        break;
                                }
                                mBtnPTRMode.setEnabled(true);
                            } else {
                                //0xFF 设置工作模式成功 Set work mode successfully
                                ToastUtils.show(R.string.toast_set_work_mode_success);
                                mCurrentWorkMode = mNextWorkMode;
                                mBtnCmdMode.setEnabled(mCurrentWorkMode == 0);
                                //启用PTR or ELD模式按钮  enable PTR or ELD mode button
                                switch (mCurrentWorkMode) {
                                    case 0://0x00 ELD
                                        mBtnPTRMode.setText(R.string.btn_enter_ptr_mode);
                                        break;
                                    case 221://0xDD PTR
                                        mBtnPTRMode.setText(R.string.btn_enter_eld_mode);
                                        break;
                                    case 255://0xFF CMD
                                        mBtnPTRMode.setText(R.string.btn_enter_ptr_mode);
                                        break;
                                    default:
                                        break;
                                }
                                mBtnPTRMode.setEnabled(true);
                            }
                            break;
                        case Response.DeviceSNSetResultResponse:
                            if (reslut == 0) {
                                //0x00 设置SN码失败 Failed to set device sn
                                ToastUtils.show(R.string.toast_set_device_sn_failed);
                            } else {
                                //0xFF 设置SN码成功 Set device sn successfully
                                ToastUtils.show(R.string.toast_set_device_sn_success);
                            }
                            break;
                        case Response.BusProtocolSetResultResponse:
                            if (reslut == 0) {
                                //0x00 设置总线协议失败 Failed to set bus protocol
                                ToastUtils.show(R.string.toast_set_bus_protocol_failed);
                            } else {
                                //0xFF 设置总线协议成功 Set bus protocol successfully
                                ToastUtils.show(R.string.toast_set_bus_protocol_success);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
            }
        }
    };

    /**
     * @method dataSyncUI
     * @description 根据当前数据同步状态刷新UI
     * @date: 12/5/2019 11:34 AM
     * @author: HJH
     * @param * @Param isSync:
     * @return * @return: void
     */
    private void dataSyncUI(boolean isSync){
        if(isSync) {
            ((MApplication)getApplication()).isWaitDataSync = true;
            mBtnSyncStart.setText(R.string.data_synching_button);
            mBtnSyncStart.setEnabled(false);
        }
        else {
            ((MApplication)getApplication()).isWaitDataSync = false;
            mBtnSyncStart.setText(R.string.data_sync_button);
            mBtnSyncStart.setEnabled(true);
        }
    }

    /**
     * @method exportDataToFile
     * @description export data to .xlsx file
     *      * name as yyyy-MM-dd.xlsx
     * @author: HJH
     * @param
     * @return * @return: void
     */
    private void exportDataToFile() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                exportToCSV();
            }
        }).start();
    }

    /**
     * @method exportToCSV
     * @description 导出ELD数据到CSV格式文件 Export ELD data to a CSV format file
     * //使用逗号分隔符（.csv）作为导出的文本格式 Use a comma delimiter (.csv) as the exported text format
     * //之所以不使用excel，是因为大部分java excel工具，导出几十万数据的时候都很容易遇到oom，而且相当慢
     * //Because most java excel tools, it is easy to encounter oom when exporting hundreds of thousands of data, and it is quite slow. So don't use execl
     * @author: HJH
     * @param
     * @return * @return: void
     */
    private void exportToCSV() {
        writeToLogFile(getString(R.string._export_start));
        //ELD 数据导出 ELD data export
        StringBuffer sb = new StringBuffer();
        String FileName = new SimpleDateFormat(sEldFileExportSimpleDateFormat, Locale.getDefault()).format(System.currentTimeMillis()) + sEldFileExportSuffix;
        File directory = new File(defaultPath);
        File file = new File(defaultPath + FileName);
        if (!directory.exists())
        {
            directory.mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        uiHandler.sendEmptyMessage(Flag.SHOW_PROGRESSDIALOG);
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file, true);
            sb.append(StringUtil.sTime).append(StringUtil.sComma).append(StringUtil.sTripDistance).append(StringUtil.sComma).append(StringUtil.sRpm).append(StringUtil.sComma).append(StringUtil.sVss).append(StringUtil.sComma).append(StringUtil.sEngine).append(StringUtil.sComma).append(StringUtil.sOdometer).append(StringUtil.sComma).append(StringUtil.sHighPrecisionOdometer).append(StringUtil.sComma).append(StringUtil.sEnter);
            fileOutputStream.write(sb.toString().getBytes());
            sb.setLength(0);
            int dataCount = ((MApplication) getApplication()).dataBase.dao().getCount();
            int offset = 0;
            List<ELD_DATA> list;
            while (dataCount > 0) {
                if (dataCount > 100) {
                    list = ((MApplication) getApplication()).dataBase.dao().getDataFromXToY(offset, 100);
                    offset += 100;
                    dataCount -= 100;
                } else {
                    list = ((MApplication) getApplication()).dataBase.dao().getDataFromXToY(offset, dataCount);
                    dataCount = 0;
                }
                for (ELD_DATA d :
                        list) {
                    //simpleDateFormat的单位是ms，而储存在数据库里的是s，需要做一个数据类型的转换
                    //The unit of simpleDateFormat is ms, and the second is stored in the database, you need to do a data type conversion
                    sb.append(new SimpleDateFormat(sEldFileTimeStamp, Locale.getDefault()).format(new Date((long) d.TIME_STAMP * 1000))).append(StringUtil.sComma)
                            .append(d.Trip_Distance).append(StringUtil.sComma)
                            .append(d.RPM).append(StringUtil.sComma)
                            .append(d.VSS).append(StringUtil.sComma)
                            .append(d.ENGINE_Hours).append(StringUtil.sComma)
                            .append(d.Odometer).append(StringUtil.sComma)
                            .append(d.High_Precision_Odometer).append(StringUtil.sComma)
                            .append(StringUtil.sEnter);
                    fileOutputStream.write(sb.toString().getBytes());
                    sb.setLength(0);
                }
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            refreshFile(DocumentFile.fromFile(file));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //固定数据导出  Fixed data export
        FileName = sEldSMBFileName;
        file = new File(defaultPath + FileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(file, true);
            sb.setLength(0);
            sb.append(StringUtil.sTime).append(StringUtil.sComma).append(StringUtil.sVin).append(StringUtil.sComma).append(StringUtil.sEngineNumber).append(StringUtil.sComma).append(StringUtil.sOdometer).append(StringUtil.sComma).append(StringUtil.sEnter);
            outputStream.write(sb.toString().getBytes());
            sb.setLength(0);
            int dataCount = ((MApplication) getApplication()).dataBase.dao().getSMBCount();
            int offset = 0;
            List<StaticMessage> list;
            while (dataCount > 0) {
                if (dataCount > 100) {
                    list = ((MApplication) getApplication()).dataBase.dao().getDataFromSMBXToY(offset, 100);
                    offset += 100;
                    dataCount -= 100;
                } else {
                    list = ((MApplication) getApplication()).dataBase.dao().getDataFromSMBXToY(offset, dataCount);
                    dataCount = 0;
                }
            for (StaticMessage s :
                    list) {
                sb.append(new SimpleDateFormat(sEldSMBFileTimeStamp, Locale.getDefault()).format(new Date((long) s.TIME_STAMP * 1000))).append(StringUtil.sComma)
                        .append(s.VIN).append(StringUtil.sComma)
                        .append(s.Engine_Number).append(StringUtil.sComma)
                        .append(s.Odometer_For_OBD).append(StringUtil.sComma)
                        .append(StringUtil.sEnter);
                outputStream.write(sb.toString().getBytes());
                sb.setLength(0);
            }}
            outputStream.close();
            refreshFile(DocumentFile.fromFile(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        uiHandler.sendEmptyMessage(Flag.HIDE_DIALOG);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtils.show(R.string.toast_export_data);
            }
        });
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
            ((MApplication) getApplication()).getService().requestTimeSync(waitToSyncTime);
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
                            //ToastUtils.show(R.string.toast_time_sync_success);
                        }
                    });
                    break;
            }

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
     * @method showDTC
     * @description 弹出Listview的Dialog，用来显示故障码 Pop up the Listview's Dialog to display the failure code
     * @param * @Param context:
     * @Param dtc: 故障码
     * @return * @return: void
     */
    private void showDTC(Context context,byte[] dtc){
        String[] dtcArr = new String(dtc).split(StringUtil.sEnter);

        final LinearLayout linearLayoutMain = new LinearLayout(context);//自定义一个布局文件
        linearLayoutMain.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        final ListView listView = new ListView(context);//this为获取当前的上下文
        listView.setFadingEdgeLength(0);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_expandable_list_item_1, dtcArr);

        listView.setAdapter(adapter);
        listView.setClickable(false);
        linearLayoutMain.addView(listView, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);//往这个布局中加入listview

        final AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(linearLayoutMain)//在这里把写好的这个listview的布局加载dialog中
                .create();
        dialog.setCanceledOnTouchOutside(true);//使除了dialog以外的地方能被点击
        dialog.show();
    }

    /**
     * @method onActivityResult
     * @description 导出路径设置，尚不可用//todo  Export path settings ，not yet available
     * @author: HJH
     * @param * @Param requestCode:
     * @Param resultCode:
     * @Param data:
     * @return * @return: void
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case 1: {
                if (data != null) {
                    Uri treeUri = data.getData();
                    if (treeUri == null) return;
                    savepath(treeUri.toString());
                    getApplicationContext().grantUriPermission(getApplicationContext().getPackageName(), treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    getApplicationContext().getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
    此处的handler应该改成handlerThread的形式
    使用WeakReference 的原因是原来的handler会获取持有Activity对象，如果activity退出时handler还没有执行完，就会造成内存泄漏
    换成handlerThread就没这个问题了
    参照eldbox_api的SerialAgent
    The handler here should be changed to handlerThread
   The reason for using WeakReference is that the original handler will hold the Activity object. If the handler hasn't been release when the activity finished, it will cause a memory leak.
   It is not a problem with the handlerThread.Refer to the SerialAgent of eldbox_api.
    */
    static class UiHandler extends Handler {
        WeakReference<BleSettingsActivity> settingsActivityWeakReference;

        UiHandler(BleSettingsActivity settingsActivity) {
            this.settingsActivityWeakReference = new WeakReference<>(settingsActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Flag.SHOW_PROGRESSDIALOG: {
                    if (settingsActivityWeakReference.get().mProgressDialog == null) {
                        settingsActivityWeakReference.get().mProgressDialog = new ProgressDialog(settingsActivityWeakReference.get());
                        settingsActivityWeakReference.get().mProgressDialog.show();
                    }
                }
                break;
                case Flag.HIDE_DIALOG: {
                    if (settingsActivityWeakReference.get().mProgressDialog != null) {
                        settingsActivityWeakReference.get().mProgressDialog.dismiss();
                        settingsActivityWeakReference.get().mProgressDialog = null;
                    }
                }

            }
        }
    }

    /**
     * @method refreshFile
     * @description 刷新文件列表，使文件能够在文件管理器等被看到。  Refresh the file list so that the file can be seen in the file manager, etc.
     * @author: HJH
     * @param file 文件路径 file path
     * @return * @return: void
     */
    private void refreshFile(DocumentFile file) {
        if (file == null) return;
        Uri localUri = file.getUri();
        Intent localIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, localUri);
        sendBroadcast(localIntent);
    }

    @Override
    protected void onDestroy() {
        if(queryBaseInfoThread != null) {
            queryBaseInfoThread.interrupt();
            queryBaseInfoThread = null;
        }

        uiHandler.removeCallbacksAndMessages(null);
        unregisterReceiver(receiver);

        ToastUtils.cancel();

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        //读取总里程和引擎码 Read odometer and engine number
        DBUtils.queryLastOneData_inSMB(getApplication(), new DBUtils.OnGetSMBLastOneListener() {
            @Override
            public void onGetLastOne(final List<StaticMessage> list) {
                if (list.size() > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //hjh 2019-08-30 remove because view visbility set gone
                            //mTvEngineNumber.setHint(String.valueOf(list.get(0).Engine_Number));
                            mEtOdometer.setHint(String.valueOf(list.get(0).Odometer_For_OBD));
                        }
                    });

                }
            }
        });
        //如果数据库读取的总里程为空，使用用户输入的总里程。
        // If the odometer read from the database is empty, use the odometer entered by the user.
        DBUtils.queryLastOneData(getApplication(), new DBUtils.OnGetLastOneListener() {
            @Override
            public void onGetLastOne(final List<ELD_DATA> list) {
                if (list.size() > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(mEtOdometer.getText() == null || mEtOdometer.getText().equals(StringUtil.sZero)) {
                                mEtOdometer.setHint(String.valueOf(list.get(0).Odometer));
                                mEtOdometer.setEnabled(false);
                            }
                        }
                    });
                }
            }
        });

        dataSyncUI(((MApplication)getApplication()).isWaitDataSync);

        if (((MApplication) getApplication()).getService().getState() == BluetoothProfile.STATE_CONNECTED) {
            queryBaseInfoThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //should disabled mBtnCmdMode first ,determine enable by current work mode
                            mBtnCmdMode.setEnabled(false);
                            mBtnPTRMode.setEnabled(false);
                        }
                    });
                    try {
                        ((MApplication) getApplication()).getService().requestVersion();
                        Thread.sleep(100);
                        ((MApplication) getApplication()).getService().requestSleepDelay();
                        Thread.sleep(100);
                        ((MApplication) getApplication()).getService().requestWorkMode();
                        Thread.sleep(100);
                        ((MApplication) getApplication()).getService().requestDeviceSN();
                        Thread.sleep(100);
                        ((MApplication) getApplication()).getService().requestBusProtocol();
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Log.e(TAG,"queryBaseInfoThread exit");
                        return;
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mTvBoxVersion.setText(((MApplication) getApplication()).mAppData.getVersion());
                            mEtSleepDelay.setHint(String.valueOf(((MApplication) getApplication()).mAppData.getSleepDelay()) + StringUtil.sMinute);
                        }
                    });

                    while(true){
                        if(queryBaseInfoThread == null || queryBaseInfoThread.isInterrupted())
                            break;
                        try {
                            ((MApplication) getApplication()).bleService.requestDTC();
                            Thread.sleep(10000);
                        }catch (InterruptedException e) {
                            e.printStackTrace();
                            Log.e(TAG,"queryDTCThread exit");
                            break;
                        }
                    }
                }
            });
            queryBaseInfoThread.start();
        }
        super.onResume();
    }

    /**
     * @method showAsJ1939
     * @description 显示J1939的设置界面？如果是，将移除OBD的设置
     * Show J1939's setup interface? If so, the OBD settings will be removed
     * @date: 1/8/2020 2:17 PM
     * @author: HJH
     * @param * @Param show:
     * @return * @return: void
     */
    private void showAsJ1939(boolean show){
        if(show){
            mTvHighPrecisionOdometerTitle.setVisibility(View.VISIBLE);
            mTvHighPrecisionOdometer.setVisibility(View.VISIBLE);
            mTvHighPrecisionOdometerUnit.setVisibility(View.VISIBLE);
            mTvOdometerTitle.setVisibility(View.GONE);
            mEtOdometer.setVisibility(View.GONE);
            mBtnOdometer.setVisibility(View.GONE);
            mTvBusProtocolTitle.setVisibility(View.VISIBLE);
            mRBtn_J1587.setVisibility(View.VISIBLE);
            mRBtn_J1939.setVisibility(View.VISIBLE);
            mBtnBusProtocol.setVisibility(View.VISIBLE);
            //adc & gpio 暂时只有OBD有  adc & gpio are temporarily only available to OBD
            mTvAdcAndGpioTitle.setVisibility(View.GONE);
            mBtnEnterAdcAndGpio.setVisibility(View.GONE);
        }else{
            mTvHighPrecisionOdometerTitle.setVisibility(View.GONE);
            mTvHighPrecisionOdometer.setVisibility(View.GONE);
            mTvHighPrecisionOdometerUnit.setVisibility(View.GONE);
            mTvOdometerTitle.setVisibility(View.VISIBLE);
            mEtOdometer.setVisibility(View.VISIBLE);
            mBtnOdometer.setVisibility(View.VISIBLE);
            mTvBusProtocolTitle.setVisibility(View.GONE);
            mRBtn_J1587.setVisibility(View.GONE);
            mRBtn_J1939.setVisibility(View.GONE);
            mBtnBusProtocol.setVisibility(View.GONE);
            //adc & gpio 暂时只有串口版本 OBD有 adc & gpio are temporarily only available to OBD when use serial port
            mTvAdcAndGpioTitle.setVisibility(View.GONE);
            mBtnEnterAdcAndGpio.setVisibility(View.GONE);
        }
    }

    /**
     * saveUri
     */
    void savepath(String value) {
        SharedPreferences.Editor editor = getSharedPreferences(sSharedPreferencesName, MODE_PRIVATE).edit();
        editor.putString(sSharedPreferences_Path, value);
        editor.apply();
    }

    class Flag {
        public static final int SHOW_PROGRESSDIALOG = 904;
        public static final int HIDE_DIALOG = 701;
    }

}
