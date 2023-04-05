package com.android.eldbox.tools;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils
{
    public static String LogFilePath = "sdcard/obd_log.txt";

    public static String createLogFile()
    {
        File file = new File("sdcard/obd_log.txt");
        if (!file.exists())
        {
            try
            {
                file.createNewFile();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return file.getPath();
    }

    public static void writeToLogFile(String value)
    {
        File LogFile = new File(LogFilePath);
        if (!LogFile.exists())
        { //Create if it isn't exist
            try
            {
                LogFile.createNewFile();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        try
        {
            FileOutputStream fos = new FileOutputStream(LogFile, true);
            value = value + "                       " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(System.currentTimeMillis())) + "\r\n";
            fos.write(value.getBytes());
            fos.close();
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * 获取版本号名称
     *
     * @param context 上下文
     * @return
     */
    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

}
