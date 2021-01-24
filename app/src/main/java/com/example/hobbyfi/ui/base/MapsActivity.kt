package com.example.hobbyfi.ui.base

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.chatroom.EventChooseLocationMapsActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import pub.devrel.easypermissions.EasyPermissions

abstract class MapsActivity : AppCompatActivity(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {
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
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onMapReady(gMap: GoogleMap) {
        map = gMap

        updateLocationUI()
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

    protected fun moveMarkerAndCamera(latLng: LatLng, title: String?, description: String?): Marker? {
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))
        return map?.addMarker(
            MarkerOptions()
            .title(title)
            .snippet(description)
            .draggable(true)
            .position(latLng)
        )
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        locationPermissionGranted = perms.contains(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // TODO: Toast or w/e in ChooseLocation and onBackPressed (?maybe?) in EventMapsActivity
    }

    companion object {
        const val DEFAULT_ZOOM = 15
    }
}