package com.example.hobbyfi.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ImageUploadWorker(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        // TODO: Well, implement, for image upload
        TODO("Not yet implemented")
    }
}