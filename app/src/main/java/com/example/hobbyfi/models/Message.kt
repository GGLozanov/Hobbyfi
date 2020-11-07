package com.example.hobbyfi.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "messages", foreignKeys = arrayOf(
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomSentId"), onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("userSentId"), onDelete = ForeignKey.CASCADE)
))
data class Message(
    @PrimaryKey
    val id: Int,
    @ColumnInfo(name = "message")
    val message: String,
    @ColumnInfo(name = "createTime")
    val createTime: String, // iso string?
    @ColumnInfo(name = "userSentId")
    val userSentId: Int,
    @ColumnInfo(name = "chatroomSentId")
    val chatroomSentId: Int
) : Model