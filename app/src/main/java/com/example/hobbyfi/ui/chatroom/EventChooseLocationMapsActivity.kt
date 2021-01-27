package com.example.hobbyfi.ui.chatroom

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityEventChooseLocationMapsBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.MapsActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class EventChooseLocationMapsActivity : MapsActivity() {
    private var marker: Marker? = null // no need to stay in VM because of savedInstanceState mechanisms

    private var eventTitle: String? = null
    private var eventDescription: String? = null
    private var eventLocation: LatLng? = null

    // location retrieved by the Fused Location Provider.
    private var lastKnownLocation: Location? = null

    private var exitFromConfirm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            savedInstanceState.run {
                lastKnownLocation = getParcelable(Constants.KEY_LOCATION)
                eventLocation = getParcelable(Constants.LOCATION)
                eventTitle = getString(Constants.NAME)
                eventDescription = getString(Constants.DESCRIPTION)
            }
        } else {
            eventLocation = intent.extras?.get(Constants.EVENT_LOCATION) as LatLng?
            eventTitle = intent.extras?.get(Constants.EVENT_TITLE) as String?
            eventDescription = intent.extras?.get(Constants.EVENT_DESCRIPTION) as String?
        }

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
            outState.run {
                putParcelable(Constants.KEY_LOCATION, lastKnownLocation)
                putParcelable(Constants.LOCATION, eventLocation)
                putString(Constants.NAME, eventTitle)
                putString(Constants.DESCRIPTION, eventDescription)
            }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)

        getLocationPermission()

        getDeviceLocation()
    }

    override fun updateLocationUI() {
        super.updateLocationUI()
        if(!locationPermissionGranted) {
            lastKnownLocation = null
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
        Log.i("EventCLMActivity", "onPermissionGranted. Location perm granted: $locationPermissionGranted")
        getDeviceLocation()
        updateLocationUI()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // not much to do for now, I guess?
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
        marker = addMarkerAndAnimateCamera(latLng, eventTitle, eventDescription)
    }

    private fun addMarkerAndAnimateCamera(latLng: LatLng, title: String? = null, description: String? = null): Marker? {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))
        return map?.addMarker(
            MarkerOptions()
                .title(title)
                .snippet(description)
                .draggable(true)
                .position(latLng)
        )
    }

    private fun sendMarkerLocationResultBack() {
        val resultIntent = Intent()
        resultIntent.putExtra(Constants.EVENT_LOCATION, marker!!.position)
        setResult(Activity.RESULT_OK, resultIntent)
    }
}