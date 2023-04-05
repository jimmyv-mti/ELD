package com.android.eldbox;

import android.app.Application;
import androidx.room.Room;

import com.android.eldbox_api.BleService;
import com.android.eldbox.DataBase.DataBase;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;

import com.android.eldbox_api.SerialAgent;
import com.hjq.toast.ToastUtils;

import static com.android.eldbox.DataBase.DataBase.MIGRATION_1_2;
import static com.android.eldbox.DataBase.DataBase.MIGRATION_2_3;


public class MApplication extends Application {

    //true  Ble ELD
    //false serial ELD
    public static final boolean isBleELD = false;

    //true factory UI
    //false customer UI
    public static final boolean isfactory = false;

    //isBleELD true
    public BleService  bleService;
    //whether bleservice bind
    private Boolean isBleBind = false;

    public Boolean isWaitDataSync = false;

    public Boolean showAsJ1939 = true;

    public SerialAgent serialAgent;

    public DataBase dataBase;

    public AppData mAppData;

    @Override
    public void onCreate() {
        super.onCreate();

        //升级数据库addMigrations(MIGRATION_1_2)
        dataBase = Room.databaseBuilder(getApplicationContext(), DataBase.class, "data")
                .addMigrations(MIGRATION_1_2)
                .addMigrations(MIGRATION_2_3)
                .build();

        if(isBleELD) {
            Intent intent = new Intent();
            intent.setClass(this, BleService.class);
            startService(intent);
            isBleBind = bindService(intent, bleServiceConnection, Context.BIND_AUTO_CREATE);
        }

        mAppData = new AppData();//for get and set app data

        ToastUtils.init(this);
        ToastUtils.setGravity(Gravity.CENTER_HORIZONTAL,0,initScreenHeight()/4);
    }

    /**
     * Gets the parameters of the screen height
     */
    private int initScreenHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager)
                this.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int myScreenHeight = metrics.heightPixels;
        return myScreenHeight;
    }

    //--------------Bluetooth ELD-------------------
    private ServiceConnection bleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (bleService == null) {
                bleService = ((BleService.LocalBinder) service).getService();
                bleService.startAutoConnect();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
        }
    };

    public void unBindAndStopService() {
        bleService.stopAutoConnect();
        if(isBleBind)
            unbindService(bleServiceConnection);
        stopService(new Intent(getApplicationContext(), BleService.class));
    }

    public BleService getService() {
        return bleService;
    }

    //--------------Serial ELD-------------------

    public void disConnectSerial() {
        serialAgent.close();
        serialAgent = null;
    }

    public SerialAgent createSerialAgent() {
        serialAgent = new SerialAgent();
        return serialAgent;
    }
}
