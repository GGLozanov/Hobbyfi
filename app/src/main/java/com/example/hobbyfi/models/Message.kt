package com.example.hobbyfi.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "messages", foreignKeys = [
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomSentId"), onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("userSentId"), onDelete = ForeignKey.CASCADE)]
)
data class Message(
    @PrimaryKey
    override val id: Long,
    val message: String,
    val createTime: String, // iso string?
    @ColumnInfo(name = "userSentId", index = true)
    val userSentId: Long?,
    @ColumnInfo(name = "chatroomSentId", index = true)
    val chatroomSentId: Long,
    val isTimeline: Boolean = false // is a timeline notification (set to true if received from notification broadcastreceiver)
) : Model {
    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Message {
        TODO("Not yet implemented")
    }
}