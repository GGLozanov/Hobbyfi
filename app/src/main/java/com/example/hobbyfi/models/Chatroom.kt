package com.example.hobbyfi.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// TODO: Room embed fields & typeconverters for saving tag lists in chatroom/user entity
@Entity(tableName = "chatrooms", foreignKeys = [
    ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("ownerId"), onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Event::class, parentColumns = arrayOf("id"), childColumns = arrayOf("lastEventId"))
])
data class Chatroom(
    @PrimaryKey
    override val id: Long,
    override val name: String,
    val description: String,
    override val hasImage: Boolean,
    val tags: List<Tag>,
    @ColumnInfo(name = "ownerId", index = true)
    val ownerId: Int,
    @ColumnInfo(name = "lastEventId", index = true)
    val lastEventId: Int
) : ExpandedModel