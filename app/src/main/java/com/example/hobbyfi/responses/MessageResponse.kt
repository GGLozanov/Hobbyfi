package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Message

class MessageResponse(
    response: String?,
    model: Message
) : CacheResponse<Message>(response, model)