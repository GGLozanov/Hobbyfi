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
    override val id: Int,
    override val name: String,
    override val description: String,
    override val hasImage: Boolean,
    val email: String,
    val tags: List<Tag>,
    @ColumnInfo(name = "chatroomId")
    var chatroomId: Int?
) : ExpandedModel