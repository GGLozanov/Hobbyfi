package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hobbyfi.shared.Constants
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity(tableName = "tags")
@Keep
@Parcelize
data class Tag(
  val name: String,
  val colour: String,
  @SerializedName(Constants.isFromFacebook)
  val isFromFacebook: Boolean = false,
  @PrimaryKey(autoGenerate = true)
  override val id: Long = 0
) : Model {
  override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Tag = this
}