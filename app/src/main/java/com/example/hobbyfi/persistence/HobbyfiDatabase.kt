package com.example.hobbyfi.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.hobbyfi.models.data.*
import com.example.hobbyfi.shared.RoomConverters

@Database(entities = [Chatroom::class, User::class, Event::class, Message::class, RemoteKeys::class], version = 1, exportSchema = false)
@TypeConverters(RoomConverters::class)
abstract class HobbyfiDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var instance: HobbyfiDatabase? = null

        fun getInstance(context: Context): HobbyfiDatabase {
            synchronized(this) {
                var instance = this.instance
                if(instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext,
                        HobbyfiDatabase::class.java, "hobbify_database.db")
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