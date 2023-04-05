package com.android.eldbox.serial_ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.eldbox.MApplication;
import com.android.eldbox.R;

import java.io.File;

import com.android.eldbox_api.EldboxOTACallBack;
import com.android.eldbox_api.SerialOTAHelper;

/**
  *
  * @ProjectName:
  * @Package:        com.android.eldbox
  * @ClassName:      SerialOTAActivity
  * @Description:    通过OTA 升级单片机界面  Interface for Upgrade the MCU firmware through OTA
  * @Author:         HGY
  * @UpdateUser:     HJH
  * @UpdateDate:
  * @UpdateRemark:
  * @Version:        1.0
 */
public class SerialOTAActivity extends AppCompatActivity{

    private Button      mBtnStartOta;
    private Button      mBtnSelectOTAFile;

    private TextView    mTvDeviceNameTitle;
    private TextView    mTvDeviceName;
    private TextView    mTvOTAVersion;
    private TextView    mTvOTAStatus;
    private TextView    mTvOTAProgress;

    private ProgressBar mPbOTA;

    private File        otaFile;

    private static final String sZero_Percent = "0%";
    private static final String sHundred_Percent = "100%";
    private static final String sPercent = "%";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);
        init();
    }

    private void init() {
        mBtnStartOta = findViewById(R.id.btn_start_ota);
        mBtnSelectOTAFile = findViewById(R.id.btn_select_ota_file);
        mTvDeviceNameTitle = findViewById(R.id.tv_device_name_title);
        mTvDeviceName = findViewById(R.id.tv_device_name);
        mTvOTAVersion = findViewById(R.id.tv_ota_version);
        mTvOTAStatus = findViewById(R.id.tv_ota_status);
        mTvOTAProgress = findViewById(R.id.tv_ota_progress);
        mPbOTA = findViewById(R.id.pb_ota);
        mBtnStartOta.setOnClickListener(new mOTAClick());
        mBtnSelectOTAFile.setOnClickListener(new mOTAClick());

        mTvDeviceNameTitle.setVisibility(View.GONE);
        mTvDeviceName.setVisibility(View.GONE);
    }

    /**
     * @interface
     * @description 按键监听接口 Button click implement interface
     * @author: HJH
     * @param * @Param null:
     * @return * @return: null
     */
    class mOTAClick implements View.OnClickListener{
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_ota: {
                    if (SerialOTAHelper.getInstance().initialize(((MApplication) getApplication()).serialAgent, otaFile, mEldboxOTACallBack)) {
                    	SerialOTAHelper.getInstance().startOTA();
                    }
                    break;
                }
                case R.id.btn_select_ota_file: {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/bin");
                    startActivityForResult(intent, 101);
                    break;
                }
            }
        }
    }

    /**
     * @description 回调函数 CallBack
     * @author: HJH
     */
    private EldboxOTACallBack mEldboxOTACallBack = new EldboxOTACallBack() {
        @Override
        public void onReadyOTA() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvOTAStatus.setText(getResources().getStringArray(R.array.ota_status)[0]);
                    mTvOTAProgress.setText(sZero_Percent);
                    mPbOTA.setProgress(0);
                }
            });
        }

        @Override
        public void onOTA(final int i) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvOTAProgress.setText(String.valueOf(i) + sPercent);
                    mPbOTA.setProgress(i);
                    mTvOTAStatus.setText(getResources().getStringArray(R.array.ota_status)[1]);
                }
            });
        }

        @Override
        public void onOTAFinish() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvOTAStatus.setText(getResources().getStringArray(R.array.ota_status)[2]);
                    mTvOTAProgress.setText(sHundred_Percent);
                }
            });
        }

        @Override
        public void onOTAStop(int i) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvOTAStatus.setText(getResources().getStringArray(R.array.ota_status)[3]);
                }
            });
        }
    };



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 101) {
            if (data != null && data.getData() != null) {
                Uri doc = data.getData();
                otaFile = new File(doc.getPath());
            }
            if (otaFile == null) {
                mTvOTAVersion.setText(getString(R.string.ota_file_no_found));
            } else {
                mTvOTAVersion.setText(otaFile.getName());
            }
        }
    }

}
