package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Chatroom

// fetches chatroom info (name, desc., size, etc.) & chatroom tags
data class ChatroomResponse(
    override val response: String?,

) : CacheResponse<Chatroom>(response) // list of this response = Map<String, ChatroomResponse>