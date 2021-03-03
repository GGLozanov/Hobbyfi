package com.example.hobbyfi.models.data

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.hobbyfi.shared.Constants
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// can allow foreign keys here since if messages are received, there are ALWAYS chatrooms/users in cache
@Entity(tableName = "messages", foreignKeys = [
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomSentId"), onDelete = ForeignKey.CASCADE)
])
@Parcelize
@Keep
data class Message(
    @PrimaryKey
    override val id: Long,
    var message: String,
    @SerializedName(Constants.CREATE_TIME)
    val createTime: String, // iso string?
    @SerializedName(Constants.USER_SENT_ID)
    val userSentId: Long?,
    @SerializedName(Constants.CHATROOM_SENT_ID)
    @ColumnInfo(name = "chatroomSentId", index = true)
    val chatroomSentId: Long,
) : Model {
    val isTimeline: Boolean get() = userSentId == null

    constructor(data: Map<String, String?>) : this((data[Constants.ID] ?: error("Message ID must not be null!")).toLong(),
        data[Constants.MESSAGE] ?: error("Message message must not be null!"),
        data[Constants.CREATE_TIME] ?: error("Message create time must not be null!"),
        data[Constants.USER_SENT_ID]?.toLong(),
        (data[Constants.CHATROOM_SENT_ID] ?: error("Message chatroom sent ID must not be null!")).toLong(),
    )

    override fun updateFromFieldMap(fieldMap: Map<String, String?>): Message {
        for((key, value) in fieldMap.entries) {
            when(key) {
                Constants.MESSAGE -> {
                    this.message = value!!
                }
            }
        }
        return this
    }
}