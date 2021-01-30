package com.example.hobbyfi.ui.chatroom

import android.content.*
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityEventMapsBinding
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.services.EventLocationUpdatesService
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.EventBroadcastReceiverFactory
import com.example.hobbyfi.shared.animateMarker
import com.example.hobbyfi.shared.buildYesNoAlertDialog
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.ui.base.MapsActivity
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class EventMapsActivity : MapsActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val viewModel: EventMapsActivityViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(
            application,
            intent.getParcelableExtra(Constants.EVENT)!!
        )
    })

    private lateinit var binding: ActivityEventMapsBinding

    // TODO: Handle DELETE_CHATROOM and check if this triggers other notifications of same type (bad)
    // sync here
    private var deleteEventReceiver: BroadcastReceiver? = null
    private var editEventReceiver: BroadcastReceiver? = null
    private var deleteEventBatchReceiver: BroadcastReceiver? = null
    private var eventReceiverFactory: EventBroadcastReceiverFactory? = null

    private val locationUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // i.e. just update authUserGeo point in backend and have the snapshot listener re-trigger
            // and after observing the authUserGeoPoint and transforming it through map, display the updated marker
            // (like it'll be done with the rest of the userGeoPoints)
            if(intent.action == Constants.UPDATED_LOCATION_ACTION) {
                Log.i(
                    "EventMapsActivity",
                    "Received location: ${intent.extras?.get(Constants.UPDATED_LOCATION)}"
                )

                val receivedLocation = intent.extras?.get(Constants.UPDATED_LOCATION) as Location

                val initialGeoPoint = this@EventMapsActivity.intent.extras!![Constants.USER_GEO_POINT] as UserGeoPoint
                // GeoPoint HERE is immutable (in this activity),
                // which means it's safe to use the one received from EventDetailsFragment
                lifecycleScope.launch {
                    viewModel.sendIntent(
                        UserGeoPointIntent.UpdateUserGeoPoint(
                            initialGeoPoint.username,
                            initialGeoPoint.chatroomIds,
                            initialGeoPoint.eventIds,
                            GeoPoint(receivedLocation.latitude, receivedLocation.longitude)
                        )
                    )
                }
            } else {
                Log.e(
                    "EventMapsActivity",
                    "locationUpdateReceiver called with wrong intent action!"
                )
            }
        }
    }

    private var locationUpdatesService: EventLocationUpdatesService? = null
    private var serviceBound: Boolean = false

    private val locationServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            locationUpdatesService = (binder as EventLocationUpdatesService.LocalBinder)
                .service
            serviceBound = true

            if(prefConfig.readRequestingLocationUpdates()) {
                enableLocationUpdates() // start the service for location tracking if applicable
            } else {
                locationUpdatesService?.removeLocationUpdates()
            }

            setFABState(prefConfig.readRequestingLocationUpdates())
        }

        override fun onServiceDisconnected(name: ComponentName) {
            locationUpdatesService = null
            serviceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(viewModel.initialStart) {
            prefConfig.writeRequestingLocationUpdates(false)
            // initialStart is useless now but due to feedback from friend, location updates are NOT initially enabled
            viewModel.setInitialStart(false)
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if(prefConfig.readRequestingLocationUpdates()) {
            if(!checkAndUpdateLocationPermission()) {
                getLocationPermission()
            } else {
                if(viewModel.userGeoPoints == null || viewModel.userGeoPoints?.value == null
                        || viewModel.userGeoPoints?.value?.isEmpty() == true) {
                    lifecycleScope.launch {
                        viewModel.sendIntent(
                            UserGeoPointIntent.FetchUsersGeoPoints(getUserGeoPointFromCurrentIntent().username)
                        )
                    }
                }
            }
        }

        observeEventListState()
    }

    override fun onMapReady(gMap: GoogleMap) {
        super.onMapReady(gMap)
        observeEvent()
        viewModel.forceEventObservation()
        observeUserGeoPointsState()
        viewModel.forceUserGeoPointsObservation()

        map?.setOnMyLocationButtonClickListener {
            viewModel.lastReceivedLocation?.let {
                map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    it, DEFAULT_ZOOM.toFloat()
                ))
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        prefConfig.registerPrefsListener(this)

        binding.disableLocationUpdatesFab.setOnClickListener {
            locationUpdatesService?.removeLocationUpdates()
            Toast.makeText(this, "Location updates disabled!", Toast.LENGTH_LONG)
                .show()
        }

        binding.enableLocationUpdatesFab.setOnClickListener {
            if(enableLocationUpdates()) {
                Toast.makeText(this, "Location updates enabled!", Toast.LENGTH_LONG)
                    .show()
            }
        }
        setFABState(prefConfig.readRequestingLocationUpdates())

        bindService(
            Intent(this, EventLocationUpdatesService::class.java),
            locationServiceConnection,
            BIND_AUTO_CREATE
        )
    }

    private fun observeEventListState() {
        lifecycleScope.launchWhenCreated {
            viewModel.eventsState.collect {
                when(it) {
                    is EventListState.Idle, is EventListState.Loading -> {

                    }
                    is EventListState.OnData.DeleteEventsCacheResult -> {
                        if (it.eventIds.contains(viewModel.event.value?.id)) {
                            emergencyActivityExit()
                        }
                    }
                    is EventListState.OnData.DeleteAnEventCacheResult -> {
                        if (viewModel.event.value?.id == it.eventId) {
                            emergencyActivityExit()
                        }
                    }
                    is EventListState.Error -> {
                        val (error, shouldReauth) = it
                        handleStateError(error, shouldReauth)
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    private fun observeUserGeoPointsState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {
                    is UserGeoPointState.Idle -> {

                    }
                    is UserGeoPointState.Loading -> {

                    }
                    is UserGeoPointState.OnData.OnUsersGeoPointsResult -> {
                        Log.i("EventMapsActivity",
                            "Event maps activity OnUsersGeoPointsResult: ${it.userGeoPoints}")
                        observeUserGeoPoints(it.userGeoPoints)
                        viewModel.forceUserGeoPointsObservation()
                    }
                    is UserGeoPointState.OnData.OnUserGeoPointSetResult -> {
                        it.setUserGeoPoint.observe(this@EventMapsActivity, Observer { geoPoint ->
                            if (geoPoint != null) {
                                // viewModel.updateNewGeoPointInList(geoPoint) -> don't add geopoint to other users list
                                val location = LatLng(geoPoint.geoPoint.latitude, geoPoint.geoPoint.longitude)
                                if(viewModel.lastReceivedLocation == null) {
                                    map?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            location,
                                            DEFAULT_ZOOM.toFloat()
                                        )
                                    )
                                }
                                viewModel.setLastReceivedLocation(location)
                            } else {
                                viewModel.setUserGeoPoints(emptyList())
                            }
                        })
                    }
                    is UserGeoPointState.Error -> {
                        val (error, shouldReauth) = it
                        handleStateError(error, shouldReauth)
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    private fun observeUserGeoPoints(users: LiveData<List<UserGeoPoint>>) {
        // hacky magic number fix for default values on the equator
        // Heh, I mean what are the *chances* of anyone picking a location there and it not appearing?!
        // A lot. A lot. This does need to be fixed eventually.
        users.map {
            it.filterNot { gp -> gp.geoPoint.latitude.equals(0.0) && gp.geoPoint.longitude.equals(0.0) }
        }.observe(this, Observer {
            Log.i("EventMapsActivity", "User geo points: $it")
            if(viewModel.userMarkers == null) {
                val markers = mutableListOf<Marker>()
                it.forEach { geoPoint ->
                    markers.add(addGeoPointMarker(geoPoint)!!)
                }
                viewModel.setUserMarkers(markers)
                return@Observer
            }

            val containedGeoPoints = it.filter { geoPoint ->
                viewModel.userMarkers!!.any { marker -> marker.title == geoPoint.username }
            }
            val animMarkersWithNewPos = it.mapNotNull { geoPoint ->
                viewModel.userMarkers!!.find { marker -> marker.title == geoPoint.username }
            }.associateWith { marker ->
                val gp = it.find { geoPoint -> geoPoint.username == marker.title }!!
                LatLng(gp.geoPoint.latitude, gp.geoPoint.longitude)
            } // animate markers map with their latlngs
            val newGeoPoints = it - containedGeoPoints // add marker list
            val removeMarkersList = viewModel.userMarkers!!.filterNot { marker ->
                it.any { geoPoint -> geoPoint.username == marker.title }
            } // remove marker list

            animMarkersWithNewPos.forEach { (marker, latLng) ->
                marker.animateMarker(latLng)
            } // animate old geo point markers

            val newMarkers = mutableListOf<Marker>()
            newGeoPoints.forEach { geoPoint ->
                newMarkers.add(addGeoPointMarker(geoPoint)!!)
            } // add new geo point markers
            viewModel.setUserMarkers(viewModel.userMarkers!! + newMarkers)

            removeMarkersList.forEach { rmMarker ->
                rmMarker.remove()
            } // remove markers

            viewModel.setUserMarkers(viewModel.userMarkers!! - removeMarkersList)
        })
    }

    private fun observeEvent() {
        viewModel.event.observe(this, Observer {
            Log.i("EventMapsActivity", "Received Event from observer in EventMapsActivity! $it")
            resetEventMarkerAndAddNew(
                LatLng(it.latitude, it.longitude),
                it.name,
                it.description,
                false
            )
        })
    }

    override fun updateLocationUI() {
        super.updateLocationUI()
        map?.setOnMyLocationButtonClickListener {
            viewModel.userGeoPoints?.value?.find {
                it.username == getUserGeoPointFromCurrentIntent().username
            }?.let {
                map?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.geoPoint.latitude, it.geoPoint.longitude),
                        DEFAULT_ZOOM.toFloat()
                    )
                )
            }
            true
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        super.onPermissionsGranted(requestCode, perms)
        if(!locationPermissionGranted) {
            onPermissionsDenied(requestCode, perms) // safeguard
            return
        }

        if(viewModel.userGeoPoints?.value == null) {
            lifecycleScope.launch {
                viewModel.sendIntent(
                    UserGeoPointIntent.FetchUsersGeoPoints(getUserGeoPointFromCurrentIntent().username)
                )
            }
        }

        updateLocationUI()
        locationUpdatesService?.requestLocationUpdates(
            viewModel.event.value!!, getUserGeoPointFromCurrentIntent()
        )
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        setFABState(false)
        val onDialogCancel = { dialogInterface: DialogInterface, _: Int ->
            Log.i(
                "EventMapsActivity",
                "User has denied location permissions. Exiting from Activity!"
            )
            Toast.makeText(this, Constants.requiredPermissionsDeniedError, Toast.LENGTH_LONG)
                .show()
            dialogInterface.dismiss()
            emergencyActivityExit(RESULT_OK)
        }
        buildYesNoAlertDialog(
            getString(R.string.location_monitor_permission_explanation),
            { dialogInterface: DialogInterface, _: Int ->
                val intent = Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri: Uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID, null
                    ) // get app URI and send its data in order to render proper settings page
                    data = uri
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK // start as new task
                    // (trigger onPause and trigger onStart once user comes back)
                }

                startActivity(intent)
                dialogInterface.dismiss()
            },
            onDialogCancel,
            {
                onDialogCancel(it, DialogInterface.BUTTON_NEGATIVE)
            }
        )
    }

    override fun onResume() {
        super.onResume()
        prefConfig.writeRequestLocationServiceRunning(false)
        eventReceiverFactory = EventBroadcastReceiverFactory.getInstance(
            viewModel, this
        )
        deleteEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_TYPE)
        deleteEventBatchReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_BATCH_TYPE)
        editEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.EDIT_EVENT_TYPE)

        with(localBroadcastManager) {
            registerReceiver(
                locationUpdateReceiver,
                IntentFilter(Constants.UPDATED_LOCATION_ACTION)
            )
            registerReceiver(deleteEventReceiver!!, IntentFilter(Constants.DELETE_EVENT_TYPE))
            registerReceiver(editEventReceiver!!, IntentFilter(Constants.EDIT_EVENT_TYPE))
            registerReceiver(deleteEventBatchReceiver!!, IntentFilter(Constants.DELETE_EVENT_BATCH_TYPE))
        }

        setFABState(prefConfig.readRequestingLocationUpdates())
    }

    override fun onPause() {
        super.onPause()
        prefConfig.writeRequestLocationServiceRunning(true)
        with(localBroadcastManager) {
            unregisterReceiver(locationUpdateReceiver)
            unregisterReceiver(deleteEventReceiver!!)
            unregisterReceiver(editEventReceiver!!)
            unregisterReceiver(deleteEventBatchReceiver!!)
        }
    }

    override fun onStop() {
        unbindServiceIfBound()
        prefConfig
            .unregisterPrefsListener(this)
        viewModel.setUserMarkers(null)
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        viewModel.userGeoPoints?.let {
            observeUserGeoPoints(it)
        }
    }

    override fun onBackPressed() {
        unbindServiceIfBound()
        locationUpdatesService?.removeLocationUpdates()
        setResult(RESULT_OK)
        super.onBackPressed()
    }

    private fun unbindServiceIfBound() {
        if(serviceBound) {
            unbindService(locationServiceConnection) // promote to foreground service by unbinding
            serviceBound = false
        }
    }

    override fun onSharedPreferenceChanged(sharedPrefs: SharedPreferences, key: String) {
        if (key == getString(R.string.pref_requesting_location_updates)) {
            setFABState(prefConfig.readRequestingLocationUpdates())
        }
    }

    private fun setFABState(requestingUpdates: Boolean) {
        with(binding) {
            enableLocationUpdatesFab.isEnabled = !requestingUpdates
            disableLocationUpdatesFab.isEnabled = requestingUpdates
        }
    }

    private fun handleStateError(error: String?, shouldReauth: Boolean) {
        Toast.makeText(this@EventMapsActivity, error, Toast.LENGTH_LONG)
            .show()
        if(shouldReauth) {
            emergencyActivityExit(RESULT_OK)
        }
    }

    private fun addGeoPointMarker(geoPoint: UserGeoPoint) = map?.addMarker(
            MarkerOptions()
                .title(geoPoint.username)
                .position(LatLng(geoPoint.geoPoint.latitude, geoPoint.geoPoint.longitude))
        )

    private fun emergencyActivityExit(result: Int = RESULT_CANCELED) {
        setResult(result)
        finish()
    }

    private fun enableLocationUpdates(): Boolean = if(!checkAndUpdateLocationPermission()) {
            getLocationPermission()
            false
        } else {
            locationUpdatesService?.requestLocationUpdates(
                viewModel.event.value!!, getUserGeoPointFromCurrentIntent()
            )
            true
        }

    private fun getUserGeoPointFromCurrentIntent(): UserGeoPoint =
        intent.extras!![Constants.USER_GEO_POINT] as UserGeoPoint

    override fun onDestroy() {
        super.onDestroy()
        prefConfig.writeRequestLocationServiceRunning(true)
        viewModel.setUserMarkers(null)
    }
}