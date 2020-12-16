package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

// TODO: Room embed fields & typeconverters for saving tag lists in chatroom/user entity
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
    @SerializedName(Constants.LAST_EVENT_ID)
    var lastEventId: Int?
) : ExpandedModel, Parcelable {
    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Chatroom {
        for((key, value) in fieldMap.entries) {
            when(key) {
                Constants.NAME -> {
                    name = value!!
                }
                Constants.DESCRIPTION -> {
                    description = value
                }
                Constants.TAGS + "[]" -> {
                    tags = GsonBuilder()
                        .registerTypeAdapter(
                            Tag::class.java,
                            TagTypeAdapter()
                        ) // FIXME: Extract into singleton
                        .create().fromJson(value!!)
                }
                Constants.PHOTO_URL -> {
                    photoUrl = value
                }
                Constants.LAST_EVENT_ID -> {
                    val eventId = value!!.toInt()
                    this.lastEventId = if(eventId.compareTo(0) == 0) null else eventId
                }
            }
        }
        return this
    }
}