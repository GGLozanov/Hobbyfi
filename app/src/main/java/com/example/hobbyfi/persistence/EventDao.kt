package com.example.hobbyfi.persistence

import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.data.Event
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EventDao : BaseDao<Event>() {
    @Query("SELECT * FROM events WHERE chatroomId = :chatroomId")
    abstract fun getEventByChatroomId(chatroomId: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    abstract fun getEventById(id: Long): Flow<Event?>

    @Query("DELETE FROM events WHERE id = :id")
    abstract suspend fun deleteEventById(id: Long): Int

    @Query("DELETE FROM events WHERE id IN (:ids)")
    abstract suspend fun deleteEventById(ids: List<Long>): Int

    @Query("DELETE FROM events")
    abstract suspend fun deleteEvents(): Int
}