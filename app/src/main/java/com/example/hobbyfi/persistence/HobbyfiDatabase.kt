package com.example.hobbyfi.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.hobbyfi.models.*
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.RoomConverters

@Database(entities = [Chatroom::class, User::class, Event::class, Message::class, RemoteKeys::class], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class HobbyfiDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var instance: HobbyfiDatabase? = null

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                // TODO: Finish trigger and have it, well, work
                /*
                val messagePagingTriggerSql =
                    "CREATE TRIGGER IF NOT EXISTS " +
                    "Message_BI_RemoteKeys_Shifter BEFORE INSERT " +
                    "ON RemoteKeys " +
                    "FOR EACH ROW WHEN (NEW.modelType LIKE 'MESSAGE') " +
                        "BEGIN " +
                            "UPDATE RemoteKeys SET nextKey = nextKey + 1, prevKey = IFNULL(prevKey, 1) + 1 " +
                            "WHERE id = (SELECT MAX(id) FROM RemoteKeys HAVING modelType LIKE 'MESSAGE' AND " +
                                "nextKey IN " +
                                    "(SELECT DISTINCT nextKey FROM RemoteKeys " +
                                "WHERE modelType LIKE 'MESSAGE' GROUP BY nextKey HAVING COUNT(nextKey) >= ${Constants.messagesPageSize})" +
                                "GROUP BY id" +
                            "); "  +
                        "END;"

                db.execSQL(messagePagingTriggerSql) */
            }
        }

        fun getInstance(context: Context): HobbyfiDatabase {
            synchronized(this) {
                var instance = this.instance
                if(instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext,
                        HobbyfiDatabase::class.java, "hobbify_database.db")
                        .addCallback(CALLBACK)
                        .build()
                }
                return instance
            }
        }
    }

    abstract fun chatroomDao() : ChatroomDao

    abstract fun eventDao() : EventDao

    abstract fun messageDao() : MessageDao

    abstract fun userDao() : UserDao

    abstract fun remoteKeysDao() : RemoteKeysDao
}