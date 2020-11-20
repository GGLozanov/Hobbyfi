package com.example.hobbyfi.persistence

import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.shared.RemoteKeyType

@Dao
interface RemoteKeysDao : BaseDao<RemoteKeys> {
    @Query("SELECT * FROM remoteKeys WHERE id = :id")
    fun getRemoteKeysById(id: Int): RemoteKeys?

    @Query("SELECT * FROM remoteKeys WHERE id = :id AND modelType = :remoteKeyType")
    fun getRemoteKeysByIdAndType(id: Int, remoteKeyType: RemoteKeyType): RemoteKeys?

    @Query("DELETE FROM remoteKeys")
    suspend fun clearRemoteKeys()

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType")
    suspend fun clearRemoteKeyType(remoteKeyType: RemoteKeyType)
}