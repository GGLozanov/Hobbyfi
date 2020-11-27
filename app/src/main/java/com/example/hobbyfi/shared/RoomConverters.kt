package com.example.hobbyfi.shared

import androidx.room.TypeConverter
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
import com.example.hobbyfi.models.Tag
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

class RoomConverters {

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Tag::class.java, TagTypeAdapter())
        .create() // TODO: Extract this into singleton (DI)

    @TypeConverter
    fun fromRemoteKeyType(value: RemoteKeyType): String = value.name

    @TypeConverter
    fun toRemoteKeyType(value: String): RemoteKeyType = enumValueOf<RemoteKeyType>(value)

    @TypeConverter
    fun fromTagList(value: List<Tag>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toTagList(value: String): List<Tag>? {
        return gson.fromJson<List<Tag>>(value)
    }
}