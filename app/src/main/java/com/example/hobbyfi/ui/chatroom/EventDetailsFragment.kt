package com.example.hobbyfi.ui.chatroom

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.EventDetailsFragmentBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.combineWith
import com.example.hobbyfi.viewmodels.chatroom.EventDetailsFragmentViewModel
import com.example.hobbyfi.viewmodels.factories.EventViewModelFactory
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.isConnected
import com.example.hobbyfi.shared.setParamsBasedOnScreenOrientation
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.UserGeoPointState
import com.example.hobbyfi.ui.base.DeviceRotationViewAware
import com.example.hobbyfi.ui.base.MapsActivity
import com.example.hobbyfi.ui.custom.EventCalendarDecorator
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.GeoPoint
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalCoroutinesApi
class EventDetailsFragment : ChatroomModelFragment(), DeviceRotationViewAware {
    // TODO: Abstract DeviceRotationViewAware impl to higher fragments and handle rotation for all views

    private val viewModel: EventDetailsFragmentViewModel by viewModels(factoryProducer = {
        EventViewModelFactory(
            requireActivity().application,
            args.event
        )
    })

    private val args: EventDetailsFragmentArgs by navArgs()

    private var map: GoogleMap? = null // TODO: Config change perseverance?
    private lateinit var binding: EventDetailsFragmentBinding

    private val usersSource: LiveData<List<User>> by lazy {
        viewModel.userGeoPoints?.let {
            activityViewModel.chatroomUsers.combineWith(it) {
                    users: List<User>?, geoPoints: List<UserGeoPoint>? ->
                if(users == null || geoPoints == null) {
                    Log.i("EventDetailsFragment", "usersSource by lazy: users -> $users; geoPoints: $geoPoints. One of them is null => Sending empty list for user event sources")
                    return@combineWith emptyList<User>()
                }

                val geoPointUsernames = geoPoints.map { gp -> gp.username }
                Log.i("EventDetailsFragment", "GeoPoints: ${geoPointUsernames}")
                Log.i("EventDetailsFragment", "users: ${users}")
                return@combineWith users.filter { user -> geoPointUsernames.contains(user.name) }
            }
        }
        Log.i("EventDetailsFragment", "UserGeoPoints are null => returning empty list")
        return@lazy activityViewModel.chatroomUsers.map { arrayListOf() }
    }

    private val userListAdapter: ChatroomUserListAdapter by lazy {
        ChatroomUserListAdapter(
            usersSource.value ?: arrayListOf(),
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
            lifecycleOwner = this@EventDetailsFragment

            initMap(savedInstanceState)
            initEventButtons()
            initCalendar()
            calculateEventDayDifference()
            initUserList()
            observeEvent()
            observeUserGeoPointState()

            if(viewModel!!.userGeoPoints == null || viewModel!!.userGeoPoints?.value == null
                    || viewModel!!.userGeoPoints?.value?.isEmpty() == true) {
                lifecycleScope.launch {
                    viewModel!!.sendIntent(
                        UserGeoPointIntent.FetchUsersGeoPoints(activityViewModel.authUserGeoPoint.value?.username)
                    )
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

        val marker = MarkerOptions().position(eventLatLng)
            .title(viewModel.relatedEvent.name)
            .snippet(viewModel.relatedEvent.description)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
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
            dateRangeCalendar.selectionMode = MaterialCalendarView.SELECTION_MODE_NONE

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
        lifecycleScope.launch {
            activityViewModel.authUserGeoPoint.collectLatest { authUserGeoPoint ->
                with(binding.eventViewButtonBar) {
                    leftButton.setOnClickListener {
                        authUserGeoPoint?.let { // user should NEVER leave event without a predefined user point
                            lifecycleScope.launch {
                                val (username, chatroomIds, eventIds, geoPoint) = it
                                viewModel.sendIntent(
                                    UserGeoPointIntent.UpdateUserGeoPoint(
                                        username,
                                        chatroomIds.filter { id -> id != activityViewModel.authChatroom.value!!.id },
                                        eventIds.filter { id -> id != viewModel.relatedEvent.id },
                                        geoPoint
                                    )
                                )
                            }
                        }
                    }
                    val userInEvent = authUserGeoPoint != null &&
                            authUserGeoPoint.eventIds.contains(viewModel.relatedEvent.id)
                    leftButton.isVisible = userInEvent
                    rightButton.setOnClickListener {
                        lifecycleScope.launch innerLaunch@ {
                            if(calculateEventDayDifference()) {
                                Toast.makeText(requireContext(), Constants.eventAlreadyConcluded, Toast.LENGTH_LONG)
                                    .show()
                                return@innerLaunch
                            }

                            if(!connectivityManager.isConnected()) {
                                Toast.makeText(requireContext(), Constants.noConnectionError, Toast.LENGTH_LONG)
                                    .show()
                                return@innerLaunch
                            }

                            if(!userInEvent) {
                                viewModel.sendIntent(
                                    UserGeoPointIntent.UpdateUserGeoPoint(
                                        authUserGeoPoint?.username
                                            ?: activityViewModel.authUser.value!!.name,
                                        (if(authUserGeoPoint?.chatroomIds?.contains(activityViewModel.authChatroom.value!!.id) == true) authUserGeoPoint.chatroomIds
                                                    else authUserGeoPoint?.chatroomIds?.plus(activityViewModel.authChatroom.value!!.id))
                                            ?: listOf(activityViewModel.authChatroom.value!!.id),
                                        (if(authUserGeoPoint?.eventIds?.contains(viewModel.relatedEvent.id) == true) authUserGeoPoint.eventIds
                                                    else authUserGeoPoint?.eventIds?.plus(viewModel.relatedEvent.id))
                                            ?: listOf(viewModel.relatedEvent.id),
                                        authUserGeoPoint?.geoPoint ?: GeoPoint(0.0, 0.0) // default coords
                                    )
                                )
                            } else {
                                navigateToEventMaps(activityViewModel.authUserGeoPoint.value!!) // user should NEVER not have event at this point here
                            }
                        }
                    }
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

    private fun calculateEventDayDifference(): Boolean {
        var diff = viewModel.relatedEvent.calculateDateDiff()
        if(diff.isNegative) {
            binding.eventViewButtonBar.leftButton.isVisible = false
            binding.eventViewButtonBar.rightButton.isVisible = false
            binding.daysLeftHeader.text = String.format(
                Locale.ENGLISH, "Event has already concluded.")
            return true
        }

        binding.daysLeftHeader.text = String.format(
            Locale.ENGLISH, "%d days and %d hours left", diff.toDays(), diff.toHours()
        )
        return false
    }

    private fun observeEventUsers(users: LiveData<List<UserGeoPoint>>) {
        users.combineWith(activityViewModel.chatroomUsers) { geoPoints: List<UserGeoPoint>?, chatroomUsers: List<User>? ->
            if(chatroomUsers == null || geoPoints == null) {
                Log.i("EventDetailsFragment", "usersSource by lazy: users -> $chatroomUsers; geoPoints: $geoPoints. One of them is null => Sending empty list for user event sources")
                return@combineWith emptyList<User>()
            }

            val geoPointUsernames = geoPoints.map { gp -> gp.username }
            Log.i("EventDetailsFragment", "GeoPoints: ${geoPointUsernames}")
            Log.i("EventDetailsFragment", "users: ${chatroomUsers}")
            return@combineWith chatroomUsers.filter {
                    user -> geoPointUsernames.contains(user.name) &&
                        user.name != activityViewModel.authUserGeoPoint.value?.username
            }
        }.observe(viewLifecycleOwner, Observer {
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
                    binding.executePendingBindings()
                    it.photoUrl?.let { photoUrl ->
                        Glide.with(requireContext())
                            .load(photoUrl)
                            .placeholder(binding.eventImage.drawable)
                            .signature(ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_events_fetch_time))) // TODO: Change
                            .into(binding.eventImage)
                    }
                    setCalendarDateData()
                    map?.let { m -> setMapsData(m) }
                }
        })
    }

    private fun observeUserGeoPointState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collectLatest {
                when(it) {
                    is UserGeoPointState.Idle -> {

                    }
                    is UserGeoPointState.Loading -> {
                        // TODO: Progressbar
                    }
                    is UserGeoPointState.OnData.OnUserGeoPointSetResult -> {
                        // after every update, the snapshotlistener is triggered and the new geoPoint is received in activiy VM
                        // therefore, with each geoPoint update here the activity's geoPoint is updated
                        it.setUserGeoPoint.observe(viewLifecycleOwner, Observer { geoPoint ->
                            if(geoPoint != null && geoPoint.eventIds.contains(viewModel.relatedEvent.id)) {
                                // user joined event
                                // since it's impossible to change the geopoint inside eventMapsActivity,
                                // just sending the geoPoint as it is will suffice (only user location is changed)
                                // and even when the user backs from maps activity, the snapshot listener OUGHT to be trigger again
                                // and the location—become up to date
                                navigateToEventMaps(geoPoint)
                            } else {
                                // user left event
                                navController.popBackStack()
                            }
                        })
                    }
                    is UserGeoPointState.OnData.OnUsersGeoPointsResult -> {
                        observeEventUsers(it.userGeoPoints)
                    }
                    is UserGeoPointState.Error -> {
                        Toast.makeText(requireContext(), it.error, LENGTH_LONG)
                            .show()
                        if(it.shouldReauth) {
                            navController.popBackStack()
                        }
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == Constants.eventMapsRequestCode) {
            when(resultCode) {
                Constants.RESULT_CHATROOM_DELETE -> {
                    Log.i("EventDetailsFragment", "NOTIFICATION FOR DELETE TRIGGERED ONACTIVITYRESULT FOR RESULT_CHATROOM_DELETE! CHECK BACKSTACK!")
                    lifecycleScope.launchWhenResumed {
                        activityViewModel.sendChatroomIntent(
                            ChatroomIntent.DeleteChatroomCache
                        )
                    }
                }
                RESULT_OK -> {
                    // refresh data on return from EventMapsActivity
                    // FIXME: On terms of scalability, this is a hacky solution and a better one would be to catch the service notifications
                    // FIXME: & launch them deferred when user reaches this point
                    // FIXME: Probably through some kind of a sharedprefs/livedata observer on a bool
                    (requireActivity() as ChatroomActivity).refreshDataOnConnectionRefresh()
                }
                RESULT_CANCELED -> {
                    Log.i("EventDetailsFragment", "NOTIFICATION FOR DELETE TRIGGERED ONACTIVITYRESULT FOR RESULT_CANCELLED! CHECK BACKSTACK!")
                    Toast.makeText(requireContext(), Constants.eventDeleted, LENGTH_LONG)
                        .show()
                    navController.popBackStack()
                }
            }
        }
    }

    private fun navigateToEventMaps(geoPoint: UserGeoPoint) {
        Intent(requireContext(), EventMapsActivity::class.java).apply {
            putExtra(Constants.EVENT, viewModel.relatedEvent)
            putExtra(Constants.USER_GEO_POINT, geoPoint)
        }.run {
            startActivityForResult(this, Constants.eventMapsRequestCode)
        }
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
        map?.let {
            setMapsData(it)
        }
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