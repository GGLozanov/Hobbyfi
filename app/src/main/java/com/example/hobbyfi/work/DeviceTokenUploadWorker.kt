package com.example.hobbyfi.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.shared.Constants
import java.lang.Exception
import java.lang.IllegalArgumentException

class DeviceTokenUploadWorker(
    context: Context,
    params: WorkerParameters
) : DeviceTokenWorker(context, params) {

    override suspend fun performDeviceTokenRequest() {
        val deviceToken = inputData.getString(Constants.TOKEN) ?: throw IllegalArgumentException()
        val authToken = inputData.getString(Constants.AUTH_HEADER) ?: throw IllegalArgumentException()

        hobbyfiAPI.sendDeviceToken(
            authToken,
            deviceToken
        )
    }

    override val writeDeviceTokenUploadedOnFinish: Boolean
        get() = true
}