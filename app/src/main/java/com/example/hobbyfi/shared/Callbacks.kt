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
            is HobbyfiAPI.NoConnectivityException -> throw Exception(Constants.noConnectionError)
            is HttpException -> {

                when(ex.code()) {
                    400 -> { // bad request (missing data)
                        throw Exception(Constants.missingDataError)
                    }
                    401 -> { // unauthorized
                        throw if(!isAuthorisedRequest) Exception(Constants.invalidCredentialsError)
                        else Repository.AuthorisedRequestException(Constants.unauthorisedAccessError) // only for login incorrect password error
                    }
                    409 -> { // conflict
                        throw Exception(Constants.resourceExistsError) // FIXME: Generify response for future endpoints with "exist" as response, idfk
                    }
                }

                throw Exception(ex.message().toString() + " ; code: " + ex.code())
            }
            is ExpiredJwtException -> {
                throw if(isAuthorisedRequest) Repository.AuthorisedRequestException()
                    else Repository.ReauthenticationException(Constants.expiredTokenError)
            }
            is Repository.ReauthenticationException -> throw ex
            else -> throw Exception(Constants.unknownError(ex.message))
        }
    }
}