package com.android.eldbox.DataBase;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * ROOM Entity for SMB
 */
@Entity(indices = @Index("TIME_STAMP"))
public class StaticMessage {
    @ColumnInfo
    public String VIN                   = "";//VIN
    @ColumnInfo
    public String Engine_Number         = "";//引擎码 Engine number
    @PrimaryKey
    @ColumnInfo
    public long    TIME_STAMP            = 0;//时间戳。做主键用 Timestamp. Use as the primary key

    /*最早的协议里通信协议OBD并不能获取总里程，需要手动设置一个总里程，
     In the earliest agreement, the communication protocol OBD could not get the odometer. You need to manually
     set a odometer.
    */
    @ColumnInfo
    public int Odometer_For_OBD = 0;
}
