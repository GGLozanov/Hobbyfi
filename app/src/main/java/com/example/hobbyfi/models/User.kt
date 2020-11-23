package com.example.hobbyfi.models

import android.os.Parcel
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

@Entity(tableName = "users", foreignKeys = [
    ForeignKey(entity = Chatroom::class, parentColumns = arrayOf("id"), childColumns = arrayOf("chatroomId"))
])
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
    var photoUrl: String?,
    var tags: List<Tag>?,
    @SerializedName(Constants.CHATROOM_ID)
    var chatroomId: Int?,
    override var hasImage: Boolean = photoUrl == null,
) : ExpandedModel, Parcelable {
    fun updateFromFieldMap(fieldMap: Map<String?, String?>): Unit {
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
                Constants.TAGS -> {
                    tags = GsonBuilder()
                        .registerTypeAdapter(
                            Tag::class.java,
                            TagTypeAdapter()
                        ) // FIXME: Extract into singleton
                        .create().fromJson(value!!)
                }
                Constants.PHOTO_URL -> {
                    photoUrl = value
                    hasImage = value == null
                }
            }
        }
    }
}