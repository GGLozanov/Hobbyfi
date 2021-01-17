package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityEventChooseLocationMapsBinding
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.MapsActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import pub.devrel.easypermissions.EasyPermissions

@ExperimentalCoroutinesApi
class EventChooseLocationMapsActivity : MapsActivity() {
    private var marker: Marker? = null

    private var eventTitle: String? = null
    private var eventDescription: String? = null
    private var eventLocation: LatLng? = null

    private var exitFromConfirm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(Constants.KEY_LOCATION)
        }

        eventTitle = intent.extras?.get(Constants.EVENT_TITLE) as String?
        eventDescription = intent.extras?.get(Constants.EVENT_DESCRIPTION) as String?
        eventLocation = intent.extras?.get(Constants.EVENT_LOCATION) as LatLng?

        val binding: ActivityEventChooseLocationMapsBinding =
            ActivityEventChooseLocationMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        (supportFragmentManager.findFragmentById(binding.mapFragment.id)
                as SupportMapFragment).getMapAsync(this)

        binding.confirmFab.setOnClickListener {
            exitFromConfirm = true
            onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        map?.let {
            outState.putParcelable(Constants.KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        getLocationPermission()

        updateLocationUI()

        getDeviceLocation()
    }

    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            map?.uiSettings?.isCompassEnabled = true

            if(locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }

            map?.setOnMyLocationButtonClickListener {
                resetMarkerAndMoveToNew(LatLng(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude))
                return@setOnMyLocationButtonClickListener true
            }

            map?.setOnMapClickListener {
                resetMarkerAndMoveToNew(it)
            }

            map?.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                override fun onMarkerDragStart(p0: Marker?) {
                }

                override fun onMarkerDrag(p0: Marker?) {
                }

                override fun onMarkerDragEnd(newMarker: Marker?) {
                    marker = newMarker
                }
            })

            eventLocation?.let {
                resetMarkerAndMoveToNew(it)
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getDeviceLocation() {
        try {
            if(locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if(task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null && eventLocation == null) {
                            resetMarkerAndMoveToNew(LatLng(
                                lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude
                            ))
                            /*
                            *                 .icon(bitmapDescriptorFromVector(this, R.drawable.ic_baseline_event_white_24))
                                    .visible(true)
                            * */
                        }
                    } else {
                        Log.d("EventChooseLMActivity", "Current location is null. Using defaults.")
                        Log.e("EventChooseLMActivity", "Exception: %s", task.exception)
                        map?.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat())
                        )
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        super.onPermissionsGranted(requestCode, perms)
        getDeviceLocation()
        updateLocationUI()
    }

    override fun onBackPressed() {
        if(!exitFromConfirm) {
            val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.back_event_choose_maps)
                .setNegativeButtonIcon(ContextCompat.getDrawable(this, R.drawable.ic_baseline_cancel_24))
                .setPositiveButtonIcon(ContextCompat.getDrawable(this, R.drawable.ic_baseline_check_24))
                .setPositiveButton(R.string.yes) { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                    sendMarkerLocationResultBack()
                    super.onBackPressed()
                }
                .setNegativeButton(R.string.no) { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                    super.onBackPressed()
                }
                .create()
            dialog.window!!.setBackgroundDrawableResource(R.color.colorBackground)
            dialog.show()
        } else {
            sendMarkerLocationResultBack()
            super.onBackPressed()
        }
    }

    private fun resetMarkerAndMoveToNew(latLng: LatLng) {
        marker?.remove()
        marker = moveMarkerAndCamera(latLng, eventTitle, eventDescription)
    }

    private fun sendMarkerLocationResultBack() {
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.EVENT_LOCATION, marker!!.position)
        setResult(Activity.RESULT_OK, resultIntent)
    }
}