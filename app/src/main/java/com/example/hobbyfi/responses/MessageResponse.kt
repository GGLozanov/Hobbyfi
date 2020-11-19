package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Message

class MessageResponse(override val response: String?) : CacheResponse<Message>(response) {
}