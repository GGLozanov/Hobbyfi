package com.example.hobbyfi.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.hobbyfi.shared.RemoteKeyType

@Entity(tableName = "remoteKeys", primaryKeys = ["id", "modelType"])
data class RemoteKeys(
    override val id: Long,
    val nextKey: Int?,
    val prevKey: Int?,
    val modelType: RemoteKeyType // user, tag, or message -> used for filtering; MUST be between these three
) : Model {
    override fun updateFromFieldMap(fieldMap: Map<String?, String?>): RemoteKeys = this
}