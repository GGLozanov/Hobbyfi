package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Model

open class CacheResponse<T: Model>(override val response: String?) : Response {
    val model: T // always has at least one room entity associated with its response
        get() {
            TODO()
        }

}