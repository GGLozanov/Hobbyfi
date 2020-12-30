package com.example.hobbyfi.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


object ImageUtils {
    /**
     * Encodes a given bitmap (preferably for an image) to Base64
     * @return String — the encoded Base64 bitmap
     */
    fun encodeImage(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // stream of bytes to represent the bitmap with
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        // compress bitmap to JPEG w/ best quality possible and pass it into the ByteArrayOutputStream
        val imageByte: ByteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(
            imageByte,
            DEFAULT
        ) // encode byte array to string in Base64 w/ default_img flags
    }

    fun getBitmapFromUri(activity: Activity, uri: Uri) : Bitmap {
        val contentResolver = activity
            .contentResolver // provides access to content model (class used to interface and access the data)
        return if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(
                contentResolver,
                uri
            )
        } else {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    contentResolver,
                    uri
                )
            )
        }
    }

    fun getEncodedImageFromUri(activity: Activity, uri: Uri) = encodeImage(getBitmapFromUri(activity, uri))
}