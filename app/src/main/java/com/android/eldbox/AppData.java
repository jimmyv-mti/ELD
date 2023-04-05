package com.android.eldbox;

// import junit.runner.Version;

import java.util.ArrayList;
import java.util.List;

/**
 * @ProjectName: ELDBOX
 * @Package: com.android.eldbox
 * @ClassName: AppData getter and setter
 * @Description: AppData
 * @Author: HJH
 * @CreateDate: 11/29/2019 1:41 PM
 * @UpdateUser: 更新者：
 * @Version: 1.0
 */
public class AppData {

    private List<byte[]> dtcList = new ArrayList<byte[]>() {
    };

    private String Version = "null";

    private int SleepDelay = -1;

    private int FuelLevel = -1;

    public byte[] getDtcList() {
        if (dtcList.size() == 0)
            return new byte[]{0x6e, 0x3d, 0x30};//n=0
        return dtcList.get(0);
    }

    public void setDtcList(byte[] dtc) {
        this.dtcList.clear();
        this.dtcList.add(dtc);
    }

    private byte[] gpioValueArray = new byte[]{(byte)0xff, (byte)0xff};

    public byte[] getGpioValueArray() {
        return gpioValueArray;
    }

    public void setGpioValueArray(byte[] gpioValueArray) {
        this.gpioValueArray = gpioValueArray;
    }

    /**
     * @method setGpioValueArray
     * @description set bit as 1 or 0
     * @date: 5/8/2020 5:07 PM
     * @author: HJH
     * @param * @Param gpioValue: 1 is high, 0 is low
     * @Param gpioPos: range 0~15,for gpio1~gpio16
     * @return * @return: void
     */
    public void setGpioValueArray(int gpioValue , int gpioPos) {
        if(gpioPos > 7 && gpioPos < 16){
            gpioPos -= 8;
            if(gpioValue == 1)
            {
                gpioValueArray[0] |= 1 << gpioPos;// 将nPos的bit位设置为1，其他位不变
            }else if(gpioValue == 0){
                gpioValueArray[0] &= ~(1 << gpioPos);
            }
        }
        else if(gpioPos >= 0 && gpioPos < 8){
            if(gpioValue == 1)
            {
                gpioValueArray[1] |= 1 << gpioPos;// 将nPos的bit位设置为1，其他位不变
            }else if(gpioValue == 0){
                gpioValueArray[1] &= ~(1 << gpioPos);
            }
        }
    }

    public String getVersion() {
        return Version;
    }

    public void setVersion(String version) {
        Version = version;
    }

    public int getSleepDelay() {
        return SleepDelay;
    }

    public void setSleepDelay(int sleepDelay) {
        SleepDelay = sleepDelay;
    }

    public int getFuelLevel() {
        return FuelLevel;
    }

    public void setFuelLevel(int fuelLevel) {
        FuelLevel = fuelLevel;
    }
}
