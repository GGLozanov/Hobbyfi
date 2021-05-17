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
    protected var marker: Marker? = null // no need to stay in VM because of savedInstanceState mechanisms

    protected var map: GoogleMap? = null
    protected lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    // default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    protected val defaultLocation = LatLng(-33.8523341, 151.2106085)

    protected var locationPermissionGranted = false

    protected fun getLocationPermission() {
        locationPermissionGranted = Callbacks.requestLocationForMapsAccess(
            this
        )
    }

    override fun onMapReady(gMap: GoogleMap) {
        map = gMap
        checkAndUpdateLocationPermission()
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

    protected fun resetEventMarkerAndMoveToNew(
        latLng: LatLng,
        eventTitle: String?,
        eventDescription: String?,
        draggable: Boolean,
        predefDescriptorColour: Float = BitmapDescriptorFactory.HUE_CYAN
    ) {
        marker?.remove()
        marker = addMarkerAndAnimateCamera(
            latLng,
            eventTitle,
            eventDescription,
            draggable,
            predefDescriptorColour
        )
    }

    private fun addMarkerAndAnimateCamera(
        latLng: LatLng,
        title: String? = null,
        description: String? = null,
        draggable: Boolean = true,
        predefDescriptorColour: Float = BitmapDescriptorFactory.HUE_RED
    ): Marker? {
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM.toFloat()))
        return addMarker(
            latLng,
            title,
            description,
            draggable,
            predefDescriptorColour
        )
    }

    protected fun resetEventMarkerAndAddNew(
        latLng: LatLng,
        eventTitle: String? = null,
        eventDescription: String? = null,
        draggable: Boolean = true,
        predefDescriptorColour: Float = BitmapDescriptorFactory.HUE_CYAN
    ) {
        marker?.remove()
        marker = addMarker(latLng, eventTitle, eventDescription, draggable, predefDescriptorColour)
    }

    private fun addMarker(
        latLng: LatLng,
        title: String?,
        description: String?,
        draggable: Boolean,
        predefDescriptorColour: Float
    ): Marker? =
        map?.addMarker(
            MarkerOptions()
                .title(title)
                .snippet(description)
                .draggable(draggable)
                .icon(BitmapDescriptorFactory.defaultMarker(predefDescriptorColour))
                .position(latLng)
        )

    protected fun animateCameraToCurrentEventMarker() =
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(marker?.position, DEFAULT_ZOOM.toFloat()))

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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    companion object {
        const val DEFAULT_ZOOM = 15
    }
}