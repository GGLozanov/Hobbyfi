package com.example.hobbyfi.ui.chatroom

import android.annotation.SuppressLint
import android.content.*
import android.content.Intent
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
import com.example.hobbyfi.intents.*
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.Message
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.base.NavigationActivity
import com.example.hobbyfi.ui.base.RefreshConnectionAware
import com.example.hobbyfi.ui.custom.EventCalendarDecorator
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.google.android.gms.common.ConnectionResult.*
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnFailureListener
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.*
import io.branch.referral.Branch
import io.branch.referral.Branch.BranchReferralInitListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import java.util.*


@ExperimentalCoroutinesApi
class ChatroomActivity : NavigationActivity(),
        ChatroomMessageBottomSheetDialogFragment.OnMessageOptionSelected, RefreshConnectionAware {
    private val viewModel: ChatroomActivityViewModel by viewModels(factoryProducer = {
        AuthUserChatroomViewModelFactory(application, args.user, args.chatroom)
    })

    private val fcmTopicErrorFallback: OnFailureListener by instance(
        tag = "fcmTopicErrorFallback",
        this
    )

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
        initNavController()

        // checks if called from push notification while app is killed and restarts entire backstack in that case
        if(isTaskRoot) {
            Log.i(
                "ChatroomActivity",
                "ChatroomActivity IS TASK ROOT. Regenerating parent activity backstack!"
            )
            val restartIntent = Intent(this, ChatroomActivity::class.java)

            restartIntent.putExtras(intent)

            viewModel.setAuthEvents(null) // reset events for re-fetch
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
            viewModel.chatroomUsers.value ?: emptyList()
        ) { _: View, user: User ->
            val bottomSheet = ChatroomUserBottomSheetDialogFragment.newInstance(user)
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        with(binding) {
            usersList.addItemDecoration(VerticalSpaceItemDecoration(10))
            usersList.adapter = userListAdapter

            initCalendar()
        }

        observeConnectionRefresh(savedInstanceState, refreshConnectivityMonitor)
    }

    @ExperimentalPagingApi
    override fun onStart() {
        super.onStart()

        // TODO: Register delete/update BroadcastReceive with User intents and Event intents
        // TODO: First fetch messages from back-end then register for receiving messages
        observeChatroomState()
        observeChatroom()
        observeUsers()
        observeUserState()
        observeUsersState()
        observeEventsState()
        observeEvents()
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
        lifecycleScope.launchWhenStarted {
            viewModel.chatroomState.collect {
                when(it) {
                    is ChatroomState.Idle -> {

                    }
                    is ChatroomState.Loading -> {
                        // TODO: Loading
                    }
                    is ChatroomState.OnData.ChatroomResult -> {
                    }
                    is ChatroomState.OnData.ChatroomDeleteResult -> {
                        Toast.makeText(
                            this@ChatroomActivity,
                            "Successfully deleted chatroom!",
                            Toast.LENGTH_LONG
                        ).show()
                        leaveDeletedChatroom()
                    }
                    is ChatroomState.OnData.DeleteChatroomCacheResult -> {
                        Toast.makeText(
                            this@ChatroomActivity,
                            "Oh no, it looks like the chatroom was deleted by the owner! We apologise for the inconvenience this may have caused!",
                            Toast.LENGTH_LONG
                        ).show()
                        leaveDeletedChatroom()
                    }
                    is ChatroomState.OnData.ChatroomUpdateResult -> {
                        Toast.makeText(
                            this@ChatroomActivity,
                            "Successfully updated chatroom!",
                            Toast.LENGTH_LONG
                        )
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

    private fun leaveDeletedChatroom() {
        localBroadcastManager.sendBroadcast(Intent(Constants.CHATROOM_DELETED)
            .apply {
                putExtra(
                    Constants.CHATROOM_ID,
                    viewModel.authChatroom.value!!.id
                )
            })

        leaveChatroom()
    }

    // for deeplink errors
    private fun leaveChatroomWithReauth() {
        localBroadcastManager.sendBroadcast(Intent(Constants.LOGOUT))

        leaveChatroom()
    }

    private fun leaveChatroom() {
        viewModel.authChatroom.value?.let {
            Callbacks.unsubscribeToChatroomTopicByCurrentConnectivity(
                {
                    viewModel.setChatroom(null) // clear chatroom in any case
                    finish()
                },
                it.id,
                fcmTopicErrorFallback,
                connectivityManager
            )
        }
    }

    private fun observeUsers() {
        viewModel.chatroomUsers.observe(this, Observer {
            userListAdapter!!.setUsers(it)
        })
    }

    private fun observeUsersState() {
        lifecycleScope.launchWhenStarted {
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
                        handleAuthActionableError(it.error, it.shouldReauth)
                    }
                }
            }
        }
    }

    private fun observeEventsState() {
        lifecycleScope.launchWhenStarted {
            viewModel.eventsState.collect {
                when(it) {
                    is EventListState.Idle -> {

                    }
                    is EventListState.Loading -> {
                        // TODO: Progressbar on event card
                    }
                    is EventListState.OnData.EventsResult -> {
                        viewModel.sendUserGeoPointIntent(
                            UserGeoPointIntent.FetchAuthUserGeoPoint
                        )
                    }
                    is EventListState.OnData.DeleteOldEventsResult -> {
                        Log.i("ChatroomActivity", "Received DeleteOldEventsResult state!")
                        Toast.makeText(
                            this@ChatroomActivity,
                            "Successfully deleted old events!",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        navController.popBackStack(R.id.chatroomMessageListFragment, false)
                        viewModel.resetEventListState()
                    }
                    is EventListState.OnData.DeleteEventsCacheResult -> {
                        Log.i(
                            "ChatroomActivity",
                            "Received DeleteEventsCacheResult state! Deleted events id: ${it.eventIds}. Attempting to pop event fragment off backstack!"
                        )
                        // TODO: Check if fragment visible, show toast, and pop
                        navController.popBackStack(R.id.chatroomMessageListFragment, false)
                        viewModel.resetEventListState()
                    }
                    is EventListState.OnData.DeleteAnEventCacheResult -> {
                        Log.i(
                            "ChatroomActivity",
                            "Received DeleteAnEventCacheResult state! Deleted event id: ${it.eventId}. Attempting to pop event fragment off backstack!"
                        )
                        // TODO: Check if fragment visible, show toast, and pop
                        navController.popBackStack(R.id.chatroomMessageListFragment, false)
                        viewModel.resetEventListState()
                    }
                    is EventListState.Error -> {
                        handleAuthActionableError(it.error, it.shouldReauth)
                        viewModel.resetEventListState()
                    }
                }
            }
        }
    }

    private fun observeEvents() {
        viewModel.authEvents.observe(this, Observer {
            Log.i("ChatroomActivity", "Received events list. Setting calendar dates to $it")
            reinitCalendarDecorators(it)
        })
    }

    @ExperimentalPagingApi
    private fun observeChatroom() {
        viewModel.authChatroom.observe(this, Observer { chatroom ->
            if (chatroom != null) {
                // TODO: Remove
                if (supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
                    title = chatroom.name
                }

                val authUserChatroomOwner = viewModel.isAuthUserChatroomOwner.value == true
                val chatroomHasEvents = chatroom.eventIds != null
                val chatroomHasEventsToFetch =
                    chatroomHasEvents && viewModel.authEvents.value == null

                if (chatroomHasEventsToFetch) {
                    lifecycleScope.launch {
                        viewModel.sendEventsIntent(
                            EventListIntent.FetchEvents
                        )
                    }
                }

                if (authUserChatroomOwner) {
                    with(binding) {
                        // TODO: On Chatroom deletes -> if no more events left, set authChatroom eventIds to  null
                        navViewAdmin.menu.findItem(R.id.action_delete_chatroom)
                            .setOnMenuItemClickListener {
                                this@ChatroomActivity.buildYesNoAlertDialog(
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
                        navViewAdmin.menu.findItem(R.id.action_event_selection)
                            .setOnMenuItemClickListener {
                                supportFragmentManager.showDistinctDialog(
                                    Constants.EVENT_SELECTION,
                                    {
                                        EventAdminSelectionBottomSheetDialogFragment.newInstance()
                                    })
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
                        .into(headerBinding.chatroomImage)
                }

                // FIXME: Small coderino duperino with ChatroomTagAdapter
                chatroom.tags?.let {
                    val adapter = TagListAdapter(
                        chatroom.tags!!,
                        this@ChatroomActivity,
                        R.layout.chatroom_tag_card
                    )
                    binding.tagsGridView.setHeightBasedOnChildren(chatroom.tags!!.size)

                    binding.tagsGridView.adapter = adapter
                }

                headerBinding.chatroomDescription.text =
                    chatroom.description // two-way data-binding, y u no work??

                if (viewModel.chatroomUsers.value!!.isEmpty()) {
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

    override fun observeConnectionRefresh(
        savedState: Bundle?,
        refreshConnectivityMonitor: RefreshConnectivityMonitor
    ) {
        super.observeConnectionRefresh(savedState, refreshConnectivityMonitor)
        refreshConnectivityMonitor.observe(this, Observer { connectionRefreshed ->
            // connectivityManager.isConnected() IMPORTANT TODO: Fix in order to refetch if user
            //  enter without internet (currently refetches old chatrooms in itiial joins)
            if (connectionRefreshed) {
                Log.i("ChatroomActivity", "ChatroomActivity CONNECTED")
                lifecycleScope.launch {
                    viewModel.sendUsersIntent(
                        UserListIntent.FetchUsers
                    )
                    viewModel.sendChatroomIntent(
                        ChatroomIntent.FetchChatroom
                    )

                    if (viewModel.authChatroom.value?.eventIds != null) {
                        viewModel.sendEventsIntent(
                            EventListIntent.FetchEvents
                        )
                    }
                }
            } else {
                Log.i("ChatroomActivity", "ChatroomActivity DIS-CONNECTED")
            }
        })
    }

    @ExperimentalPagingApi
    private fun initTopNavigation(chatroomOwner: Boolean) {
        binding.toolbar.setNavigationIconTint(Color.WHITE)
        setToolbarAdminIconOnOwnership(chatroomOwner)
    }

    @ExperimentalPagingApi
    override fun onResume() {
        super.onResume()
        assertGooglePlayAvailability()
        registerCRUDReceivers()
        with(binding) {
            navViewChatroom.setupWithNavController(navController)

            if(viewModel!!.isAuthUserChatroomOwner.value == true) {
                navViewAdmin.setupWithNavController(navController)
                toolbar.setupWithNavController(
                    navController, AppBarConfiguration(
                        setOf(R.id.chatroomMessageListFragment),
                        drawerLayout
                    )
                )

                drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNDEFINED,
                    GravityCompat.START
                )
                setToolbarAdminIconOnOwnership(true)
            } else {
                drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.START
                )
                toolbar.setupWithNavController(
                    navController,
                    AppBarConfiguration(setOf(R.id.chatroomMessageListFragment))
                )
                // TODO: Back button (white tint)
            }
        }

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

    @ExperimentalPagingApi
    private fun setToolbarAdminIconOnOwnership(owner: Boolean?) {
        if(supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
            binding.toolbar.navigationIcon = if(owner == true )
                ContextCompat.getDrawable(
                    this@ChatroomActivity,
                    R.drawable.ic_baseline_admin_panel_settings_24
                ) else null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chatroom_appbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.action_search) {
            // TODO: Future chat search functionality with SearchView
        } else if(item.itemId == R.id.action_info) {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        return true
    }

    fun handleAuthActionableError(error: String?, shouldExit: Boolean, context: Context? = null) {
        Toast.makeText(
            context ?: this@ChatroomActivity,
            "Whoops! Looks like something went wrong! $error", Toast.LENGTH_LONG
        ).show()
        if(shouldExit) {
            finish()
            // TODO: Add another field (shouldReauth) for REALLY bad errors
            localBroadcastManager.sendBroadcast(Intent(Constants.LOGOUT))
        }
    }

    private fun reinitCalendarDecorators(events: List<Event>) {
        with(binding) {
            eventCalendar.removeDecorators()
            eventCalendar.addDecorator(
                EventCalendarDecorator(
                    ContextCompat.getColor(this@ChatroomActivity, R.color.colorPrimary),
                    events.map { it.calendarDayFromDate }
                )
            )
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
                Log.i(
                    "ChatroomActivity",
                    "Triggered event calendar touch event w/ motion: $motionEvent"
                )
                when(motionEvent.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        drawerLayout.requestDisallowInterceptTouchEvent(true)
                    }
                }
                true
            }

            eventCalendar.selectionMode = SELECTION_MODE_SINGLE
            eventCalendar.setOnDateChangedListener { calendar, date, _ ->
                Log.i("ChatroomActivity", "Calendar selected events: ${calendar.selectedDates}")

                Log.i("ChatroomActivity", "Calendar selected event with date: $date")
                calendar.isEnabled = false
                supportFragmentManager.showDistinctDialog(Constants.EVENT_SELECTION, {
                    EventCalendarSelectionBottomSheetDialogFragment.newInstance(date)
                })
                calendar.postDelayed({
                    calendar.isEnabled = true
                }, 1000) // antispam
            }
        }
    }

    override fun onBackPressed() {
        Callbacks.unsubscribeToChatroomTopicByCurrentConnectivity(
            {
                prefConfig.resetLastEnteredChatroomId()
                if(!isFinishing) {
                    super.onBackPressed()
                }
            },
            viewModel.authChatroom.value!!.id,
            fcmTopicErrorFallback,
            connectivityManager
        )
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

        with(localBroadcastManager) {
            registerReceiver(editChatroomReceiver!!, IntentFilter(Constants.EDIT_CHATROOM_TYPE))
            registerReceiver(deleteChatroomReceiver!!, IntentFilter(Constants.DELETE_CHATROOM_TYPE))
            registerReceiver(editUserReceiver!!, IntentFilter(Constants.EDIT_USER_TYPE))
            registerReceiver(joinUserReceiver!!, IntentFilter(Constants.JOIN_USER_TYPE))
            registerReceiver(leaveUserReceiver!!, IntentFilter(Constants.LEAVE_USER_TYPE))
            registerReceiver(createEventReceiver!!, IntentFilter(Constants.CREATE_EVENT_TYPE))
            registerReceiver(editEventReceiver!!, IntentFilter(Constants.EDIT_EVENT_TYPE))
            registerReceiver(deleteBatchEventReceiver!!, IntentFilter(Constants.DELETE_EVENT_BATCH_TYPE))
            registerReceiver(deleteEventReceiver!!, IntentFilter(Constants.DELETE_EVENT_TYPE))
        }
    }

    private fun unregisterCRUDReceivers() {
        with(localBroadcastManager) {
            unregisterReceiver(editChatroomReceiver!!)
            unregisterReceiver(deleteChatroomReceiver!!)
            unregisterReceiver(editUserReceiver!!)
            unregisterReceiver(joinUserReceiver!!)
            unregisterReceiver(leaveUserReceiver!!)
            unregisterReceiver(createEventReceiver!!)
            unregisterReceiver(editEventReceiver!!)
            unregisterReceiver(deleteBatchEventReceiver!!)
            unregisterReceiver(deleteEventReceiver!!)
        }
    }
}