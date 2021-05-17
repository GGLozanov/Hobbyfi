package com.example.hobbyfi.shared

import androidx.room.TypeConverter
import com.example.hobbyfi.models.data.Tag

class RoomConverters {

    @TypeConverter
    fun fromRemoteKeyType(value: RemoteKeyType): String = value.name

    @TypeConverter
    fun toRemoteKeyType(value: String): RemoteKeyType = enumValueOf(value)

    @TypeConverter
    fun fromTagList(value: List<Tag>?): String {
        return Constants.jsonConverter.toJson(value)
    }

    @TypeConverter
    fun toTagList(value: String): List<Tag>? {
        return Constants.jsonConverter.fromJson<List<Tag>>(value)
    }

    @TypeConverter
    fun toLongList(value: String): List<Long>? {
        return Constants.jsonConverter.fromJson(value)
    }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String = Constants.jsonConverter.toJson(value)
}