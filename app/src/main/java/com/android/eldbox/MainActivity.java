package com.android.eldbox;

import android.content.Intent;
import android.os.Bundle;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.eldbox.factory.WriteSNActivity;
import com.android.eldbox.ble_ui.BleUIMainActivity;
import com.android.eldbox.serial_ui.SerialUIMainActivity;



/**
 * @ProjectName: ELDBOX
 * @Package: com.android.eld
 * @ClassName: MainActivity
 * @Description:  主要入口 main entrance
 * @Author: HJH
 * @UpdateUser: HJH
 * @UpdateDate: 2019-11-24
 * @UpdateRemark: V2.0 new Factory UI Intent
 * @Version: 2.0
 */
public class MainActivity extends AppCompatActivity {

    Intent mBleUIIntent = null;
    Intent mSerialUIIntent = null;

    Intent mGPScode = null;
    Intent mFactoryUIIntent = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(com.android.eldbox.MApplication.isBleELD) {
            //跳转到 蓝牙ELD界面 Jump to the Bluetooth ELD interface
            mBleUIIntent = new Intent(MainActivity.this, BleUIMainActivity.class);
            startActivity(mBleUIIntent);
        }else {
            if(MApplication.isfactory)
            {
                //跳转到 工厂写入设置界面 Jump to the factory write SN interface
                mFactoryUIIntent = new Intent(MainActivity.this, WriteSNActivity.class);
                startActivity(mFactoryUIIntent);
                MainActivity.this.finish();
                return;
            }
            //跳转到 串口ELD界面 Jump to the serial ELD interface
            mSerialUIIntent = new Intent(MainActivity.this, SerialUIMainActivity.class);
            startActivity(mSerialUIIntent);


        }

        MainActivity.this.finish();
    }
}
