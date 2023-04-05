package com.android.eldbox.DataBase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**     
  * 
  * @ProjectName:    
  * @Package:        com.android.eldbox.DataBase
  * @ClassName:      ELD_DATA.java
  * @Description:    ROOM　Entity for ELD Data
  * @Author:         HGY
  * @UpdateUser:     HJH
  * @UpdateDate:     2019-11-28
  * @UpdateRemark:   2019-11-28 add HighPrecisionOdometer
  * @Version:        2.0
 */
@Entity(indices = @Index("TIME_STAMP"))
public class ELD_DATA {
    @ColumnInfo
    public int    RPM                     = 0;//转速 Rotating speed
    @ColumnInfo
    public int    VSS                     = 0;//车速 Vehicle Speed
    @PrimaryKey
    @ColumnInfo
    public long   TIME_STAMP              = 0;//时间戳。做主键用 Timestamp. Use as the primary key
    @ColumnInfo
    public int    Trip_Distance           = 0;//短里程 Trip distance
    @ColumnInfo
    public int    Odometer                = 0;//总里程 odometer
    @ColumnInfo(typeAffinity = ColumnInfo.REAL)
    public double ENGINE_Hours            = 0.00;//引擎工作时间 Engine hours
    @ColumnInfo(typeAffinity = ColumnInfo.INTEGER)
    public long   High_Precision_Odometer = 0;//高精度总里程，仅1939可用 HighPrecisionOdometer，only J1939
}
