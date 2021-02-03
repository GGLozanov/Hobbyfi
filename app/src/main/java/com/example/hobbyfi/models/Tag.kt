package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(tableName = "tags")
@Keep
@Parcelize
data class Tag(
  val name: String,
  val colour: String,
  val isFromFacebook: Boolean = false,
  @PrimaryKey(autoGenerate = true)
  override val id: Long = 0
) : Model {
  override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Tag = this
}