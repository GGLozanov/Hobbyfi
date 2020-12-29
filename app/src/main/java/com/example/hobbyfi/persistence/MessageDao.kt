package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.Message

@Dao
interface MessageDao : BaseDao<Message> {
    @Query("SELECT * FROM messages")
    fun getMessages(): PagingSource<Int, Message>

    @Query("DELETE FROM messages")
    fun deleteMessages(): Int

    @Query("DELETE FROM messages WHERE id = :id")
    fun deleteMessageById(id: Long): Int

    @Query("UPDATE messages SET message = :message WHERE id = :id")
    fun updateMessageById(id: Long, message: String)
}