package com.example.hobbyfi.persistence

import androidx.room.*
import com.example.hobbyfi.models.Model


@Dao
abstract class BaseDao<T : Model> {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entityList: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(entity: T): Long

    @Delete
    abstract suspend fun delete(entity: T): Int

    @Delete
    abstract suspend fun delete(entityList: List<T>): Int

    @Update
    abstract suspend fun update(entity: T)

    @Update
    abstract suspend fun update(entityList: List<T>?)

    @Transaction
    open suspend fun upsert(obj: T) {
        val id: Long = insert(obj)
        if(id == -1L) {
            update(obj)
        }
    }

    @Transaction
    open suspend fun upsert(entityList: List<T>) {
        val insertResult: List<Long> = insert(entityList)
        val updateList: MutableList<T> = ArrayList()
        for (i in insertResult.indices) {
            if(insertResult[i] == -1L) {
                updateList.add(entityList[i])
            }
        }
        if(updateList.isNotEmpty()) {
            update(updateList)
        }
    }
}