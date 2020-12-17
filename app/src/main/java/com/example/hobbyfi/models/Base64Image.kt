package com.example.hobbyfi.models

class Base64Image {
    private var _base64: String? = null
    val base64 get() = _base64

    fun setImageBase64(base64Image: String) {
        _base64 = base64Image
    }
}