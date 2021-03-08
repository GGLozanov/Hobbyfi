package com.example.hobbyfi.work

import android.content.Context
import android.util.Log
import androidx.work.WorkerParameters
import retrofit2.HttpException
import java.lang.Exception
import java.lang.IllegalArgumentException

abstract class DeviceTokenWorker(
    context: Context,
    params: WorkerParameters
) : KodeinCoroutineWorker(context, params) {
    abstract suspend fun performDeviceTokenRequest()

    abstract val writeDeviceTokenUploadedOnFinish: Boolean

    override suspend fun doWork(): Result {
        return try {
            performDeviceTokenRequest()

            Log.i("DeviceTokenWorker", "Hobbyfi device token request " +
                    "(writeDeviceTokenUploadedOnFinish: ${writeDeviceTokenUploadedOnFinish}) =>>> SUCCESS")
            prefConfig.writeCurrentDeviceTokenUploaded(writeDeviceTokenUploadedOnFinish)
            Result.success()
        } catch(ex: Exception) {
            ex.printStackTrace()
            Log.w("DeviceTokenWorker", "Failed to upload FCM token to server. Exception: ${ex.message}")
            Log.w("DeviceTokenWorker", "Hobbyfi device token request " +
                    "(writeDeviceTokenUploadedOnFinish: ${writeDeviceTokenUploadedOnFinish}) =>>> FAIL")
            return if(ex !is IllegalArgumentException &&
                (ex is HttpException && ex.code() != 406 && ex.code() != 400)) Result.retry() else Result.failure()
        }
    }
}