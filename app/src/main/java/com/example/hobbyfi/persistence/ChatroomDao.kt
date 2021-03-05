package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.data.Chatroom
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ChatroomDao : BaseDao<Chatroom>() {
    @Query("SELECT * FROM chatrooms")
    abstract fun getChatrooms(): PagingSource<Int, Chatroom>

    @Query("SELECT * FROM chatrooms WHERE id IN (:chatroomIds)")
    abstract fun getChatroomsByIds(chatroomIds: List<Long>?): PagingSource<Int, Chatroom>

    @Query("SELECT * FROM chatrooms WHERE id NOT IN (:chatroomIds)")
    abstract fun getChatroomsNotPresentInIds(chatroomIds: List<Long>?): PagingSource<Int, Chatroom>

    @Query("DELETE FROM chatrooms")
    abstract suspend fun deleteChatrooms()

    @Query("DELETE FROM chatrooms WHERE id IN (:chatroomIds)")
    abstract suspend fun deleteChatroomsByIds(chatroomIds: List<Long>?)

    @Query("DELETE FROM chatrooms WHERE id NOT IN (:chatroomIds)")
    abstract fun deleteChatroomsNotPresentInIds(chatroomIds: List<Long>?)

    @Query("DELETE FROM chatrooms WHERE id != :chatroomId")
    abstract suspend fun deleteChatroomsExceptId(chatroomId: Long): Int

    @Query("SELECT * FROM chatrooms WHERE ownerId = :ownerId")
    abstract fun getChatroomByOwnerId(ownerId: Long): Flow<Chatroom?>

    @Query("SELECT * FROM chatrooms WHERE id = :id")
    abstract fun getChatroomById(id: Long): Flow<Chatroom?>
}