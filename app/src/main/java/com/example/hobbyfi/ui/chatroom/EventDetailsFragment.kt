package com.example.hobbyfi.ui.chatroom

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.EventDetailsFragmentBinding
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.combineWith
import com.example.hobbyfi.viewmodels.chatroom.EventDetailsViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import com.example.hobbyfi.models.User
import com.example.hobbyfi.ui.base.MapsActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.SELECTION_MODE_NONE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@ExperimentalCoroutinesApi
class EventDetailsFragment : ChatroomModelFragment() {
    // TODO: Add users subscribed users recyclerview (geoPoint fetch from viewModel for given event)
    // TODO: Add join/leave buttons depending on usergeopoint contains event id

    private val viewModel: EventDetailsViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(
            requireActivity().application,
            requireArguments()[Constants.EVENT] as Event
        )
    })

    private lateinit var binding: EventDetailsFragmentBinding

    private val usersSource: LiveData<List<User>> by lazy {
        activityViewModel.chatroomUsers.combineWith(viewModel.userGeoPoints) {
            users: List<User>, geoPoints: List<UserGeoPoint> ->
        val geoPointUsernames = geoPoints.map { gp -> gp.username }
        users.filter { user -> geoPointUsernames.contains(user.name) }
    } }

    private val userListAdapter: ChatroomUserListAdapter by lazy {
        ChatroomUserListAdapter(
            usersSource.value ?: emptyList(),
            null
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.event_details_fragment,
            container,
            false
        )
        binding.viewModel = viewModel

        with(binding) {
            initMap(savedInstanceState)
            initCalendar()
            calculateEventDayDifference()

            observeEventUsers()

            // TODO: startActivityForResult + intent extra parcelable event
            // TODO: Also pass List<UserGeoPoint> as data for initial maps activity view (then continue fetching?)

            return@onCreateView root
        }
    }

    private fun initMap(savedInstanceState: Bundle?) {
        with(binding) {
            mapPreview.onCreate(savedInstanceState)
            mapPreview.onResume() // necessary to display map immediately

            mapPreview.getMapAsync { map ->
                map.uiSettings.setAllGesturesEnabled(false)

                val eventLatLng = LatLng(
                    viewModel!!.relatedEvent.latitude, viewModel!!.relatedEvent.longitude
                )

                map.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, MapsActivity.DEFAULT_ZOOM.toFloat()))

                val marker = MarkerOptions().position(
                    eventLatLng
                ).title(viewModel!!.relatedEvent.name).snippet(viewModel!!.relatedEvent.description)

                map.addMarker(marker)
            }
        }
    }

    private fun initCalendar() {
        with(binding) {
            dateRangeCalendar.selectionMode = SELECTION_MODE_NONE

            dateRangeCalendar.selectRange(
                viewModel!!.relatedEvent.calendarDayFromStartDate,
                viewModel!!.relatedEvent.calendarDayFromDate
            )

            dateRangeCalendar.isPagingEnabled = false
            dateRangeCalendar.setAllowClickDaysOutsideCurrentMonth(false)
        }
    }

    private fun calculateEventDayDifference() {
        val diff = Duration.between(LocalDateTime.now(), viewModel.relatedEvent.localDateTimeFromDate)

        binding.daysLeftHeader.text = String.format(
            Locale.ENGLISH, "%d days and %d hours left", diff.toDays(), diff.toHours()
        )
    }

    private fun observeEventUsers() {
        usersSource.observe(viewLifecycleOwner, Observer {
            userListAdapter.setUsers(it)
        })
    }

    companion object {
        fun newInstance(event: Event): EventDetailsFragment {
            val instance = EventDetailsFragment()
            val args = Bundle()
            args.putParcelable(Constants.EVENT, event)
            instance.arguments = args

            return instance
        }
    }

    @ExperimentalCoroutinesApi
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val activity = requireActivity() as ChatroomActivity
        activity.title = viewModel.relatedEvent.name
        // TODO: Handle navdrawer
        // activity.enableNavDrawer(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO: If result == cancelled && activity request code => pop self from backstack
    }

    override fun onStart() {
        super.onStart()
        binding.mapPreview.onStart()
    }

    override fun onResume() {
        super.onResume()
        calculateEventDayDifference()
        binding.mapPreview.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapPreview.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapPreview.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapPreview.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapPreview.onLowMemory()
    }
}