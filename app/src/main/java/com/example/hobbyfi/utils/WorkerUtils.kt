package com.example.hobbyfi.utils

import android.content.Context
import android.graphics.Bitmap
import androidx.work.*
import com.example.hobbyfi.shared.Constants
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
        chatroomId: Long? = null
    ) {
        val workData = workDataOf(
            Constants.ID to modelId,
            Constants.AUTH_HEADER to authToken,
            Constants.TYPE to type,
            Constants.CHATROOM_ID to chatroomId,
            Constants.IMAGE to imageUri
        )

        // send to server (auth'd)
        val imageUploadWork = OneTimeWorkRequestBuilder<ImageUploadWorker>()
            .setInputData(workData)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS * 6, // 10 seconds * 6 = 1 minute
                TimeUnit.MILLISECONDS
            ).build()
        WorkManager.getInstance(context).enqueueUniqueWork(type,
            ExistingWorkPolicy.APPEND_OR_REPLACE, imageUploadWork) // key by upload type
    }
}