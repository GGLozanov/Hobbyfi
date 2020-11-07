package com.example.hobbyfi.shared

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter
    fun fromRemoteKeyType(value: RemoteKeyType): String? = value.name

    @TypeConverter
    fun toRemoteKeyType(value: String): RemoteKeyType? = enumValueOf<RemoteKeyType>(value)
}