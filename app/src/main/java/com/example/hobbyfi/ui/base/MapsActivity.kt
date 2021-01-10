package com.example.hobbyfi.ui.base

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import pub.devrel.easypermissions.EasyPermissions

abstract class MapsActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {
    protected var map: GoogleMap? = null
    protected lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    protected val defaultLocation = LatLng(-33.8523341, 151.2106085)

    protected var locationPermissionGranted = false

    // location retrieved by the Fused Location Provider.
    protected var lastKnownLocation: Location? = null

    protected fun getLocationPermission() {
        locationPermissionGranted = Callbacks.requestLocationForEventCreate(
            this
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        locationPermissionGranted = requestCode == Constants.locationPermissionsRequestCode
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // TODO: Toast or w/e in ChooseLocation and onBackPressed (?maybe?) in EventMapsActivity
    }
}