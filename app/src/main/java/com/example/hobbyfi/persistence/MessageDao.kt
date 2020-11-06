package com.example.hobbyfi.persistence

import androidx.room.Dao
import com.example.hobbyfi.models.Message

@Dao
interface MessageDao : BaseDao<Message> {
}