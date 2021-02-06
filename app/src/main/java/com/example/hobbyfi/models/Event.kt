package com.example.hobbyfi.models

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.core.view.isVisible
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.fromJson
import com.google.gson.annotations.SerializedName
import com.prolificinteractive.materialcalendarview.CalendarDay
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
    var date: String,
    @SerializedName(Constants.PHOTO_URL)
    override var photoUrl: String?,
    var latitude: Double,
    var longitude: Double,
    @SerializedName(Constants.CHATROOM_ID)
    val chatroomId: Long
) : ExpandedModel {
    constructor(data: Map<String, String?>) : this(
        (data[Constants.ID] ?: error("Event ID must not be null!")).toLong(),
        data[Constants.NAME] ?: error("Event name must not be null!"),
        data[Constants.DESCRIPTION],
        data[Constants.START_DATE] ?: error("Event start date must not be null!"),
        data[Constants.DATE] ?: error("Event date must not be null!"),
        data[Constants.PHOTO_URL],
        data[Constants.LATITUDE]?.toDouble() ?: error("Event lat must not be null!"),
        data[Constants.LONGITUDE]?.toDouble() ?: error("Event long must not be null!"),
        data[Constants.CHATROOM_ID]?.toLong() ?: error("Event chatroom ID must not be null!")
    )

    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): Event {
        for((key, value) in fieldMap.entries) {
            when(key) {
                Constants.NAME -> {
                    name = value!!
                }
                Constants.DESCRIPTION -> {
                    description = value
                }
                Constants.PHOTO_URL -> {
                    photoUrl = BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir + "/" + id + ".jpg"
                    // no need to update it generally because it's always the same but we need to wake up observer and reload it?
                }
                Constants.DATE -> {
                    date = value!!
                }
                Constants.LONGITUDE -> {
                    longitude = value!!.toDouble()
                }
                Constants.LATITUDE -> {
                    latitude = value!!.toDouble()
                }
            }
        }
        return this
    }

    val calendarDayFromDate: CalendarDay get() = calendarDayFromString(date)

    val calendarDayFromStartDate: CalendarDay get() = calendarDayFromString(startDate)

    val localDateTimeFromDate: LocalDateTime get() = localDateTimeFromString(date)

    val localDateTimeFromStartDate: LocalDateTime get() = localDateTimeFromString(startDate)

    private fun calendarDayFromString(src: String): CalendarDay {
        val dateTime = localDateTimeFromString(src)
        return CalendarDay.from(dateTime.year, dateTime.month.value, dateTime.dayOfMonth)
    }

    private fun localDateTimeFromString(src: String): LocalDateTime {
        return LocalDateTime.parse(src, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    fun calculateDateDiff(): Duration = Duration.between(LocalDateTime.now(), localDateTimeFromDate)
}