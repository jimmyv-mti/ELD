package com.android.eldbox.DataBase;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
  *
  * @ProjectName:
  * @Package:        com.android.eldbox.DataBase
  * @ClassName:      DAO
  * @Description:    ROOM DAO
  * @Author:         HGY
  * @UpdateUser:     HJH
  * @UpdateDate:     2019-11-28
  * @UpdateRemark:   2019-11-28 add updateHighPrecisionOdometerByTimeStamp
  * @Version:        2.0
 */
@Dao
public interface DAO {

    /*
     * ELD＿DATA  盒子数据
     */

    //-------------------update---------------
    @Update
    void update(ELD_DATA... data);

    //-------------------query---------------
    @Query("UPDATE ELD_DATA SET RPM=:rpm WHERE TIME_STAMP=:timeStamp")
    void updateRPMByTimeStamp(int rpm, long timeStamp);

    @Query("UPDATE ELD_DATA SET VSS=:Vss WHERE TIME_STAMP=:timeStamp")
    void updateVSSByTimeStamp(int Vss, long timeStamp);

    @Query("UPDATE ELD_DATA SET Trip_Distance=:distance WHERE TIME_STAMP=:timeStamp")
    void updateTripDistanceByTimeStamp(int distance, long timeStamp);

    @Query("UPDATE ELD_DATA SET Odometer=:odometer WHERE TIME_STAMP=:timeStamp")
    void updateOdometerByTimeStamp(int odometer, long timeStamp);

    @Query("UPDATE ELD_DATA SET ENGINE_Hours=:engine WHERE TIME_STAMP=:timeStamp")
    void updateEngineHoursByTimeStamp(double engine, long timeStamp);

    @Query("UPDATE ELD_DATA SET High_Precision_Odometer=:highPrecisionOdometer WHERE TIME_STAMP=:timeStamp")
    void updateHighPrecisionOdometerByTimeStamp(long highPrecisionOdometer, long timeStamp);

    @Query("SELECT * FROM ELD_DATA ORDER BY TIME_STAMP DESC LIMIT 1")
    List<ELD_DATA> getLastOne();

    @Query("SELECT * FROM ELD_DATA WHERE TIME_STAMP=:timeStamp")
    List<ELD_DATA> getByTimeStamp(long timeStamp);

    //从数据库分页获取数据 Get data from database paging
    @Query("SELECT * FROM ELD_DATA LIMIT :count OFFSET :offset")
    List<ELD_DATA> getDataFromXToY(int offset, int count);

    // 删除从某一时间段的数据，Unix时间戳，单位为ms
    // Delete data from a certain time period, Unix timestamp, unit millsecond
    @Query("DELETE FROM ELD_DATA WHERE TIME_STAMP BETWEEN :start AND :end")
    void deleteEldDATAByTimeStamp(long start, long end);

    // 删除App存储的ELD数据 Delete ELD data stored by the app
    @Query("DELETE FROM ELD_DATA")
    void deleteEldDATA();

    @Query("SELECT COUNT(*) FROM ELD_DATA")
    int getCount();

    @Query("SELECT COUNT(TIME_STAMP) FROM ELD_DATA WHERE TIME_STAMP=:timeStamp")
    int getCountByTimeStamp(long timeStamp);

    //-------------------insert---------------
    //冲突时做替换。 Doing replacement when conflicts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertData(ELD_DATA... data);



    /*
     *SMB = static message book，
     * 不需要每秒更新的数据
     * No need to update data per second
     */

    //-------------------update---------------
    @Update
    void update_inSMB(StaticMessage... messages);

    //-------------------query---------------
    //SMB指StaticMessageBook   SMB refers to StaticMessageBook
    @Query("UPDATE StaticMessage SET VIN=:vin WHERE TIME_STAMP=:timeStamp")
    void update_VINByID_inSMB(String vin, long timeStamp);

    @Query("UPDATE StaticMessage SET Engine_Number=:Engine WHERE TIME_STAMP=:timeStamp")
    void update_EngineByID_inSMB(String Engine, long timeStamp);

    //指的是手动输入的总里程 Refers to the odometer manually entered
    @Query("UPDATE StaticMessage SET Odometer_For_OBD=:offset WHERE TIME_STAMP=:timeStamp")
    void update_OdometerForOBDByID_inSMB(int offset, long timeStamp);

    //静态数据一般不多，不需要分页查找 Static data is generally simple, no need to find pages
    @Query("SELECT * FROM StaticMessage")
    List<StaticMessage> getALLFormSMB();

    //从数据库分页获取数据 Get data from database paging
    @Query("SELECT * FROM StaticMessage LIMIT :count OFFSET :offset")
    List<StaticMessage> getDataFromSMBXToY(int offset, int count);

    @Query("DELETE FROM StaticMessage WHERE TIME_STAMP BETWEEN :start AND :end")
    void deleteFromSMBByTimeStamp(int start, int end);

    @Query("SELECT COUNT(*) FROM StaticMessage")
    int getSMBCount();

    @Query("SELECT * FROM StaticMessage ORDER BY TIME_STAMP DESC LIMIT 1")
    List<StaticMessage> getLastOneFromSMB();

    //-------------------insert---------------
    //冲突时做替换。 Doing replacement when conflicts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertToStaticMessageBook(StaticMessage... staticMessages);

}
