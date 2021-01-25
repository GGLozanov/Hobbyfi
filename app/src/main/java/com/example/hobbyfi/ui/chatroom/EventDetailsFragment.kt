package com.example.hobbyfi.ui.chatroom

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.bumptech.glide.Glide
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.EventDetailsFragmentBinding
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.combineWith
import com.example.hobbyfi.viewmodels.chatroom.EventDetailsViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.setParamsBasedOnScreenOrientation
import com.example.hobbyfi.ui.base.DeviceRotationViewAware
import com.example.hobbyfi.ui.base.MapsActivity
import com.example.hobbyfi.ui.custom.EventCalendarDecorator
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.SELECTION_MODE_NONE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@ExperimentalCoroutinesApi
class EventDetailsFragment : ChatroomModelFragment(), DeviceRotationViewAware {
    // TODO: Abstract DeviceRotationViewAware impl to higher fragments and handle rotation for all views

    private val viewModel: EventDetailsViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(
            requireActivity().application,
            requireArguments()[Constants.EVENT] as Event
        )
    })

    private var map: GoogleMap? = null // TODO: Config change perseverance?
    private lateinit var binding: EventDetailsFragmentBinding

    private val usersSource: LiveData<List<User>> by lazy {
        activityViewModel.chatroomUsers.combineWith(viewModel.userGeoPoints) {
            users: List<User>?, geoPoints: List<UserGeoPoint>? ->
        if(users == null || geoPoints == null) {
            Log.i("EventDetailsFragment", "usersSource by lazy: users -> $users; geoPoints: $geoPoints. One of them is null => Sending empty list for user event sources")
            return@combineWith emptyList<User>()
        }

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
            initEventButtons()
            initMap(savedInstanceState)
            initCalendar()
            calculateEventDayDifference()
            initUserList()
            observeEvent()
            observeEventUsers()

            if(viewModel!!.userGeoPoints.value?.isEmpty() == true) {
                lifecycleScope.launch {
                    viewModel!!.sendIntent(UserGeoPointIntent.FetchUsersGeoPoints)
                }
            }

            return@onCreateView root
        }
    }

    private fun initMap(savedInstanceState: Bundle?) {
        with(binding) {
            mapPreview.onCreate(savedInstanceState)
            mapPreview.onResume() // necessary to display map immediately

            mapPreview.setParamsBasedOnScreenOrientation(
                requireActivity(),
                3,
                2,
                3,
                2
            )
            mapPreview.getMapAsync { map ->
                this@EventDetailsFragment.map = map
                map.uiSettings.setAllGesturesEnabled(false)

                setMapsData(map)
            }
        }
    }

    private fun setMapsData(map: GoogleMap) {
        val eventLatLng = LatLng(
            viewModel.relatedEvent.latitude, viewModel.relatedEvent.longitude
        )

        val marker = MarkerOptions().position(
            eventLatLng
        ).title(viewModel.relatedEvent.name).snippet(viewModel.relatedEvent.description)
        viewModel.removeAndSetLastMarker(map.addMarker(marker))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(eventLatLng, MapsActivity.DEFAULT_ZOOM.toFloat()))
    }

    private fun initCalendar() {
        with(binding) {
            dateRangeCalendar.setParamsBasedOnScreenOrientation(
                requireActivity(),
                3,
                3,
                2,
                3
            )
            dateRangeCalendar.selectionMode = SELECTION_MODE_NONE

            setCalendarDateData()
        }
    }

    private fun setCalendarDateData() {
        with(binding) {
            val startDate = viewModel!!.relatedEvent.calendarDayFromStartDate
            val date = viewModel!!.relatedEvent.calendarDayFromDate
            dateRangeCalendar.selectRange(
                startDate,
                date
            )

            dateRangeCalendar.state().edit()
                .setMinimumDate(startDate)
                .setMaximumDate(date)
                .commit()
            dateRangeCalendar.addDecorator(
                EventCalendarDecorator(
                    ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                    listOf(CalendarDay.today())
                )
            )
        }
    }

    private fun initEventButtons() {
        with(binding.eventViewButtonBar) {
            leftButton.setOnClickListener {
                activityViewModel.authUserGeoPoint.value?.let {
                    lifecycleScope.launch {
                        val (username, chatroomId, eventIds, geoPoint) = it
                        viewModel.sendIntent(
                            UserGeoPointIntent.UpdateUserGeoPoint(
                                username,
                                chatroomId,
                                eventIds.filter { id -> id != viewModel.relatedEvent.id },
                                geoPoint
                            )
                        )
                    }
                }
                parentFragmentManager.popBackStack()
            }
            leftButton.isVisible = activityViewModel.authUserGeoPoint.value != null
            rightButton.setOnClickListener {
                Intent(requireContext(), EventMapsActivity::class.java).apply {
                    putExtra(Constants.EVENT, viewModel.relatedEvent)
                }.run {
                    startActivityForResult(this, Constants.eventMapsRequestCode)
                }
            }
        }
    }

    private fun initUserList() {
        with(binding) {
            usersList.setParamsBasedOnScreenOrientation(
                requireActivity(),
                3,
                3,
                2,
                3
            )

            usersList.addItemDecoration(VerticalSpaceItemDecoration(10))
            usersList.adapter = userListAdapter
        }
    }

    private fun calculateEventDayDifference() {
        var diff = Duration.between(LocalDateTime.now(), viewModel.relatedEvent.localDateTimeFromDate)
        if(diff.isNegative) {
            binding.eventViewButtonBar.rightButton.isVisible = false
            diff = diff.minus(diff)
        }

        binding.daysLeftHeader.text = String.format(
            Locale.ENGLISH, "%d days and %d hours left", diff.toDays(), diff.toHours()
        )
    }

    private fun observeEventUsers() {
        usersSource.observe(viewLifecycleOwner, Observer {
            Log.i("EventDetailsFragment", "Users from users source: $it")
            binding.noUsersText.isVisible = it.isEmpty()
            binding.usersList.isVisible = it.isNotEmpty()

            userListAdapter.setUsers(it)
        })
    }

    private fun observeEvent() {
        activityViewModel.authEvents
            .map { it.find { ev -> ev.id == viewModel.relatedEvent.id } }
            .observe(viewLifecycleOwner, Observer {
                if(it != null) {
                    viewModel.setEvent(it)
                    Glide.with(requireContext())
                        .load(it.photoUrl)
                        .into(binding.eventImage)
                    setToolbarTitle()
                    setCalendarDateData()
                    map?.let { m -> setMapsData(m) }
                }
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

    override fun setViewParamsOnRotationChange() {
        with(binding) {
            // FIXME: Code dup by adding these as extension funcs scoped to the given views
            mapPreview.setParamsBasedOnScreenOrientation(
                requireActivity(),
                3,
                2,
                3,
                2
            )
            dateRangeCalendar.setParamsBasedOnScreenOrientation(
                requireActivity(),
                3,
                3,
                2,
                3
            )
            usersList.setParamsBasedOnScreenOrientation(
                requireActivity(),
                3,
                3,
                2,
                3
            )
        }
    }

    @ExperimentalCoroutinesApi
    override fun onAttach(context: Context) {
        super.onAttach(context)
        setToolbarTitle()
    }

    private fun setToolbarTitle() {
        val activity = requireActivity() as ChatroomActivity
        activity.title = resources.getString(R.string.event_details)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO: If result == cancelled && activity request code => pop self from backstack
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setViewParamsOnRotationChange()
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
        viewModel.removeAndSetLastMarker(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapPreview.onDestroy()
        viewModel.removeAndSetLastMarker(null)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapPreview.onLowMemory()
        viewModel.removeAndSetLastMarker(null)
    }
}