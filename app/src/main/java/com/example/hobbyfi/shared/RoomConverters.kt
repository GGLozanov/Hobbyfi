package com.example.hobbyfi.shared

import androidx.room.TypeConverter
import com.example.hobbyfi.models.Tag
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoomConverters {
    @TypeConverter
    fun fromRemoteKeyType(value: RemoteKeyType): String? = value.name

    @TypeConverter
    fun toRemoteKeyType(value: String): RemoteKeyType? = enumValueOf<RemoteKeyType>(value)

    @TypeConverter
    fun fromTagList(value: List<Tag>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toTagList(value: String): List<Tag> {
        return Gson().fromJson<List<Tag>>(value)
    }

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
}