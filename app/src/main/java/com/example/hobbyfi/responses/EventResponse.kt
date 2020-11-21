package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Event

// fetched last event for given chatroom
data class EventResponse(
    override val response: String?,
    override val model: Event
) : CacheResponse<Event>