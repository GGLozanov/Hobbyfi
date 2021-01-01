package com.example.hobbyfi.persistence

import androidx.room.Dao
import androidx.room.Query
import com.example.hobbyfi.models.Event
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao : BaseDao<Event> {
    @Query("SELECT * FROM events WHERE id = (SELECT lastEventId FROM chatrooms WHERE id = :chatroomId)")
    fun getEventByChatroomId(chatroomId: Long): Flow<Event?>

    @Query("DELETE FROM events WHERE id = :id")
    fun deleteEventById(id: Long): Int
}