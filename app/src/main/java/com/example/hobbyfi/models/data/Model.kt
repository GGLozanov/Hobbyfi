package com.example.hobbyfi.models.data

import android.os.Parcelable

// FIXME: This is not a very good abstraction...
interface Model : Parcelable {
    val id: Long // FIXME: find a way to work around this being 'Long' since it's only required for the huge facebook user Id

    fun updateFromFieldMap(fieldMap: Map<String, String?>): Model
}
