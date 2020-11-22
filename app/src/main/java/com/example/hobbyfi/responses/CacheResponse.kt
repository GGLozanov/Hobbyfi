package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Model

abstract class CacheResponse<T: Model>(
    response: String?,
    val model: T
) : Response(response) {
} // always has at least one room entity associated with its response