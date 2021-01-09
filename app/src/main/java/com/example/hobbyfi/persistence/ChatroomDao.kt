package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.Chatroom
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChatroomDao : BaseDao<Chatroom>() {
    @Query("SELECT * FROM chatrooms WHERE id IN (:chatroomIds)")
    abstract fun getChatroomsByIds(chatroomIds: List<Long>?): PagingSource<Int, Chatroom>

    abstract fun getChatroomsNotPresentInIds(chatroomIds: List<Long>?): PagingSource<Int, Chatroom>

    @Query("DELETE FROM chatrooms")
    abstract suspend fun deleteChatrooms()

    @Query("DELETE FROM chatrooms WHERE id != :chatroomId")
    abstract suspend fun deleteChatroomsExceptId(chatroomId: Long): Int

    @Query("SELECT * FROM chatrooms WHERE ownerId = :ownerId")
    abstract fun getChatroomByOwnerId(ownerId: Long): Flow<Chatroom?>
}