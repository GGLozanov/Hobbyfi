package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

// in-memory db for predefined tags
// users can create their own tags (name + colour)
// once list of tag names is sent to backend for user/chatroom creation
// backend performs SELECT query to see if tag of same name already exists
// if not, insert and make connections with chatroom/user
// if yes, don't insert and make connections
// once user goes to see other chatrooms/users, they'll see their custom tags from the API fetch from the backend
// TODO: Save only custom tags for relevant users & chatrooms here
@Entity(tableName = "tags")
@Keep
@Parcelize
data class Tag(
  val name: String,
  val colour: String,
  val isFromFacebook: Boolean = false,
  @PrimaryKey(autoGenerate = true)
  override val id: Long = 0
) : Model, Parcelable {
  override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Tag = this
}