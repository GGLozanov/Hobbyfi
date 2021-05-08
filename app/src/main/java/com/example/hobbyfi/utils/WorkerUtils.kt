package com.example.hobbyfi.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.work.DeviceTokenDeleteWorker
import com.example.hobbyfi.work.DeviceTokenUploadWorker
import com.example.hobbyfi.work.DeviceTokenWorker
import com.example.hobbyfi.work.ImageUploadWorker
import java.util.concurrent.TimeUnit

object WorkerUtils {
    inline fun <reified T: DeviceTokenWorker> buildAndEnqueueDeviceTokenWorker(
        authToken: String,
        token: String?, context: Context
    ) {
        val workData = workDataOf(Constants.TOKEN to token, Constants.AUTH_HEADER to authToken)

        // send to server (auth'd)
        val deviceTokenUploadWork = OneTimeWorkRequestBuilder<T>()
            .setInputData(workData)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS * 6, // 10 seconds * 6 = 1 minute
                TimeUnit.MILLISECONDS
            ).build()
        WorkManager.getInstance(context).enqueue(deviceTokenUploadWork)
    }

    fun buildAndEnqueueImageUploadWorker(
        modelId: Long,
        authToken: String,
        type: String,
        imageUri: String,
        context: Context,
        clearRefreshPrefId: Int, // TODO: These should be replaced w/ separate prefs tracking image changes, not cache
        chatroomId: Long? = null
    ) {
        val workData = workDataOf(
            Constants.ID to modelId,
            Constants.AUTH_HEADER to authToken,
            Constants.TYPE to type,
            Constants.CHATROOM_ID to chatroomId,
            Constants.IMAGE to imageUri,
            Constants.PREF_ID to clearRefreshPrefId
        )

        // send to server (auth'd)
        val imageUploadWork = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(workData)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS * 6, // 10 seconds * 6 = 1 minute
                TimeUnit.MILLISECONDS
            ).build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(type, ExistingWorkPolicy.APPEND_OR_REPLACE,
                imageUploadWork) // key by upload type
    }

    data class ImageUploadEvent(
        val modelId: Long,
        val type: String,
        val response: String
    )
}