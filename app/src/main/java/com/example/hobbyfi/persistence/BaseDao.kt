package com.example.hobbyfi.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.example.hobbyfi.models.Model

@Dao
interface BaseDao<T: Model> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(entityList: List<T>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T)
}