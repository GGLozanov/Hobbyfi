package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Message

data class MessageResponse(
    override val response: String?,
    override val model: Message
) : CacheResponse<Message>