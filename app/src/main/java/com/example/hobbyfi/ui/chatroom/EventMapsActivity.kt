package com.example.hobbyfi.ui.chatroom

import android.content.*
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.activity.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityEventMapsBinding
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.models.data.UserGeoPoint
import com.example.hobbyfi.services.EventLocationUpdatesService
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.ui.base.*
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
class EventMapsActivity : MapsActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        ServerSocketAccessor, RefreshConnectionForegroundFCMReactivationListener {
    private val viewModel: EventMapsActivityViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(
            application,
            intent.getParcelableExtra(Constants.EVENT)!!
        )
    })

    private lateinit var binding: ActivityEventMapsBinding

    private var initialServerSocketConnect: Boolean = true

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

                // GeoPoint HERE is immutable (in this activity),
                // which means it's safe to use the one received from EventDetailsFragment
                sendLocationUpdateIntentWithInitialUserGeoPoint(receivedLocation.latitude, receivedLocation.longitude)
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

    private val authUserIdWithErrorHandle: Long? get() =
        try {
            prefConfig.getAuthUserIdFromToken()
        } catch(e: Exception) {
            showFailureToast(getString(R.string.reauth_error))
            emergencyActivityExit(RESULT_OK) // reauth will trigger after attempted fetch fail
            null
        }

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

    // FIXME: This, too, should probably be extracted in a specific interface w/ ChatroomActivity
    @Volatile
    private var sentJoinChatroomSocketEvent = false

    override val serverSocket: Socket? by lazy {
        initSocket()
    }
    override val emitterListenerFactory: EmitterListenerFactory by lazy {
        EmitterListenerFactory(this)
    }

    private val socketEventErrorFallback = { _: Exception ->
        showFailureToast(getString(R.string.socket_emission_fail))
        emergencyActivityExit(Constants.RESULT_REAUTH)
    }

    // FIXME: Code dup w/ ChatroomActivity
    private val editEventEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForEdit(
            { editFields ->
                lifecycleScope.launchWhenCreated {
                    viewModel.sendEventsIntent(
                        EventListIntent.UpdateAnEventCache(
                            editFields
                        )
                    )
                }
            },
            socketEventErrorFallback
        )
    }

    private val deleteEventEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForDelete(
            { id ->
                lifecycleScope.launchWhenCreated {
                    viewModel.sendEventsIntent(
                        EventListIntent.DeleteAnEventCache(
                            id
                        )
                    )
                }
            },
            socketEventErrorFallback
        )
    }

    private val deleteEventBatchEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForDeleteArray(
            { ids ->
                lifecycleScope.launchWhenCreated {
                    viewModel.sendEventsIntent(
                        EventListIntent.DeleteEventsCache(
                            ids
                        )
                    )
                }
            },
            socketEventErrorFallback,
            Constants.EVENT_IDS
        )
    }

    private val deleteChatroomEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForDelete(
            {
                emergencyActivityExit(Constants.RESULT_CHATROOM_DELETE, Intent(Constants.DELETE_CHATROOM_TYPE).apply {
                    putExtra(Constants.DELETED_MODEL_ID, it)
                })
            },
            socketEventErrorFallback
        )
    }

    private val userLeaveEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForCreate(
            ::User,
            { user ->
                if(user.id == authUserIdWithErrorHandle) {
                    emergencyActivityExit(Constants.RESULT_KICKED, Intent(Constants.LEAVE_USER_TYPE).apply {
                        putExtra(Constants.DELETED_MODEL_ID, user.id)
                    })
                } else {
                    Log.w(
                        "EventMapsActivity",
                        "leaveUserReceiver called with ID different from auth user..."
                    )
                }
            },
            errorFallback = socketEventErrorFallback
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventMapsBinding.inflate(layoutInflater)
        localBroadcastManager.registerReceiver(foregroundFCMReceiver, IntentFilter(Constants.FOREGROUND_REACTIVATION_ACTION))
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_container) as SupportMapFragment
        mapFragment.getMapAsync(this)

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

            if(viewModel.initialStart) {
                buildLocationTrackingDialog()
                // initialStart is useless now but due to feedback from friend, location updates are NOT initially enabled
                viewModel.setInitialStart(false)
            }
        }

        observeEventListState()
        observeConnectionRefresh(savedInstanceState, refreshConnectivityMonitor)
    }

    override fun onMapReady(gMap: GoogleMap) {
        super.onMapReady(gMap)
        observeEvent()
        viewModel.forceEventObservation()
        observeUserGeoPointsState()
        viewModel.forceUserGeoPointsObservation()

        map?.setOnMyLocationButtonClickListener {
            viewModel.lastReceivedLocation?.let {
                // TODO: Remove this and keep the direct null check
                if(it.latitude != 0.0 && it.longitude != 0.0) {
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        it, DEFAULT_ZOOM.toFloat()
                    ))
                }
            }
            true
        }
    }

    override fun onStart() {
        super.onStart()
        connectServerSocket()
        prefConfig.registerPrefsListener(this)

        with(binding) {
            disableLocationUpdatesFab.setOnClickListener {
                locationUpdatesService?.removeLocationUpdates()
                this@EventMapsActivity.showSecondaryColourBackgroundToast(getString(R.string.location_updates_disabled))
            }

            enableLocationUpdatesFab.setOnClickListener {
                if(enableLocationUpdates()) {
                    this@EventMapsActivity.showSecondaryColourBackgroundToast(getString(R.string.location_updates_enabled))
                }
            }

            resetLocationFab.setOnClickListener {
                if(prefConfig.readRequestingLocationUpdates()) {
                    showWarningToast(getString(R.string.no_update_reset_only))
                    return@setOnClickListener
                }

                if(viewModel.lastReceivedLocation != null) {
                    sendLocationUpdateIntentWithInitialUserGeoPoint(
                        0.0, 0.0
                    )
                }
            }

            goToEventLocationFab.setOnClickListener {
                animateCameraToCurrentEventMarker()
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
                    is UserGeoPointState.Idle, is UserGeoPointState.Loading -> {

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

                                // TODO: Replace with null check
                                if(geoPoint.geoPoint.latitude == 0.0 && geoPoint.geoPoint.longitude == 0.0) {
                                    showSuccessToast(getString(R.string.location_reset))
                                }
                            } else {
                                viewModel.setUserGeoPoints(arrayListOf())
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

            // FIXME: Highly unoptimised
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
            emitJoinChatroomEventOnEventObserve(it)
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
        buildLocationTrackingDialog()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        setFABState(false)
        val onDialogCancel = { dialogInterface: DialogInterface, _: Int ->
            Log.i(
                "EventMapsActivity",
                "User has denied location permissions. Remaining in Activity!"
            )
            dialogInterface.dismiss()
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

    override fun onConnectedServerSocketFail() {
        Log.w("EventMapsActivity", "Socket connection from current auth user for event maps activity failed!")
        if(!viewModel.shownSocketError) {
            viewModel.setShownSocketError(true)
            runOnUiThread {
                buildYesNoAlertDialog(getString(R.string.socket_connection_fail), { _: DialogInterface, _: Int ->
                    emergencyActivityExit()
                }, null)
            }
        }
    }

    override fun connectServerSocketListeners() {
        serverSocket?.on(Socket.EVENT_CONNECT) {
            if(!initialServerSocketConnect) {
                refreshDataOnConnectionRefresh()
            } else initialServerSocketConnect = false
        }

        serverSocket?.on(Socket.EVENT_DISCONNECT) {
            sentJoinChatroomSocketEvent = false
        }

        serverSocket?.on(Constants.LEAVE_USER_TYPE, userLeaveEmitterListener)
        serverSocket?.on(Constants.DELETE_CHATROOM_TYPE, deleteChatroomEmitterListener)
        serverSocket?.on(Constants.EDIT_EVENT_TYPE, editEventEmitterListener)
        serverSocket?.on(Constants.DELETE_EVENT_TYPE, deleteEventEmitterListener)
        serverSocket?.on(Constants.DELETE_EVENT_BATCH_TYPE, deleteEventBatchEmitterListener)
    }

    override fun disconnectServerSocketListeners() {
        sentJoinChatroomSocketEvent = false
    }

    override fun onResume() {
        super.onResume()
        if(!initialServerSocketConnect && serverSocket?.connected() == false) {
            connectServerSocket()
        }
        prefConfig.writeRequestLocationServiceRunning(false)
        localBroadcastManager.registerReceiver(locationUpdateReceiver, IntentFilter(Constants.UPDATED_LOCATION_ACTION))

        setFABState(prefConfig.readRequestingLocationUpdates())
    }

    override fun onPause() {
        super.onPause()
        disconnectServerSocket()
        prefConfig.writeRequestLocationServiceRunning(true)
        localBroadcastManager.unregisterReceiver(locationUpdateReceiver)
    }

    override fun onStop() {
        disconnectServerSocket()
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

    override fun observeConnectionRefresh(
        savedState: Bundle?,
        refreshConnectivityMonitor: RefreshConnectivityMonitor
    ) {
        super.observeConnectionRefresh(savedState, refreshConnectivityMonitor)
        refreshConnectivityMonitor.observe(this, Observer {
            if(it) {
                Log.i("EventMapsActivity", "EventMapsActivity CONNECTED")
                refreshDataOnConnectionRefresh()
            } else {
                Log.i("EventMapsActivity", "EventMapsActivity DIS-CONNECTED")
            }
        })
    }

    override fun refreshDataOnConnectionRefresh() {
        lifecycleScope.launch {
            viewModel.sendEventsIntent(
                EventListIntent.RefetchEvent
            )
        }
    }

    override fun onForegroundReactivation(intent: Intent) {
        when(intent.action) {
            Constants.DELETE_EVENT_TYPE -> {
                deleteEventEmitterListener.call(intent)
            }
            Constants.EDIT_EVENT_TYPE -> {
                editEventEmitterListener.call(intent)
            }
            Constants.DELETE_EVENT_BATCH_TYPE -> {
                deleteEventBatchEmitterListener.call(intent)
            }
            Constants.DELETE_CHATROOM_TYPE -> {
                deleteChatroomEmitterListener.call(intent)
            }
            Constants.LEAVE_USER_TYPE -> {
                userLeaveEmitterListener.call(intent)
            }
        }
    }

    private fun emitJoinChatroomEventOnEventObserve(event: Event) {
        if(!sentJoinChatroomSocketEvent) {
            Log.i("EventMapsActivity", "Emitting join_chatroom event!!!!")

            authUserIdWithErrorHandle?.let {
                serverSocket?.emit(Constants.JOIN_CHATROOM, JSONObject(mapOf(
                    Constants.ID to it,
                    Constants.CHATROOM_ID to event.chatroomId
                )))
                sentJoinChatroomSocketEvent = true
            }
        } else {
            Log.w("EventMapsActivity", "Not emitting join_chatroom event due to it already having been emitted")
        }
    }

    private fun setFABState(requestingUpdates: Boolean) {
        with(binding) {
            enableLocationUpdatesFab.isEnabled = !requestingUpdates
            disableLocationUpdatesFab.isEnabled = requestingUpdates
        }
    }

    private fun handleStateError(error: String?, shouldReauth: Boolean) {
        showFailureToast(error ?: getString(R.string.something_wrong))
        if(shouldReauth) {
            emergencyActivityExit(Constants.RESULT_REAUTH)
        }
    }

    private fun addGeoPointMarker(geoPoint: UserGeoPoint) = map?.addMarker(
            MarkerOptions()
                .title(geoPoint.username)
                .position(LatLng(geoPoint.geoPoint.latitude, geoPoint.geoPoint.longitude))
        )

    private fun emergencyActivityExit(result: Int = RESULT_CANCELED, intent: Intent? = null) {
        if(intent != null) setResult(result, intent) else setResult(result)
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

    private fun sendLocationUpdateIntentWithInitialUserGeoPoint(lat: Double, long: Double) {
        val initialGeoPoint = getUserGeoPointFromCurrentIntent()
        lifecycleScope.launch {
            viewModel.sendIntent(
                UserGeoPointIntent.UpdateUserGeoPoint(
                    initialGeoPoint.username,
                    initialGeoPoint.chatroomIds,
                    initialGeoPoint.eventIds,
                    GeoPoint(lat, long)
                )
            )
        }
    }

    private fun buildLocationTrackingDialog() {
        if(!checkAndUpdateLocationPermission()) {
            throw IllegalStateException(Constants.incorrectCallToBuildLocationTrackingDialog)
        }

        val onCancel = { dialogInterface: DialogInterface, _: Int ->
            animateCameraToCurrentEventMarker()
            dialogInterface.dismiss()
        }
        // TODO: Convert to Remember-No-RememberYes dialog
        buildYesNoAlertDialog(
            getString(R.string.location_monitor_ask),
            { dialogInterface: DialogInterface, _: Int ->
                locationUpdatesService?.requestLocationUpdates(
                    viewModel.event.value!!, getUserGeoPointFromCurrentIntent()
                )
                dialogInterface.dismiss()
            },
            onCancel,
            { dialogInterface: DialogInterface ->
                onCancel(dialogInterface, DialogInterface.BUTTON_NEGATIVE)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(foregroundFCMReceiver)
        prefConfig.writeRequestLocationServiceRunning(true)
        viewModel.setUserMarkers(null)
    }
}