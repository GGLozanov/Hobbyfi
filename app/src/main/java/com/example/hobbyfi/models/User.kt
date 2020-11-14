package com.example.hobbyfi.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "users", foreignKeys = [
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomId"))
])
@Keep
@Parcelize
data class User(
    @PrimaryKey
    override val id: Long,
    val email: String?,
    override val name: String,
    val description: String?,
    override val hasImage: Boolean,
    val tags: List<Tag>?,
    var chatroomId: Int?
) : ExpandedModel, Parcelable