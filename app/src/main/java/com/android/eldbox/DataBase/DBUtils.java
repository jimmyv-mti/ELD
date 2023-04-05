package com.android.eldbox.DataBase;

import android.app.Application;

import com.android.eldbox.MApplication;

import java.util.List;

/**     
  * 
  * @ProjectName:
  * @Package:        com.android.eldbox.DataBase
  * @ClassName:      DBUtils
  * @Description:    DBUtils to use ROOM Database
  * @Author:         HGY
  * @UpdateUser:     HJH
  * @UpdateRemark:
 *  V2.1 2019-12-20 更新说明：add deleteEldData
 *  V2.0 2019-11-28 更新说明：add HighPrecisionOdometer
  * @Version:        2.1
 */
public class DBUtils {

    /* 如果删除ELD Data旧数据，需要等待删除完成，才开始记录数据。防止第一条数据不完整。
       If you delete the old ELD Data, you need to wait for the deletion to complete before recording the Data.Prevent the first data from being incomplete.

    ELD设备传输数据的顺序如下：ELD devices transmit data in the following order:
    RPM，转速
    VSS，车速
    Trip distance，短里程
    odometer, 总里程
    VIN，车架号
    Engine Hours，引擎时间
    HighPrecisionOdometer 高精度总里程
     */
    private static boolean isDeleting = false;

    // SMB = static message book，
    // 不需要每秒更新的数据
    // No need to update data per second
    // 对单一列的insert操作，都需要检查该列是否存在，如果不存在则要创建这一个数据段，插入整个数据
    // For a single column insert operation, you need to check whether the column exists. If it does not exist, you need to create this data segment and insert the entire data.

    /**
     * @method insert
     * @description ELD数据 写入数据库  ELD data is written to the database
     * @author: HJH
     * @param * @Param application: 
     * @Param data: ELD数据  ELD data
     * @return * @return: void
     */
    public static void insert(final Application application, final ELD_DATA data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if(isDeleting)
                        return;
                    getDao(application).insertData(data);
                }
            }
        }).start();
    }

    /**
     * @method deleteEldData
     * @description 删除某个时间段的旧数据 Delete old data for a certain period of time
     * @author: HJH
     * @param * @Param application:
     * @Param start: 起始UTC时间   Start UTC time
     * @Param end: 结束UTC时间     End UTC Time
     * @return * @return: void
     */
    public static void deleteEldData(final Application application, final long start, final long end) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    isDeleting = true;
                    getDao(application).deleteEldDATAByTimeStamp(start,end);
                }
            }
        }).start();
    }

    /**
     * @method deleteEldData
     * @description 删除App存储的ELD数据 Delete ELD data stored by the app
     * @author: HJH
     * @param * @Param application:
     * @return * @return: void
     */
    public static void deleteEldData(final Application application) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    isDeleting = true;
                    getDao(application).deleteEldDATA();
                }
            }
        }).start();
    }

    /**
     * @method insertRPM
     * @description RPM转数 写入数据库 RPM revolutions write to the database
     * @author: HJH
     * @param * @Param application: 
     * @Param rpm: RPM转速 RPM speed
     * @Param timeStamp: 时间戳 Timestamp
     * @return * @return: void
     */
    public static void insertRPM(final Application application, final int rpm, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    isDeleting = false;
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        ELD_DATA data = new ELD_DATA();
                        data.RPM = rpm;
                        data.TIME_STAMP = timeStamp;
                        getDao(application).insertData(data);
                    } else {
                        getDao(application).updateRPMByTimeStamp(rpm, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insertVSS
     * @description VSS车速 写入数据库 vehicle speed write to the database
     * @author: HJH
     * @param * @Param application: 
     * @Param vss: VSS车速 VSS speed
     * @Param timeStamp: 时间戳 Timestamp
     * @return * @return: void
     */
    public static void insertVSS(final Application application, final int vss, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if(isDeleting)
                        return;
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        ELD_DATA data = new ELD_DATA();
                        data.VSS = vss;
                        data.TIME_STAMP = timeStamp;
                        getDao(application).insertData(data);
                    } else {
                        getDao(application).updateVSSByTimeStamp(vss, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insertTripDistance
     * @description 短里程 写入数据库 trip distance is written to the database
     * @author: HJH
     * @param * @Param application: 
     * @Param tripDistance: 短里程 distance
     * @Param timeStamp: 时间戳 Timestamp
     * @return * @return: void
     */
    public static void insertTripDistance(final Application application, final int distance, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if(isDeleting)
                        return;
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        ELD_DATA data = new ELD_DATA();
                        data.Trip_Distance = distance;
                        data.TIME_STAMP = timeStamp;
                        getDao(application).insertData(data);
                    } else {
                        getDao(application).updateTripDistanceByTimeStamp(distance, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insertOdometer
     * @description 总里程 写入数据库 Odometer written to the database
     * @author: HJH
     * @param * @Param application: 
     * @Param total: 总里程  Odometer
     * @Param timeStamp: 时间戳 Timestamp
     * @return * @return: void
     */
    public static void insertOdometer(final Application application, final int total, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if(isDeleting)
                        return;
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        ELD_DATA data = new ELD_DATA();
                        data.Odometer = total;
                        data.TIME_STAMP = timeStamp;
                        getDao(application).insertData(data);
                    } else {
                        getDao(application).updateOdometerByTimeStamp(total, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insertHighPrecisionOdometer
     * @description 高精度总里程 写入数据库 HighPrecisionOdometer written to the database
     * @author: HJH
     * @param * @Param application:
     * @Param total: 高精度总里程  HighPrecisionOdometer
     * @Param timeStamp: 时间戳 Timestamp
     * @return * @return: void
     */
    public static void insertHighPrecisionOdometer(final Application application, final long total, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if(isDeleting)
                        return;
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        ELD_DATA data = new ELD_DATA();
                        data.High_Precision_Odometer = total;
                        data.TIME_STAMP = timeStamp;
                        getDao(application).insertData(data);
                    } else {
                        getDao(application).updateHighPrecisionOdometerByTimeStamp(total, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insertEngineHours
     * @description 引擎启动时间 写入数据库 Engine Hours write to database
     * @author: HJH
     * @param * @Param application:
     * @Param time:引擎启动时间  Engine hours
     * @Param timeStamp:时间戳  Timestamp
     * @return * @return: void
     */
    public static void insertEngineHours(final Application application, final double time, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if(isDeleting)
                        return;
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        ELD_DATA data = new ELD_DATA();
                        data.ENGINE_Hours = time;
                        data.TIME_STAMP = timeStamp;
                        getDao(application).insertData(data);
                    } else {
                        getDao(application).updateEngineHoursByTimeStamp(time, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insert_VIN_inSMB
     * @description VIN码 写入数据库  VIN code is written to the database
     * @author: HJH
     * @param * @Param application:
     * @Param vin: Vin 码  VIN code
     * @Param timeStamp: 时间戳 Timestamp
     * @return * @return: void
     */
    public static void insert_VIN_inSMB(final Application application, final String vin, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        StaticMessage mMessage = new StaticMessage();
                        mMessage.VIN = vin;
                        mMessage.TIME_STAMP = timeStamp;
                        getDao(application).insertToStaticMessageBook(mMessage);
                    } else {
                        getDao(application).update_VINByID_inSMB(vin, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insert_OdometerForOBD_inSMB
     * @description 总里程 写入数据库（Settings手动写入，OBD适用） Odometer written to the database (manual write by user in Settings, OBD applies)
     * @author: HJH
     * @param * @Param application:
     * @Param total:总里程（OBD） Odometer (OBD)
     * @Param timeStamp:时间戳 Timestamp
     * @return * @return: void
     */
    public static void insert_OdometerForOBD_inSMB(final Application application, final int total, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        StaticMessage mMessage = new StaticMessage();
                        mMessage.Odometer_For_OBD = total;
                        mMessage.TIME_STAMP = timeStamp;
                        getDao(application).insertToStaticMessageBook(mMessage);
                    } else {
                        getDao(application).update_OdometerForOBDByID_inSMB(total, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method insert_EngineNumber_inSMB
     * @description 引擎码 写入数据库 Engine Number write to database
     * @author: HJH
     * @param * @Param application:
     * @Param number: 引擎码 Engine number
     * @Param timeStamp: 时间戳 Timestamp
     * @return * @return: void
     */
    public static void insert_EngineNumber_inSMB(final Application application, final String number, final long timeStamp) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if (getDao(application).getCountByTimeStamp(timeStamp) == 0) {
                        StaticMessage mMessage = new StaticMessage();
                        mMessage.Engine_Number = number;
                        mMessage.TIME_STAMP = timeStamp;
                        getDao(application).insertToStaticMessageBook(mMessage);
                    } else {
                        getDao(application).update_EngineByID_inSMB(number, timeStamp);
                    }
                }
            }
        }).start();
    }

    /**
     * @method queryLastOneData
     * @description 查询上个数据 Query last data
     * @author: HJH
     * @param * @Param application:
     * @Param onGetLastOneListener:
     * @return * @return: void
     */
    public static void queryLastOneData(final Application application, final OnGetLastOneListener onGetLastOneListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if (onGetLastOneListener != null) {
                        onGetLastOneListener.onGetLastOne(getDao(application).getLastOne());
                    }
                }
            }
        }).start();
    }

    /**
     * @method queryLastOneData_inSMB
     * @description 从SMB数据库查询上个数据 Query the last data from the SMB database
     * @author: HJH
     * @param * @Param application:
     * @Param onGetSMBLastOneListener:
     * @return * @return: void
     */
    public static void queryLastOneData_inSMB(final Application application, final OnGetSMBLastOneListener onGetSMBLastOneListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    if (onGetSMBLastOneListener != null) {
                        onGetSMBLastOneListener.onGetLastOne(getDao(application).getLastOneFromSMB());
                    }
                }
            }
        }).start();
    }

    /**
     * @method update_inSMB
     * @description 更新SMB数据库 Update SMB database
     * @author: HJH
     * @param * @Param application:
     * @Param messages:
     * @return * @return: void
     */
    public static void update_inSMB(final Application application, final StaticMessage messages) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (getDao(application)) {
                    getDao(application).update_inSMB(messages);
                }
            }
        }).start();
    }

    private static MApplication castToApplication(Application application) {
        return (MApplication) application;
    }

    private static DAO getDao(Application application) {
        return castToApplication(application).dataBase.dao();
    }


    public interface OnGetLastOneListener {
        void onGetLastOne(List<ELD_DATA> list);
    }

    public interface OnGetSMBLastOneListener {
        void onGetLastOne(List<StaticMessage> list);
    }

}
