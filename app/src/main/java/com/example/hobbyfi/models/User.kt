package com.example.hobbyfi.models

import android.os.Parcel
import android.os.Parcelable
import android.util.Log
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
    @SerializedName(Constants.CHATROOM_ID)
    var chatroomId: Long?,
) : ExpandedModel, Parcelable {
    fun updateFromFieldMap(fieldMap: Map<String?, String?>): User {
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
                Constants.CHATROOM_ID -> {
                    val chatroomId = value!!.toLong()
                    this.chatroomId = if(chatroomId.compareTo(0) == 0) null else chatroomId
                }
            }
        }
        return this
    }
}