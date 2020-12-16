package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
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
) : ExpandedModel {
    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Event {
        TODO("Not implemented yet")
    }
}