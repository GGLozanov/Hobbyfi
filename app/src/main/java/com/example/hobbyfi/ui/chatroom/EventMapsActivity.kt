package com.example.hobbyfi.ui.chatroom

import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.EventBroadcastReceiverFactory
import com.example.hobbyfi.state.EventListState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.MapsActivity
import com.example.hobbyfi.viewmodels.chatroom.EventMapsActivityViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

@ExperimentalCoroutinesApi
class EventMapsActivity : MapsActivity() {
    private val viewModel: EventMapsActivityViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(application, intent.getParcelableExtra(Constants.EVENT)!!)
    })

    // TODO: Do we even need deleteEventReceivers here ? ? ?
    // sync here
    private var deleteEventReceiver: BroadcastReceiver? = null
    private var editEventReceiver: BroadcastReceiver? = null
    private var deleteEventBatchReceiver: BroadcastReceiver? = null
    private var eventReceiverFactory: EventBroadcastReceiverFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        observeEventListState()
        observeUserGeoPoints()
    }

    private fun observeEventListState() {
        lifecycleScope.launchWhenCreated {
            viewModel.eventsState.collect {
                when(it) {
                    is EventListState.Idle -> {

                    }
                    is EventListState.Loading -> {
                    }
                    is EventListState.OnData.DeleteEventsCacheResult -> {
                        if(it.eventIds.contains(viewModel.event.value?.id)) {
                            emergencyActivityExit()
                        }
                    }
                    is EventListState.OnData.DeleteAnEventCacheResult -> {
                        if(viewModel.event.value?.id == it.eventId) {
                            emergencyActivityExit()
                        }
                    }
                    is EventListState.Error -> {
                        // TODO: Handle 'shouldReauth'/'shouldExit'
                        Toast.makeText(this@EventMapsActivity, it.error, Toast.LENGTH_LONG)
                            .show()
                        finish()
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    private fun observeUserGeoPoints() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {

                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        super.onMapReady(googleMap)
        
    }

    override fun onResume() {
        super.onResume()
        eventReceiverFactory = EventBroadcastReceiverFactory.getInstance(
            viewModel, this
        )
        deleteEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_TYPE)
        deleteEventBatchReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_BATCH_TYPE)
        editEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.EDIT_EVENT_TYPE)

        registerReceiver(deleteEventReceiver, IntentFilter(Constants.DELETE_EVENT_TYPE))
        registerReceiver(editEventReceiver, IntentFilter(Constants.EDIT_EVENT_TYPE))
        registerReceiver(deleteEventBatchReceiver, IntentFilter(Constants.DELETE_EVENT_BATCH_TYPE))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(deleteEventReceiver)
        unregisterReceiver(editEventReceiver)
        unregisterReceiver(deleteEventBatchReceiver)
    }

    private fun emergencyActivityExit() {
        setResult(RESULT_CANCELED)
        finish()
    }
}