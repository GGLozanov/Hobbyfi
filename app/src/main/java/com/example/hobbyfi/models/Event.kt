package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "events")
@Keep
@Parcelize
data class Event(
    @PrimaryKey
    override val id: Long,
    override var name: String,
    var description: String,
    val startDate: String, // converted to Date format in back-end
    val date: String,
    override var photoUrl: String?,
    val latitude: Double,
    val longitude: Double
) : ExpandedModel, Parcelable {
    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Event {
        TODO("Not implemented yet")
    }

}