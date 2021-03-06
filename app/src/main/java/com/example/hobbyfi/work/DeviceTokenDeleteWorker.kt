package com.example.hobbyfi.work

import android.content.Context
import androidx.work.WorkerParameters
import com.example.hobbyfi.shared.Constants
import java.lang.IllegalArgumentException

class DeviceTokenDeleteWorker(
    context: Context,
    params: WorkerParameters
) : DeviceTokenWorker(context, params) {

    override suspend fun performDeviceTokenRequest() {
        val deviceToken = inputData.getString(Constants.TOKEN) ?: throw IllegalArgumentException()

        hobbyfiAPI.deleteDeviceToken(
            prefConfig.getAuthUserToken()!!,
            deviceToken
        )
    }

    override val writeDeviceTokenUploadedOnFinish: Boolean
        get() = false
}