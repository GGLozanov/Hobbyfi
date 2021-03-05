package com.example.hobbyfi.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.shared.Constants
import java.lang.Exception

class DeviceTokenUploadWorker(
    context: Context,
    params: WorkerParameters
) : KodeinCoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deviceToken = inputData.getString(Constants.TOKEN) ?: return Result.failure()

        return try {
            hobbyfiAPI.sendDeviceToken(
                prefConfig.getAuthUserToken()!!,
                deviceToken
            )
            Log.i("DeviceTokenUploadW", "Hobbyfi send device token (${deviceToken}) =>>> SUCCESS")
            prefConfig.writeCurrentDeviceTokenUploaded(true)
            Result.success()
        } catch(ex: Exception) {
            Log.w("DeviceTokenUploadW", "Hobbyfi send device token (${deviceToken}) =>>> FAIL")
            Result.retry()
        }
    }
}