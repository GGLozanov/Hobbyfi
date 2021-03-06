package com.example.hobbyfi.models.data

import androidx.annotation.Keep
import androidx.room.Entity
import com.example.hobbyfi.shared.RemoteKeyType
import kotlinx.parcelize.Parcelize

@Entity(tableName = "remoteKeys", primaryKeys = ["id", "modelType"])
@Parcelize
@Keep
data class RemoteKeys(
    override val id: Long,
    val nextKey: Int?,
    val prevKey: Int?,
    val modelType: RemoteKeyType // user, tag, or message -> used for filtering; MUST be between these three
) : Model {
    override fun updateFromFieldMap(fieldMap: Map<String, String?>): RemoteKeys = this
}