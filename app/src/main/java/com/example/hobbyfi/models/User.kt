package com.example.hobbyfi.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "users", foreignKeys = arrayOf(
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomId"))
))
data class User(
    @PrimaryKey
    val id: Int,

    @ColumnInfo(name = "chatroomId")
    val chatroomId: Int?
) : Model