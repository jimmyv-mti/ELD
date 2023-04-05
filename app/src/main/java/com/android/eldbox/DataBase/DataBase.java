package com.android.eldbox.DataBase;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.room.Database;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.annotation.NonNull;

/**
  *
  * @ProjectName:
  * @Package:        com.android.eldbox.DataBase
  * @ClassName:      DataBase
  * @Description:    ROOM RoomDatabase
  * @Author:         Hgy
  * @UpdateUser:     HJH
  * @UpdateDate:     2019-12-04
  * @UpdateRemark:
 *  2019-12-04 version3 : change all timestamp as long,change enginehours as double
 *  2019-11-28 version2 : add column High_Precision_Odometer
  * @Version:        3.0
 */
@Database(entities = {ELD_DATA.class, StaticMessage.class}, version = 3)
public abstract class DataBase extends RoomDatabase {
    public abstract DAO dao();

    @NonNull
    @Override
    protected SupportSQLiteOpenHelper createOpenHelper(DatabaseConfiguration config) {
        return null;
    }

    @NonNull
    @Override
    protected InvalidationTracker createInvalidationTracker() {
        return null;
    }

    @Override
    public void clearAllTables() {

    }

    /**
     * @method
     * @description 更新原因：协议变更为1.1.3 添加高精度总里程
     * Reason for update: Agreement changed to 1.1.3，add high-precision odometer
     * @date: 11/30/2019 4:25 PM
     * @author: HJH
     */
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            //为旧表添加新的字段 add new column (vlaue < 1099511627775)
            database.execSQL("ALTER TABLE ELD_DATA " + " ADD COLUMN High_Precision_Odemeter INTEGER NOT NULL DEFAULT 0");
            //创建新的数据表 add new table
            //database.execSQL("CREATE TABLE IF NOT EXISTS `book` (`book_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT)");
        }
    };

    /**
     * @method
     * @description 更新原因：协议变更为V1.2.1，更改engine hours 为小数，并纠正High_Precision_Odometer命名
     * Reason for update: Agreement changed to V1.2.1, engine hours changed to decimal
     * @date: 12/4/2019 4:24 PM
     * @author: HJH
     */
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            //重命名数据表 rename old table
            database.execSQL("ALTER TABLE ELD_DATA RENAME TO ELD_DATA_old");
            //创建新的数据表 add new table
            database.execSQL("CREATE TABLE IF NOT EXISTS ELD_DATA ("
            + "`RPM` INTEGER NOT NULL,"
            + "`VSS` INTEGER NOT NULL,"
            + "`TIME_STAMP` INTEGER NOT NULL PRIMARY KEY,"
            + "`Trip_Distance` INTEGER NOT NULL,"
            + "`Odometer` INTEGER NOT NULL,"
            + "`ENGINE_Hours` REAL NOT NULL,"
            + "`High_Precision_Odometer` INTEGER NOT NULL)");
            //先删除再创建索引 delete and create index
            database.execSQL("DROP INDEX IF EXISTS index_ELD_DATA_TIME_STAMP");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_ELD_DATA_TIME_STAMP ON ELD_DATA (TIME_STAMP)");
            //从旧表复制数据到新表 copy old table to new table
            database.execSQL("INSERT INTO ELD_DATA "
                    +"(RPM, VSS,TIME_STAMP,Trip_Distance,Odometer,ENGINE_Hours,High_Precision_Odometer)"
                    +"SELECT `RPM`, `VSS`,`TIME_STAMP`,`Trip_Distance`,`Odometer`,`ENGINE_Hours`,`High_Precision_Odemeter`"
                    +"FROM ELD_DATA_old");
            //删除旧表 delete old table
            database.execSQL("DROP TABLE ELD_DATA_old");

        }
    };
}
