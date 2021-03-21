package com.example.hobbyfi.work

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.lang.IllegalArgumentException

class ImageUploadWorker(
    context: Context,
    workerParameters: WorkerParameters
) : KodeinCoroutineWorker(context, workerParameters) {
    override suspend fun doWork(): Result {
        val authToken = inputData.getString(Constants.AUTH_HEADER) ?: throw IllegalArgumentException()
        val modelId = if(inputData.getLong(Constants.ID, -1L) == -1L) throw IllegalArgumentException()
                else inputData.getLong(Constants.ID, -1L)
        val type = inputData.getString(Constants.TYPE) ?: throw IllegalArgumentException()
        val chatroomId = if(inputData.getLong(Constants.CHATROOM_ID, -1L) == -1L
                    && type == Constants.EDIT_EVENT_TYPE) throw IllegalArgumentException()
                else inputData.getLong(Constants.CHATROOM_ID, -1L)
        val image = inputData.getString(Constants.IMAGE) ?: throw IllegalArgumentException()
        val prefId = if(inputData.getInt(Constants.PREF_ID, -1) == -1) throw IllegalArgumentException()
                else inputData.getInt(Constants.PREF_ID, -1)

        return try {
            hobbyfiAPI.uploadImage(
                authToken,
                modelId,
                ImageUtils.getEncodedImageFromUri(context.contentResolver, Uri.parse(image)),
                type,
                chatroomId
            )
            // yes, this shouldn't be here but we have context, so why not abuse it?
            withContext(Dispatchers.Main) {
                Toast.makeText(context, Constants.imageUploadSuccess, Toast.LENGTH_LONG)
                    .show()
            }
            prefConfig.writeLastPrefFetchTimeNow(prefId)
            Result.success()
        } catch(ex: Exception) {
            Log.w("DeviceTokenWorker", "Hobbyfi image upload request =>>> FAIL")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, Constants.imageUploadFail, Toast.LENGTH_LONG)
                    .show()
            }
            return if(ex !is IllegalArgumentException &&
                (ex is HttpException && ex.code() != 406 && ex.code() != 400)) Result.retry() else Result.failure()
        }
    }
}