package com.example.hobbyfi.persistence

import androidx.room.Dao
import com.example.hobbyfi.models.Event

@Dao
interface EventDao : BaseDao<Event> {

}