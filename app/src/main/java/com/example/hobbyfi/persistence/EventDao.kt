package com.example.hobbyfi.persistence

import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.Event
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EventDao : BaseDao<Event>() {
    @Query("SELECT * FROM events WHERE chatroomId = :chatroomId")
    abstract fun getEventByChatroomId(chatroomId: Long): Flow<List<Event>?>

    @Query("DELETE FROM events WHERE id = :id")
    abstract fun deleteEventById(id: Long): Int
}