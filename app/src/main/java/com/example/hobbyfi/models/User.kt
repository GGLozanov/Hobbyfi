package com.example.hobbyfi.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "users", foreignKeys = arrayOf(
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomId"))
))
@Parcelize
data class User(
    @PrimaryKey
    override val id: Int,
    override val name: String,
    override val description: String,
    override val hasImage: Boolean,
    val email: String,
    val tags: List<Tag>,
    var chatroomId: Int?
) : ExpandedModel, Parcelable