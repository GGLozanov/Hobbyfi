package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.fromJson
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

// can allow foreign keys here since if messages are received, there are ALWAYS chatrooms/users in cache
@Entity(tableName = "messages", foreignKeys = [
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomSentId"), onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("userSentId"), onDelete = ForeignKey.CASCADE)]
)
@Parcelize
@Keep
data class Message(
    @PrimaryKey
    override val id: Long,
    var message: String?,
    @SerializedName(Constants.CREATE_TIME)
    val createTime: String, // iso string?
    @SerializedName(Constants.USER_SENT_ID)
    @ColumnInfo(name = "userSentId", index = true)
    val userSentId: Long?,
    @SerializedName(Constants.CHATROOM_SENT_ID)
    @ColumnInfo(name = "chatroomSentId", index = true)
    val chatroomSentId: Long,
) : Model, Parcelable {
    // is a timeline notification (set to true if received from notification broadcastreceiver)
    val isTimeline: Boolean get() = message == null

    constructor(data: Map<String, String?>) : this((data[Constants.ID] ?: error("Message ID must not be null!")).toLong(),
        data[Constants.MESSAGE],
        data[Constants.CREATE_TIME] ?: error("Message create time must not be null!"),
        (data[Constants.USER_SENT_ID] ?: error("Message user sent ID must not be null!")).toLong(),
        (data[Constants.CHATROOM_SENT_ID] ?: error("Message chatroom sent ID must not be null!")).toLong()
    )

    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Message {
        TODO("Not yet implemented")
    }
}