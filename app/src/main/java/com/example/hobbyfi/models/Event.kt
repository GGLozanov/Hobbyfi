package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey
    override val id: Int,
    val name: String,
    val description: String,
    val startDate: String, // converted to Date format in back-end
    val date: String,
    val hasImage: Boolean,
    val latitude: Double,
    val longitude: Double
) : Model