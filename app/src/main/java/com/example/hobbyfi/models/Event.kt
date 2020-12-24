package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hobbyfi.shared.Constants
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity(tableName = "events")
@Keep
@Parcelize
data class Event(
    @PrimaryKey
    override val id: Long,
    override var name: String,
    var description: String?,
    @SerializedName(Constants.START_DATE)
    val startDate: String, // converted to Date format in back-end
    val date: String,
    @SerializedName(Constants.PHOTO_URL)
    override var photoUrl: String?,
    val latitude: Double,
    val longitude: Double
) : ExpandedModel, Parcelable {
    constructor(data: Map<String, String?>) : this(
        (data[Constants.ID] ?: error("Event ID must not be null!")).toLong(),
        data[Constants.NAME] ?: error("Event name must not be null!"),
        data[Constants.DESCRIPTION],
        data[Constants.START_DATE] ?: error("Event start date must not be null!"),
        data[Constants.DATE] ?: error("Event date must not be null!"),
        data[Constants.PHOTO_URL],
        data[Constants.LATITUDE]?.toDouble() ?: error("Event lat must not be null!"),
        data[Constants.LONGITUDE]?.toDouble() ?: error("Event long must not be null!")
    )

    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Event {
        TODO("Not implemented yet")
    }
}