package com.example.hobbyfi.utils

import android.content.Context
import androidx.work.*
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.work.DeviceTokenDeleteWorker
import com.example.hobbyfi.work.DeviceTokenUploadWorker
import com.example.hobbyfi.work.DeviceTokenWorker
import java.util.concurrent.TimeUnit

object WorkerUtils {
    inline fun <reified T: DeviceTokenWorker> buildAndEnqueueDeviceTokenWorker(token: String?, context: Context) {
        val workData = workDataOf(Constants.TOKEN to token)

        // send to server (auth'd)
        val deviceTokenUploadWork = OneTimeWorkRequestBuilder<T>()
            .setInputData(workData)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS * 6, // 10 seconds * 6 = 1 minute
                TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueue(deviceTokenUploadWork)
    }
}