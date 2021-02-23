package com.example.hobbyfi.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.shared.RemoteKeyType

@Dao
abstract class MessageDao : BaseDao<Message>() {
    @Query("SELECT * FROM messages ORDER BY createTime DESC")
    abstract fun getMessages(): PagingSource<Int, Message>

    @Query("SELECT id FROM messages WHERE chatroomSentId = :chatroomSentId")
    abstract fun getMessagesIdsByChatroomId(chatroomSentId: Long): List<Long>

    @Query("SELECT * FROM messages WHERE chatroomSentId = :chatroomSentId ORDER BY createTime DESC")
    abstract fun getMessagesByChatroomId(chatroomSentId: Long): PagingSource<Int, Message>

    @Query("SELECT id FROM messages WHERE chatroomSentId = :chatroomSentId AND id IN (:ids) ORDER BY createTime DESC")
    abstract fun getMessagesIdsByChatroomIdAndIds(chatroomSentId: Long, ids: List<Long>): List<Long>

    @Query("SELECT * FROM messages WHERE chatroomSentId = :chatroomSentId AND id IN (:ids) ORDER BY createTime DESC")
    abstract fun getMessagesByChatroomIdAndIds(chatroomSentId: Long, ids: List<Long>): PagingSource<Int, Message>

    @Query("SELECT * FROM messages msgs INNER JOIN remoteKeys rmkeys ON rmKeys.id = msgs.id AND rmKeys.modelType = :remoteKeyType WHERE msgs.chatroomSentId = :chatroomSentId ORDER BY msgs.createTime DESC")
    abstract fun getMessagesByChatroomIdAndRemoteKeyType(chatroomSentId: Long, remoteKeyType: RemoteKeyType): PagingSource<Int, Message>

    @Query("DELETE FROM messages")
    abstract fun deleteMessages(): Int

    @Query("DELETE FROM messages WHERE chatroomSentId = :chatroomSentId")
    abstract fun deleteMessagesByChatroomId(chatroomSentId: Long): Int

    @Query("DELETE FROM messages WHERE id IN (:ids)")
    abstract fun deleteMessagesByIds(ids: List<Long>): Int

    @Query("DELETE FROM messages WHERE id = :id")
    abstract fun deleteMessageById(id: Long): Int

    @Query("UPDATE messages SET message = :message WHERE id = :id")
    abstract fun updateMessageById(id: Long, message: String)
}