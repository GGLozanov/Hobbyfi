package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Chatroom

// fetches chatroom info (name, desc., size, etc.) & chatroom tags
class ChatroomResponse(
    response: String?,
    model: Chatroom
) : CacheResponse<Chatroom>(response, model) // list of this response = Map<String, ChatroomResponse>