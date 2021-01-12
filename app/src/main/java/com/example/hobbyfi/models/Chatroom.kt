package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.fromJson
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// TODO: lastEventId change to eventIds List for one-to-many connection
@Entity(tableName = "chatrooms")
@Keep
@Parcelize
data class Chatroom(
    @PrimaryKey
    override val id: Long,
    override var name: String,
    var description: String?,
    @SerializedName(Constants.PHOTO_URL)
    override var photoUrl: String?,
    var tags: List<Tag>?,
    @SerializedName(Constants.OWNER_ID)
    val ownerId: Long,
    @SerializedName(Constants.EVENT_IDS)
    var eventIds: List<Long>?
) : ExpandedModel {
    constructor(data: Map<String, String?>) :
            this((data[Constants.ID] ?: error("Chatroom ID must not be null!")).toLong(),
                data[Constants.NAME] ?: error("Chatroom name must not be null!"),
                data[Constants.DESCRIPTION],
                data[Constants.PHOTO_URL],
                Constants.tagJsonConverter.fromJson(data[Constants.TAGS]),
                (data[Constants.OWNER_ID] ?: error("Chatroom owner ID must not be null!")).toLong(),
                Constants.tagJsonConverter.fromJson(data[Constants.EVENT_IDS])
            )

    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Chatroom {
        for((key, value) in fieldMap.entries) {
            when(key) {
                Constants.NAME -> {
                    name = value!!
                }
                Constants.DESCRIPTION -> {
                    description = value
                }
                Constants.TAGS, Constants.TAGS + "[]" -> {
                    tags = Constants.tagJsonConverter
                        .fromJson(value)
                }
                Constants.IMAGE -> {
                    photoUrl = BuildConfig.BASE_URL + "uploads/" + Constants.chatroomProfileImageDir(id) + "/" + id + ".jpg"
                }
                Constants.EVENT_IDS, Constants.EVENT_IDS + "[]" -> {
                    eventIds = Constants
                        .tagJsonConverter.fromJson(value)
                }
            }
        }
        return this
    }
}