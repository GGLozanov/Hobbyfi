package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Model

interface CacheResponse<T: Model> : Response {
    override val response: String?
    val model: T
} // always has at least one room entity associated with its response