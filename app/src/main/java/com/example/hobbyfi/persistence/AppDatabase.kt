package com.example.hobbyfi.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.hobbyfi.models.Chatroom
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.User

@Database(entities = [Chatroom::class, User::class, Event::class, Message::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context) : AppDatabase {
            synchronized(this) {
                var instance = this.instance
                if(instance == null) {
                    instance = Room.databaseBuilder(context.applicationContext,
                        AppDatabase::class.java, "hobbify_database.db")
                        .build()
                }
                return instance
            }
        }
    }

    abstract fun chatroomDao() : ChatroomDao

    abstract fun eventDao() : EventDao
}