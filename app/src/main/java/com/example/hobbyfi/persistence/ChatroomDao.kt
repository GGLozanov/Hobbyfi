package com.example.hobbyfi.persistence

import androidx.paging.PageKeyedDataSource
import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.Chatroom
import javax.sql.DataSource

@Dao
interface ChatroomDao : BaseDao<Chatroom> {
    @Query("SELECT * FROM chatrooms")
    fun getChatrooms(): PagingSource<Int, Chatroom>

    @Query("DELETE FROM chatrooms")
    suspend fun deleteChatrooms()

    @Query("DELETE FROM users WHERE id != :chatroomId")
    fun deleteChatroomsExceptId(chatroomId: Long): Int
}