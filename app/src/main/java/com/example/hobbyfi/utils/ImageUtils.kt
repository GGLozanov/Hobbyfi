package com.example.hobbyfi.utils

import android.app.Activity
import android.content.ContentResolver
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
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.*


object ImageUtils {
    /**
     * Encodes a given bitmap (preferably for an image) to Base64
     * @return String — the encoded Base64 bitmap
     */
    suspend fun encodeImage(bitmap: Bitmap, compressType: CompressType): String =
        withContext(Dispatchers.IO) {
            val imageByte = compressType.compressBitmap(bitmap)
            val image = Base64.encodeToString(
                imageByte,
                DEFAULT
            ) // encode byte array to string in Base64 w/ default_img flags
            withContext(Dispatchers.Main) {
                image
            }
        }

    fun getBitmapFromUri(contentResolver: ContentResolver, uri: Uri) : Bitmap {
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

    suspend fun getEncodedImageFromUri(contentResolver: ContentResolver,
                                       uri: Uri, compressType: CompressType) =
        encodeImage(getBitmapFromUri(contentResolver, uri), compressType)

    enum class CompressType(val imageQuality: Int, val width: Float, val height: Float) {
        PROFILE_PICTURE(75,
            MainApplication.applicationContext
                .resources
                .getDimension(R.dimen.circle_pic_width),
            MainApplication.applicationContext
                .resources
                .getDimension(R.dimen.circle_pic_height)),
        MESSAGE_PICTURE(85,
            MainApplication.applicationContext
                .resources
                .getDimension(R.dimen.flat_pic_width),
            MainApplication.applicationContext
                .resources
                .getDimension(R.dimen.flat_pic_height));

        fun compressBitmap(bitmap: Bitmap): ByteArray {
            val byteArrayOutputStream = ByteArrayOutputStream()
            // stream of bytes to represent the bitmap with
            val bm = Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), false)
            bm.compress(Bitmap.CompressFormat.JPEG, imageQuality, byteArrayOutputStream)
            // compress bitmap to JPEG w/ best quality possible and pass it into the ByteArrayOutputStream
            return byteArrayOutputStream.toByteArray()
        }
    }
}