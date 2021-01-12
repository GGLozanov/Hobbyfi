package com.example.hobbyfi.shared

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.TokenUtils
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.lang.InstantiationException
import kotlinx.coroutines.CancellationException
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.HttpException
import java.io.IOException


object Callbacks {

    fun handleImageRequestWithPermission(
        activity: Activity, requestCode: Int,
        resultCode: Int, data: Intent?,
        onImageSuccess: (bitmap: Bitmap) -> Unit
    ) {
        getBitmapFromImageOnActivityResult(
            activity,
            Constants.imageRequestCode,
            requestCode,
            resultCode,
            data
        ).also { if(it != null) { onImageSuccess.invoke(it) } }
    }

    private fun getBitmapFromImageOnActivityResult(
        activity: Activity,
        requiredRequestCode: Int,
        requestCode: Int, resultCode: Int, data: Intent?
    ): Bitmap? {
        if (requestCode == requiredRequestCode &&
            resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            try {
                return ImageUtils.getBitmapFromUri(activity, data.data!!)
            } catch (ex: IOException) {
                Log.e(
                    "Callbacks.imageCallback", "onActivityResult (image retrieval) " +
                            "with required request code " + requiredRequestCode + " —> " + ex.toString()
                )
            }

        }
        return null
    }

    fun requestImage(
        callingFragment: Fragment, requestCode: Int = Constants.imageRequestCode,
        permissionRequestCode: Int = Constants.imagePermissionsRequestCode
    ) {
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

    fun requestLocationForEventCreate(
        activity: Activity,
        permissionRequestCode: Int = Constants.locationPermissionsRequestCode
    ): Boolean {
        return if(EasyPermissions.hasPermissions(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION,
            )) {
            true
        } else {
            EasyPermissions.requestPermissions(
                activity,
                activity.getString(R.string.access_fine_location_rationale),
                permissionRequestCode,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            false
        }
    }

    fun hideKeyboardFrom(context: Context, view: View?) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // always throws an exception
    fun dissectRepositoryExceptionAndThrow(ex: Exception, isAuthorisedRequest: Boolean = false): Nothing {
        ex.printStackTrace()
        when(ex) {
            is HobbyfiAPI.NoConnectivityException -> throw Exception(Constants.noConnectionError)
            is HttpException -> {
                ex.printStackTrace()

                when (ex.code()) {
                    400 -> { // bad request (missing data)
                        throw Exception(Constants.missingDataError)
                    }
                    401 -> { // unauthorized
                        throw if (!isAuthorisedRequest) Exception(Constants.invalidTokenError)
                        else if (!Constants.isFacebookUserAuthd())
                            Repository.AuthorisedRequestException(Constants.unauthorisedAccessError)  // only for login incorrect password error
                        else Repository.ReauthenticationException(Constants.reauthError)
                    }
                    404 -> { // not found
                        throw Repository.ReauthenticationException(Constants.resourceNotFoundError)
                    }
                    406 -> { // not acceptable
                        throw Exception(Constants.resourceExistsError)
                    }
                    409 -> { // conflict
                        throw Exception(Constants.resourceExistsError) // FIXME: Generify response for future endpoints with "exist" as response, idfk
                    }
                    500 -> {
                        throw Repository.ReauthenticationException(Constants.internalServerError)
                    }
                }

                throw Repository.NetworkException(ex.message().toString() + "; code: " + ex.code())
            }
            is ExpiredJwtException -> {
                throw if (isAuthorisedRequest) Repository.AuthorisedRequestException()
                else Repository.ReauthenticationException(Constants.expiredTokenError)
            }
            is Repository.ReauthenticationException,
            TokenUtils.InvalidStoredTokenException, is InstantiationException, is CancellationException -> throw ex
            else -> throw if(ex.message?.contains("failed to connect to") == true)
                Repository.ReauthenticationException(Constants.serverConnectionError)
                    else Repository.UnknownErrorException(Constants.unknownError(ex.message))
        }
    }
}