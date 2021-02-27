package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey

// FIXME: This is not a very good abstraction...
interface Model : Parcelable {
    val id: Long // FIXME: find a way to work around this being 'Long' since it's only required for the huge facebook user Id

    fun updateFromFieldMap(fieldMap: Map<String, String?>): Model
}
