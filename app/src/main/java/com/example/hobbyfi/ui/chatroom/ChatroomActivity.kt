package com.example.hobbyfi.ui.chatroom

import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.ExperimentalPagingApi
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagListAdapter
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.ActivityChatroomBinding
import com.example.hobbyfi.databinding.NavHeaderChatroomBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.state.*
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.google.android.gms.common.ConnectionResult.*
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.base.RefreshConnectionAware
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.*
import org.kodein.di.generic.instance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

@ExperimentalCoroutinesApi
class ChatroomActivity : BaseActivity(),
        ChatroomMessageBottomSheetDialogFragment.OnMessageOptionSelected, RefreshConnectionAware {
    private val viewModel: ChatroomActivityViewModel by viewModels(factoryProducer = {
        AuthUserChatroomViewModelFactory(application, args.user, args.chatroom)
    })

    private val fcmTopicErrorFallback: OnFailureListener by instance(tag = "fcmTopicErrorFallback", this)

    lateinit var binding: ActivityChatroomBinding
    private val args: ChatroomActivityArgs by navArgs()

    private lateinit var headerBinding: NavHeaderChatroomBinding

    private var userListAdapter: ChatroomUserListAdapter? = null

    private var chatroomReceiverFactory: ChatroomBroadcastReceiverFactory? = null
    private var editChatroomReceiver: BroadcastReceiver? = null
    private var deleteChatroomReceiver: BroadcastReceiver? = null
    private var editUserReceiver: BroadcastReceiver? = null
    private var joinUserReceiver: BroadcastReceiver? = null
    private var leaveUserReceiver: BroadcastReceiver? = null

    private var deleteBatchEventReceiver: BroadcastReceiver? = null
    private var deleteEventReceiver: BroadcastReceiver? = null
    private var editEventReceiver: BroadcastReceiver? = null
    private var createEventReceiver: BroadcastReceiver? = null
    private var eventReceiverFactory: EventBroadcastReceiverFactory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // checks if called from push notification while app is killed and restarts entire backstack in that case
        if(isTaskRoot) {
            Log.i("ChatroomActivity", "ChatroomActivity IS TASK ROOT. Regenerating parent activity backstack!")
            val restartIntent = Intent(this, ChatroomActivity::class.java)

            restartIntent.putExtras(intent)

            TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(restartIntent)
                .startActivities(intent.extras)

            finishAffinity()
            return
        }

        assertGooglePlayAvailability()

        binding.viewModel = viewModel

        headerBinding = NavHeaderChatroomBinding.bind(binding.navViewChatroom.getHeaderView(0))
        headerBinding.viewModel = viewModel

        if(viewModel.authUser.value == null) {
            // deeplink situation
            lifecycleScope.launch {
                viewModel.sendIntent(UserIntent.FetchUser)
            }
        }

        if(viewModel.authChatroom.value == null) {
            lifecycleScope.launch {
                viewModel.sendChatroomIntent(ChatroomIntent.FetchChatroom)
            }
        }

        userListAdapter = ChatroomUserListAdapter(
            viewModel.currentAdapterUsers.value ?: emptyList()
        ) { _: View, user: User ->
            val bottomSheet = ChatroomUserBottomSheetDialogFragment.newInstance(user)
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        with(binding) {
            usersList.addItemDecoration(VerticalSpaceItemDecoration(10))
            usersList.adapter = userListAdapter
            // TODO: Get GeoUser model from Cloud Firestore and observe. After fetch => set button visibility depending on user joinw

            initCalendar()
        }

        observeConnectionRefresh(savedInstanceState, refreshConnectivityMonitor)
    }

    @ExperimentalPagingApi
    override fun onStart() {
        super.onStart()

        binding.navViewAdmin.setupWithNavController(navController)

        // TODO: Register delete/update BroadcastReceive with User intents and Event intents
        // TODO: First fetch messages from back-end then register for receiving messages

        observeChatroomState()
        observeUsers()
        observeUserState()
        observeUsersState()
        observeEventsState()
        observeEvents()
        observeChatroom()
        observeChatroomOwnRights()
    }

    private fun observeUserState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {
                    is UserState.Idle -> {

                    }
                    is UserState.Loading -> {
                        // TODO: Progressbar
                    }
                    is UserState.OnData.UserResult -> {
                    }
                    is UserState.Error -> {
                        handleAuthActionableError(it.error + " USER", it.shouldReauth)
                    }
                    else -> throw State.InvalidStateException()
                }
                // no need for UserState.OnData.UserUpdateResult for null chatroom
                // because user gets nulled chatroom in backend when it's deleted
            }
        }
    }

    private fun observeChatroomState() {
        // whenever broadcast receiver triggered => sets the state in the viewmodel
        lifecycleScope.launchWhenCreated {
            viewModel.chatroomState.collect {
                when(it) {
                    is ChatroomState.Idle -> {

                    }
                    is ChatroomState.Loading -> {
                        // TODO: Loading
                    }
                    is ChatroomState.OnData.ChatroomResult -> {
                    }
                    is ChatroomState.OnData.ChatroomDeleteResult, is ChatroomState.OnData.DeleteChatroomCacheResult -> {
                        Toast.makeText(this@ChatroomActivity, "Successfully deleted chatroom!", Toast.LENGTH_LONG)
                            .show()
                        sendBroadcast(Intent(Constants.CHATROOM_DELETED)
                            .apply { putExtra(Constants.CHATROOM_ID, viewModel.authChatroom.value!!.id) })
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants
                            .chatroomTopic(viewModel.authChatroom.value!!.id)).addOnFailureListener(fcmTopicErrorFallback).continueWith {
                            viewModel.setChatroom(null) // clear chatroom in any case
                            finish()
                        }
                    }
                    is ChatroomState.OnData.ChatroomUpdateResult -> {
                        Toast.makeText(this@ChatroomActivity, "Successfully updated chatroom!", Toast.LENGTH_LONG)
                            .show()
                        viewModel.resetChatroomState()
                    }
                    is ChatroomState.Error -> {
                        handleAuthActionableError(it.error + " CHATROOM", it.shouldExit)
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    private fun observeUsers() {
        viewModel.currentAdapterUsers.observe(this, Observer {
            userListAdapter!!.setUsers(it)
        })
    }

    private fun observeUsersState() {
        lifecycleScope.launch {
            viewModel.usersState.collect {
                when(it) {
                   is UserListState.Idle -> {

                   }
                    is UserListState.Loading -> {
                        // TODO: Progressbar on RecyclerView
                    }
                    is UserListState.OnData.UsersResult -> {
                        viewModel.resetUserListState()
                    }
                    is UserListState.Error -> {
                        handleAuthActionableError(it.error + " USERLIST", it.shouldReauth)
                    }
                }
            }
        }
    }

    private fun observeEventsState() {
        lifecycleScope.launch {
            viewModel.eventsState.collect {
                when(it) {
                    is EventListState.Idle -> {

                    }
                    is EventListState.Loading -> {
                        // TODO: Progressbar on event card
                    }
                    is EventListState.OnData.EventsResult -> {
                        // TODO: Need to update here?
                    }
                    is EventListState.OnData.DeleteEventsCacheResult -> {

                    }
                    is EventListState.Error -> {
                        handleAuthActionableError(it.error + " EVENTLIST", it.shouldReauth)
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    private fun observeEvents() {
        viewModel.authEvents.observe(this, Observer {
            setEventCalendarDates(it)
        })
    }

    @ExperimentalPagingApi
    private fun observeChatroom() {
        viewModel.authChatroom.observe(this, Observer { chatroom ->
            if(chatroom != null) {
                if(supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
                    title = chatroom.name
                }

                val authUserChatroomOwner = viewModel.isAuthUserChatroomOwner.value == true
                val chatroomHasEvents = chatroom.eventIds != null
                val chatroomHasEventsToFetch = chatroomHasEvents && viewModel.authEvents.value == null

                if(chatroomHasEventsToFetch) {
                    lifecycleScope.launch {
                        viewModel.sendEventsIntent(
                            EventListIntent.FetchEvents
                        )
                    }
                }

                if(authUserChatroomOwner) {
                    with(binding) {
                        // TODO: On Chatroom deletes -> if no more events left, set authChatroom eventIds to  null

                        navViewAdmin.menu.findItem(R.id.action_delete_chatroom).setOnMenuItemClickListener {
                            Constants.buildDeleteAlertDialog(
                                this@ChatroomActivity,
                                Constants.confirmChatroomDeletionMessage,
                                { dialogInterface: DialogInterface, _: Int ->
                                    lifecycleScope.launch {
                                        viewModel!!.sendChatroomIntent(ChatroomIntent.DeleteChatroom)
                                    }
                                    dialogInterface.dismiss()
                                },
                                { dialogInterface: DialogInterface, _: Int ->
                                    dialogInterface.dismiss()
                                }
                            )
                            return@setOnMenuItemClickListener true
                        }
                        navViewAdmin.menu.findItem(R.id.action_event_selection).setOnMenuItemClickListener {
                            val bottomSheet = (supportFragmentManager.findFragmentByTag(Constants.EVENT_SELECTION)
                                    as EventSelectionBottomSheetDialogFragment?)
                                ?: EventSelectionBottomSheetDialogFragment.newInstance()
                            bottomSheet.show(supportFragmentManager, Constants.EVENT_SELECTION)
                            return@setOnMenuItemClickListener true
                        }
                    }
                }

                chatroom.photoUrl?.let {
                    Glide.with(this@ChatroomActivity)
                        .load(it)
                        .placeholder(headerBinding.chatroomImage.drawable)
                        .signature(
                            ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_chatrooms_fetch_time))
                        )
                        // calculate current page based on item position
                        .into(headerBinding.chatroomImage)
                }

                // FIXME: Small coderino duperino with ChatroomTagAdapter
                chatroom.tags?.let {
                    val adapter = TagListAdapter(chatroom.tags!!, this@ChatroomActivity, R.layout.chatroom_tag_card)
                    binding.tagsGridView.setHeightBasedOnChildren(chatroom.tags!!.size)

                    binding.tagsGridView.adapter = adapter
                }

                headerBinding.chatroomDescription.text = chatroom.description // two-way data-binding, y u no work??

                if(viewModel.currentAdapterUsers.value!!.isEmpty()) {
                    lifecycleScope.launch {
                        viewModel.sendUsersIntent(
                            UserListIntent.FetchUsers
                        )
                    }
                }
            }
        })
    }

    @ExperimentalPagingApi
    private fun observeChatroomOwnRights() {
        viewModel.isAuthUserChatroomOwner.observe(this, Observer {
            initTopNavigation(it)
        })
    }

    override fun observeConnectionRefresh(savedState: Bundle?, refreshConnectivityMonitor: RefreshConnectivityMonitor) {
        super.observeConnectionRefresh(savedState, refreshConnectivityMonitor)
        refreshConnectivityMonitor.observe(this, Observer { connectionRefreshed ->
            if(connectionRefreshed) {
                Log.i("ChatroomActivity", "ChatroomActivity CONNECTED")
                lifecycleScope.launch {
                    viewModel.sendUsersIntent(
                        UserListIntent.FetchUsers
                    )
                    viewModel.sendChatroomIntent(
                        ChatroomIntent.FetchChatroom
                    )
                    viewModel.sendEventsIntent(
                        EventListIntent.FetchEvents
                    )
                }
            } else {
                Log.i("ChatroomActivity", "ChatroomActivity DIS-CONNECTED")
            }
        })
    }

    @ExperimentalPagingApi
    private fun initTopNavigation(chatroomOwner: Boolean) {
        with(binding) {
            toolbar.setNavigationIconTint(Color.WHITE)
            if(chatroomOwner) {
                Log.i("ChatroomActivity", "Current auth user is chatroom owner")

                navViewAdmin.setupWithNavController(navController)
                toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(R.id.chatroomMessageListFragment), drawerLayout))
                if(supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
                    toolbar.navigationIcon =
                        ContextCompat.getDrawable(this@ChatroomActivity, R.drawable.ic_baseline_admin_panel_settings_24)
                }
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNDEFINED, GravityCompat.START)
            } else {
                Log.i("ChatroomActivity", "Current auth user is NOT chatroom owner")
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.START)
                toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(R.id.chatroomMessageListFragment)))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        assertGooglePlayAvailability()
        registerCRUDReceivers()
        // TODO: Move receiver registration after chatroom/users/event fetches!!!
    }

    override fun onPause() {
        super.onPause()
        unregisterCRUDReceivers()
    }

    private fun assertGooglePlayAvailability() {
        val googleApiInstance = GoogleApiAvailability.getInstance()
        val availability = googleApiInstance.isGooglePlayServicesAvailable(this)
        if(availability == SERVICE_MISSING || availability == SERVICE_INVALID
            || availability == SERVICE_DISABLED) {
            googleApiInstance.makeGooglePlayServicesAvailable(this)
            // TODO: onBackPressed?
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chatroom_appbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_search) {
            // TODO: Future chat search functionality with SearchView
        } else {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        return true
    }

    fun handleAuthActionableError(error: String?, shouldExit: Boolean, context: Context? = null) {
        Toast.makeText(context ?: this@ChatroomActivity,
                "Whoops! Looks like something went wrong! $error", Toast.LENGTH_LONG)
            .show()
        if(shouldExit) {
            finish()
            // TODO: Add another field (shouldReauth) for REALLY bad errors
            sendBroadcast(Intent(Constants.LOGOUT))
        }
    }

    private fun setEventCalendarDates(events: List<Event>) {
        with(binding) {
            events.forEach { event ->
                val eventDate = event.calendarDayFromDate
                eventCalendar.setDateSelected(eventDate, true)
            }
        }
    }


    @ExperimentalPagingApi
    override fun onEditMessageSelect(view: View, message: Message) {
        (supportFragmentManager.currentNavigationFragment as ChatroomMessageListFragment)
            .onEditMessageSelect(view, message)
    }

    @ExperimentalPagingApi
    override fun onDeleteMessageSelect(view: View, message: Message) {
        (supportFragmentManager.currentNavigationFragment as ChatroomMessageListFragment)
            .onDeleteMessageSelect(view, message)
    }

    // Override here due to FragmentActivity modifying request codes and being passed through the fragment hosting activity first
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initCalendar() {
        with(binding) {
            eventCalendar.setOnTouchListener { _, motionEvent ->
                Log.i("ChatroomActivity", "Triggered event calendar touch event w/ motion: $motionEvent")
                when(motionEvent.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        drawerLayout.requestDisallowInterceptTouchEvent(true)
                    }
                }
                true
            }

            eventCalendar.selectionMode = SELECTION_MODE_SINGLE
            eventCalendar.setOnDateChangedListener { calendar, date, selected ->
                Log.i("ChatroomActivity", "Calendar selected events: ${calendar.selectedDates}")
                val correspondingEvent: Event? = viewModel!!.authEvents.value?.find {
                    it.date.equals(it.calendarDayFromDate) }
                if(viewModel!!.authEvents.value == null || (correspondingEvent != null && selected)) {
                    Log.i("ChatroomActivity", "Calendar selected event outside event dates. Selected date: $date")
                    calendar.setDateSelected(date, false)
                    return@setOnDateChangedListener
                }

                Log.i("ChatroomActivity", "Calendar selected event with date: $date")
                Log.i("ChatroomActivity", "Corresponding event: $correspondingEvent")
                // TODO: show event dialog/decorator for event info + join/leave buttons depending on user fcm eventids
            }
        }
    }

    override fun onBackPressed() {
        FirebaseMessaging.getInstance()
            .unsubscribeFromTopic(Constants.chatroomTopic(viewModel.authChatroom.value!!.id))
        .addOnCompleteListener {
            prefConfig.resetLastEnteredChatroomId()
        }.addOnCanceledListener {
            Log.i("ChatroomActivity", "Cancelled user unsubsription from topic upon exiting chatroom.")
        }.addOnFailureListener {
            it.printStackTrace()
            Log.i("ChatroomActivity", "Failed to unsubscribe user from topic upon exiting chatroom. $it")
        }.continueWith {
            super.onBackPressed()
        }
    }

    private fun registerCRUDReceivers() {
        chatroomReceiverFactory = ChatroomBroadcastReceiverFactory.getInstance(viewModel, this)
        editChatroomReceiver = chatroomReceiverFactory!!.createActionatedReceiver(Constants.EDIT_CHATROOM_TYPE)
        deleteChatroomReceiver = chatroomReceiverFactory!!.createActionatedReceiver(Constants.DELETE_CHATROOM_TYPE)
        editUserReceiver = chatroomReceiverFactory!!.createActionatedReceiver(Constants.EDIT_USER_TYPE)
        joinUserReceiver = chatroomReceiverFactory!!.createActionatedReceiver(Constants.JOIN_USER_TYPE)
        leaveUserReceiver = chatroomReceiverFactory!!.createActionatedReceiver(Constants.LEAVE_USER_TYPE)
        eventReceiverFactory = EventBroadcastReceiverFactory.getInstance(viewModel, this)
        createEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.CREATE_EVENT_TYPE)
        editEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.EDIT_EVENT_TYPE)
        deleteBatchEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_BATCH_TYPE)
        deleteEventReceiver = eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_TYPE)

        registerReceiver(editChatroomReceiver, IntentFilter(Constants.EDIT_CHATROOM_TYPE))
        registerReceiver(deleteChatroomReceiver, IntentFilter(Constants.DELETE_CHATROOM_TYPE))
        registerReceiver(editUserReceiver, IntentFilter(Constants.EDIT_USER_TYPE))
        registerReceiver(joinUserReceiver, IntentFilter(Constants.JOIN_USER_TYPE))
        registerReceiver(leaveUserReceiver, IntentFilter(Constants.LEAVE_USER_TYPE))
        registerReceiver(createEventReceiver, IntentFilter(Constants.CREATE_EVENT_TYPE))
        registerReceiver(editEventReceiver, IntentFilter(Constants.EDIT_EVENT_TYPE))
        registerReceiver(deleteBatchEventReceiver, IntentFilter(Constants.DELETE_EVENT_BATCH_TYPE))
        registerReceiver(deleteEventReceiver, IntentFilter(Constants.DELETE_EVENT_TYPE))
    }

    private fun unregisterCRUDReceivers() {
        unregisterReceiver(editChatroomReceiver)
        unregisterReceiver(deleteChatroomReceiver)
        unregisterReceiver(editUserReceiver)
        unregisterReceiver(joinUserReceiver)
        unregisterReceiver(leaveUserReceiver)
        unregisterReceiver(createEventReceiver)
        unregisterReceiver(editEventReceiver)
        unregisterReceiver(deleteBatchEventReceiver)
        unregisterReceiver(deleteEventReceiver)
    }
}