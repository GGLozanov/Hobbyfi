package com.example.hobbyfi.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.Keep
import com.google.firebase.firestore.GeoPoint
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Keep
data class UserGeoPoint(
    val username: String,
    val chatroomId: Long,
    val eventIds: List<Long>,
    val geoPoint: GeoPoint
) : Parcelable { // custom parcelable in lieu of annotation because GeoPoint can't be serialized directly
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(username)
        parcel.writeLong(chatroomId)
        parcel.writeArray(eventIds.toTypedArray())
        parcel.writeDouble(geoPoint.latitude)
        parcel.writeDouble(geoPoint.longitude)
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<UserGeoPoint> {
            override fun createFromParcel(parcel: Parcel) = UserGeoPoint(parcel)
            override fun newArray(size: Int) = arrayOfNulls<UserGeoPoint>(size)
        }
    }

    private constructor(parcel: Parcel) : this(
        username = parcel.readString()!!,
        chatroomId = parcel.readLong(),
        eventIds = parcel.readArray(Long::class.java.classLoader)!!.toList() as List<Long>,
        geoPoint = GeoPoint(parcel.readDouble(), parcel.readDouble())
    )
}