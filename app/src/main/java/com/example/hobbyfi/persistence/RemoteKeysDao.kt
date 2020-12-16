package com.example.hobbyfi.persistence

import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.shared.RemoteKeyType

@Dao
interface RemoteKeysDao : BaseDao<RemoteKeys> {
    @Query("SELECT * FROM remoteKeys WHERE id = :id")
    suspend fun getRemoteKeysById(id: Long): RemoteKeys?

    @Query("SELECT * FROM remoteKeys WHERE id = :id AND modelType = :remoteKeyType")
    suspend fun getRemoteKeysByIdAndType(id: Long, remoteKeyType: RemoteKeyType): RemoteKeys?

    @Query("DELETE FROM remoteKeys")
    suspend fun deleteRemoteKeys()

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType")
    suspend fun deleteRemoteKeyByType(remoteKeyType: RemoteKeyType)

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType AND id = :id")
    suspend fun deleteRemoteKeysForIdAndType(id: Long, remoteKeyType: RemoteKeyType): Int

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType AND id != :id")
    suspend fun deleteRemoteKeysExceptForIdAndForType(id: Long, remoteKeyType: RemoteKeyType): Int
}