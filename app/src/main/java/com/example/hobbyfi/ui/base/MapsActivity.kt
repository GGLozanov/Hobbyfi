package com.example.hobbyfi.ui.base

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.hobbyfi.shared.Callbacks
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint
import pub.devrel.easypermissions.EasyPermissions

abstract class MapsActivity : BaseActivity(), OnMapReadyCallback,
        EasyPermissions.PermissionCallbacks {
    protected var map: GoogleMap? = null
    protected lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    protected val defaultLocation = LatLng(-33.8523341, 151.2106085)

    protected var locationPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    protected fun getLocationPermission() {
        locationPermissionGranted = Callbacks.requestLocationForMapsAccess(
            this
        )
    }

    override fun onMapReady(gMap: GoogleMap) {
        map = gMap
        updateLocationUI()
    }

    protected fun checkAndUpdateLocationPermission(): Boolean {
        locationPermissionGranted =
            EasyPermissions.hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return locationPermissionGranted
    }

    protected open fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            map?.uiSettings?.isCompassEnabled = true

            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    // TODO: Use to add icon to marker
    protected fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorResId: Int
    ): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        locationPermissionGranted = perms.contains(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    companion object {
        const val DEFAULT_ZOOM = 15
    }
}