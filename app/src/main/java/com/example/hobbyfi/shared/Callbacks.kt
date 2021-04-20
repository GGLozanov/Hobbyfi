package com.example.hobbyfi.shared

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.repositories.Repository
import com.example.hobbyfi.ui.chatroom.EventChooseLocationMapsActivity
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.viewmodels.chatroom.EventAccessorViewModel
import com.facebook.AccessToken
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.FirebaseException
import com.google.firebase.messaging.FirebaseMessaging
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.lang.InstantiationException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pub.devrel.easypermissions.EasyPermissions
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*


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
                return ImageUtils.getBitmapFromUri(activity.contentResolver, data.data!!)
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

    fun requestLocationForMapsAccess(
        activity: Activity,
        permissionRequestCode: Int = Constants.locationPermissionsRequestCode
    ): Boolean {
        return if(EasyPermissions.hasPermissions(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION,)) {
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

    fun requestExternalWriteForBelowQ(
        activity: Activity,
        permissionRequestCode: Int = Constants.externalStorageWriteCode
    ): Boolean {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || EasyPermissions.hasPermissions(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            true
        } else {
            EasyPermissions.requestPermissions(
                activity,
                activity.getString(R.string.access_fine_location_rationale),
                permissionRequestCode,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            false
        }
    }

    fun hideKeyboardFrom(context: Context, view: View?) {
        val imm: InputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }


    @ExperimentalCoroutinesApi
    fun initDateTimeDatePickerDialog(
        context: Context,
        listener: DatePickerDialog.OnDateSetListener,
        viewModel: EventAccessorViewModel
    ) {
        val c = Calendar.getInstance()
        val initialYear = c.get(Calendar.YEAR)
        val initialMonth = c.get(Calendar.MONTH)
        val initialDay = c.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            context,
            listener,
            initialYear,
            initialMonth,
            initialDay
        )
        dialog.datePicker.minDate = c.timeInMillis
        initDateTimePickerDialogDismissHandler(context, dialog, viewModel)
        dialog.show()
    }

    @ExperimentalCoroutinesApi
    fun initDateTimePickerDialogDismissHandler(
        context: Context,
        dialog: AlertDialog,
        viewModel: EventAccessorViewModel
    ) {
        dialog.setOnCancelListener {
            context.buildYesNoAlertDialog(
                context.resources.getString(R.string.keep_date),
                { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                },
                { dialogInterface: DialogInterface, _: Int ->
                    viewModel.setEventDate(null)
                    dialogInterface.dismiss()
                }
            )
        }
    }

    @ExperimentalCoroutinesApi
    fun onEventDateSet(
        eventCalendar: Calendar,
        year: Int,
        month: Int,
        day: Int,
        viewModel: EventAccessorViewModel,
        context: Context,
        activity: Activity, listener: TimePickerDialog.OnTimeSetListener
    ) {
        eventCalendar.set(Calendar.YEAR, year)
        eventCalendar.set(Calendar.MONTH, month)
        eventCalendar.set(Calendar.DAY_OF_MONTH, day)

        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)

        val dialog = TimePickerDialog(activity, listener, hour, minute, DateFormat.is24HourFormat(activity))

        dialog.show()
        initDateTimePickerDialogDismissHandler(context, dialog, viewModel)
    }

    @ExperimentalCoroutinesApi
    fun onEventTimeSet(eventCalendar: Calendar, hours: Int, minutes: Int, viewModel: EventAccessorViewModel) {
        eventCalendar.set(Calendar.HOUR_OF_DAY, hours)
        eventCalendar.set(Calendar.MINUTE, minutes)

        viewModel.setEventDate(eventCalendar.time)
    }

    @ExperimentalCoroutinesApi
    fun startChooseEventLocationMapsActivity(fragment: Fragment, viewModel: EventAccessorViewModel, latLng: LatLng? = null) {
        Intent(fragment.requireContext(), EventChooseLocationMapsActivity::class.java).apply {
            putExtra(Constants.EVENT_TITLE, viewModel.name.value)
            putExtra(Constants.EVENT_DESCRIPTION, viewModel.description.value)
            putExtra(Constants.EVENT_LOCATION, latLng ?: viewModel.eventLatLng)
        }.run {
            fragment.startActivityForResult(this, Constants.eventLocationRequestCode)
        }
    }
}