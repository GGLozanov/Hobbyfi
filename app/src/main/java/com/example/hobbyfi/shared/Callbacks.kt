package com.example.hobbyfi.shared

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.utils.ImageUtils
import io.jsonwebtoken.ExpiredJwtException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.jvm.Throws


object Callbacks {
    fun getBitmapFromImageOnActivityResult(
        activity: Activity,
        requiredRequestCode: Int,
        requestCode: Int, resultCode: Int, data: Intent?
    ): Bitmap? {
        if (requestCode == requiredRequestCode &&
            resultCode == Activity.RESULT_OK && data != null && data.data != null) {

            try {
                return ImageUtils.getBitmapFromUri(activity, data.data!!)
            } catch(ex: IOException) {
                Log.e(
                    "Callbacks.imageCallback", "onActivityResult (image retrieval) " +
                            "with required request code " + requiredRequestCode + " —> " + ex.toString()
                )
            }

        }
        return null
    }

    // "returns" a response when in reality it always throws an exception
    fun dissectRepositoryExceptionAndThrow(ex: Exception, isAuthorisedRequest: Boolean = false): Nothing {
        when(ex) {
            is HobbyfiAPI.NoConnectivityException -> throw Exception("Couldn't perform operation! Please check your connection!")
            is HttpException -> {

                if(ex.code() == 409) { // conflict
                    throw Exception("This user/thing already exists!") // FIXME: Generify response for future endpoints with "exist" as response, idfk
                } // TODO: Might use if responses from API are too generic. Will make them not be!

                if(ex.code() == 401) { // unauthorized
                    throw if(isAuthorisedRequest) Exception("Invalid credentials!")
                        else Repository.AuthorisedRequestException() // only for login incorrect password error
                }

                throw Exception(ex.message().toString() + " ; code: " + ex.code())
            }
            is ExpiredJwtException -> {
                throw if(isAuthorisedRequest) Repository.AuthorisedRequestException()
                    else Repository.ReauthenticationException("Your session may have expired and you need to (re)authenticate!")
            }
            is Repository.ReauthenticationException -> throw ex
            else -> throw Exception("Unknown error! Please check your connection or contact a developer! ${ex.message}")
        }
    }
}