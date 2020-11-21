package com.example.hobbyfi.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.hobbyfi.shared.Constants
import com.google.gson.annotations.SerializedName
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
    @SerializedName(Constants.USERNAME)
    override val name: String,
    val description: String?,
    @SerializedName(Constants.PHOTO_URL)
    val photoUrl: String?,
    val tags: List<Tag>?,
    @SerializedName(Constants.CHATROOM_ID)
    var chatroomId: Int?,
    override val hasImage: Boolean = photoUrl == null,
) : ExpandedModel, Parcelable