package com.example.hobbyfi.models.data

import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.Glide

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

    fun loadUriIntoWithoutSignature(context: Context, into: ImageView) {
        originalUri?.let {
            Glide.with(context)
                .load(it)
                .into(into)
        }
    }
}