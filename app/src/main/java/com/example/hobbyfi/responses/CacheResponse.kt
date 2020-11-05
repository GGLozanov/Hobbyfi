package com.example.hobbyfi.responses

import com.example.hobbyfi.models.Model

abstract class CacheResponse<T: Model> : Response() {
    val model: T // always has at least one room entity associated with its response
        get() {
            TODO()
        }

}