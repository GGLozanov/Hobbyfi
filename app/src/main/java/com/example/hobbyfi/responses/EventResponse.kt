package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Event

// fetched last event for given chatroom
class EventResponse(
    response: String?,
    model: Event
) : CacheResponse<Event>(response, model)