package com.example.hobbyfi.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

// TODO: Room embed fields & typeconverters for saving tag lists in user entity
@Entity(tableName = "chatrooms", foreignKeys = arrayOf(
    ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("ownerId"), onDelete = ForeignKey.CASCADE)
))
data class Chatroom(
    @PrimaryKey
    val id: Int,

    @ColumnInfo(name = "ownerId")
    val ownerId: Int
) : Model