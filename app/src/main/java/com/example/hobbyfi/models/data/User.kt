package com.example.hobbyfi.models.data

import androidx.annotation.Keep
import androidx.room.*
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.fromJson
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// TODO: chatroomId change to eventIds List for one-to-many connection
@Entity(tableName = "users")
@Keep
@Parcelize
data class User(
    @PrimaryKey
    override val id: Long,
    var email: String?,
    @SerializedName(Constants.USERNAME)
    override var name: String,
    var description: String?,
    @SerializedName(Constants.PHOTO_URL)
    override var photoUrl: String?,
    var tags: List<Tag>?,
    @SerializedName(Constants.CHATROOM_IDS)
    var chatroomIds: List<Long>?,
    @SerializedName(Constants.ALLOWED_PUSH_CHATROOM_IDS)
    var allowedPushChatroomIds: List<Long>?
) : ExpandedModel {
    constructor(data: Map<String, String?>) : this(
        (data[Constants.ID] ?: error("User ID must not be null!")).toLong(),
        data[Constants.EMAIL],
        data[Constants.USERNAME] ?: error("User username must not be null!"),
        data[Constants.DESCRIPTION],
        data[Constants.PHOTO_URL],
        Constants.jsonConverter.fromJson(data[Constants.TAGS]),
        Constants.jsonConverter.fromJson(data[Constants.CHATROOM_IDS]),
        Constants.jsonConverter.fromJson(data[Constants.ALLOWED_PUSH_CHATROOM_IDS])
    )

    override fun updateFromFieldMap(fieldMap: Map<String, String?>): User {
        for((key, value) in fieldMap.entries) {
            when(key) {
                Constants.EMAIL -> {
                    email = value
                }
                Constants.USERNAME -> {
                    name = value!!
                }
                Constants.DESCRIPTION -> {
                    description = value
                }
                Constants.TAGS, Constants.TAGS + "[]" -> {
                    tags = Constants.jsonConverter
                        .fromJson(value!!)
                }
                Constants.IMAGE -> {
                    photoUrl = BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir + "/" + id + ".jpg"
                        // no need to update it generally because it's always the same but we need to wake up observer and reload it?
                }
                Constants.CHATROOM_IDS, Constants.CHATROOM_IDS + "[]" -> {
                    chatroomIds = Constants.jsonConverter
                        .fromJson(value!!)
                }
                Constants.ALLOWED_PUSH_CHATROOM_IDS, Constants.ALLOWED_PUSH_CHATROOM_IDS + "[]" -> {
                    allowedPushChatroomIds = Constants.jsonConverter
                        .fromJson(value!!)
                }
            }
        }
        return this
    }
}