package com.example.hobbyfi.persistence

import androidx.room.Dao
import com.example.hobbyfi.models.Chatroom

@Dao
interface ChatroomDao : BaseDao<Chatroom> {

}