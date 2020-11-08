package com.example.hobbyfi.utils

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.util.*


object ImageUtils {
    /**
     * Encodes a given bitmap (preferably for an image) to Base64
     * @return String — the encoded Base64 bitmap
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun encodeImage(bitmap: Bitmap): String? {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // stream of bytes to represent the bitmap with
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        // compress bitmap to JPEG w/ best quality possible and pass it into the ByteArrayOutputStream
        val imageByte: ByteArray = byteArrayOutputStream.toByteArray()
        return Base64.getEncoder().encodeToString(
            imageByte
        ) // encode byte array to string in Base64 w/ default_img flags
    }
}