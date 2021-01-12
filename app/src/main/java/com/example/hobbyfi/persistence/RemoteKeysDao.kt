package com.example.hobbyfi.persistence

import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.RemoteKeys
import com.example.hobbyfi.shared.RemoteKeyType

@Dao
abstract class RemoteKeysDao : BaseDao<RemoteKeys>() {
    @Query("SELECT * FROM remoteKeys WHERE id = :id")
    abstract suspend fun getRemoteKeysById(id: Long): RemoteKeys?

    @Query("SELECT * FROM remoteKeys WHERE id = :id AND modelType = :remoteKeyType")
    abstract suspend fun getRemoteKeysByIdAndType(id: Long, remoteKeyType: RemoteKeyType): RemoteKeys?

    @Query("SELECT * FROM remoteKeys WHERE id = :id AND id IN (:ids) AND modelType = :remoteKeyType")
    abstract suspend fun getRemoteKeysTypeAndIds(id: Long, ids: List<Long>, remoteKeyType: RemoteKeyType): RemoteKeys?

    @Query("SELECT * FROM remoteKeys WHERE id = :id AND id NOT IN (:ids) AND modelType = :remoteKeyType")
    abstract suspend fun getRemoteKeysTypeAndNotPresentInIds(id: Long, ids: List<Long>, remoteKeyType: RemoteKeyType): RemoteKeys?

    @Query("DELETE FROM remoteKeys")
    abstract suspend fun deleteRemoteKeys()

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType")
    abstract suspend fun deleteRemoteKeyByType(remoteKeyType: RemoteKeyType)

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType AND id IN (:ids)")
    abstract suspend fun deleteRemoteKeysByTypeAndIds(remoteKeyType: RemoteKeyType, ids: List<Long>)

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType AND id NOT IN (:ids)")
    abstract suspend fun deleteRemoteKeysByTypeAndNotPresentInIds(remoteKeyType: RemoteKeyType, ids: List<Long>)

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType AND id = :id")
    abstract suspend fun deleteRemoteKeysForIdAndType(id: Long, remoteKeyType: RemoteKeyType): Int

    @Query("DELETE FROM remoteKeys WHERE modelType = :remoteKeyType AND id != :id")
    abstract suspend fun deleteRemoteKeysExceptForIdAndForType(id: Long, remoteKeyType: RemoteKeyType): Int

}