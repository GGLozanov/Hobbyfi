package com.example.hobbyfi.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// TODO: Room embed fields & typeconverters for saving tag lists in chatroom/user entity
@Entity(tableName = "chatrooms", foreignKeys = [
    ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("ownerId"), onDelete = ForeignKey.CASCADE)
])
data class Chatroom(
    @PrimaryKey
    override val id: Int,
    override val name: String,
    override val description: String,
    override val hasImage: Boolean,
    val tags: List<Tag>,
    val ownerId: Int
) : ExpandedModel