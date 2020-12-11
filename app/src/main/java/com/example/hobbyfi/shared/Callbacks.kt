package com.example.hobbyfi.shared

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.TokenUtils
import io.jsonwebtoken.ExpiredJwtException
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.HttpException
import java.io.IOException
import androidx.fragment.app.Fragment


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

    fun handlePermissionsResult(callingFragment: Fragment, requiredRequestCode: Int, requestCode: Int, resultCode: Int) {
        if(requestCode == requiredRequestCode && resultCode == Activity.RESULT_OK) {
            openImageSelection(callingFragment, requestCode)
        }
    }

    fun requestImage(callingFragment: Fragment, requestCode: Int = Constants.imageRequestCode, permissionRequestCode: Int = Constants.imagePermissionsRequestCode) {
        if(EasyPermissions.hasPermissions(
                callingFragment.requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            )) {
            openImageSelection(callingFragment, requestCode)
        } else {
            EasyPermissions.requestPermissions(
                callingFragment,
                callingFragment.getString(R.string.read_external_storage_rationale),
                permissionRequestCode,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun openImageSelection(callingFragment: Fragment, requestCode: Int) {
        val selectImageIntent = Intent()
        selectImageIntent.type = "image/*" // set MIME data type to all images

        selectImageIntent.action =
            Intent.ACTION_GET_CONTENT // set the desired action to get image

        callingFragment.startActivityForResult(
            selectImageIntent,
            requestCode
        ) // start activity and await result
    }

    // always throws an exception
    fun dissectRepositoryExceptionAndThrow(ex: Exception, isAuthorisedRequest: Boolean = false): Nothing {
        when(ex) {
            is HobbyfiAPI.NoConnectivityException -> throw Exception(Constants.noConnectionError)
            is HttpException -> {
                ex.printStackTrace()

                when(ex.code()) {
                    400 -> { // bad request (missing data)
                        throw Exception(Constants.missingDataError)
                    }
                    401 -> { // unauthorized
                        throw if(!isAuthorisedRequest) Exception(Constants.invalidTokenError)
                            else if(!Constants.isFacebookUserAuthd())
                                Repository.AuthorisedRequestException(Constants.unauthorisedAccessError)  // only for login incorrect password error
                            else Repository.ReauthenticationException(Constants.reauthError)
                    }
                    409 -> { // conflict
                        throw Exception(Constants.resourceExistsError) // FIXME: Generify response for future endpoints with "exist" as response, idfk
                    }
                    500 -> {
                        throw Repository.ReauthenticationException(Constants.internalServerError)
                    }
                }

                throw Exception(ex.message().toString() + " ; code: " + ex.code())
            }
            is ExpiredJwtException -> {
                throw if(isAuthorisedRequest) Repository.AuthorisedRequestException()
                    else Repository.ReauthenticationException(Constants.expiredTokenError)
            }
            is Repository.ReauthenticationException, TokenUtils.InvalidStoredTokenException -> throw ex
            else -> throw if(ex.message?.contentEquals("failed to connect to /") == true)
                Repository.ReauthenticationException(Constants.serverConnectionError)
                else Exception(Constants.unknownError(ex.message))
        }
    }
}