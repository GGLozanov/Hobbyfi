package com.example.hobbyfi.models.data

import android.graphics.Bitmap

class Base64Image {
    private var _base64: String? = null
    private var _originalUri: String? = null
    val base64 get() = _base64
    val originalUri get() = _originalUri

    fun setImageBase64(base64Image: String) {
        _base64 = base64Image
    }

    fun setOriginalUri(uri: String) {
        _originalUri = uri
    }
}