package com.example.hobbyfi.ui.chatroom

import android.content.*
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
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
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.ActivityChatroomBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.EventIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.shared.ChatroomBroadcastReceiverFactory
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.currentNavigationFragment
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
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class ChatroomActivity : BaseActivity() {
    private val viewModel: ChatroomActivityViewModel by viewModels(factoryProducer = {
        AuthUserChatroomViewModelFactory(application, args.user, args.chatroom)
    })

    private val fcmTopicErrorFallback: OnFailureListener by instance(tag = "fcmTopicErrorFallback", this)

    lateinit var binding: ActivityChatroomBinding
    private val args: ChatroomActivityArgs by navArgs()

    private lateinit var userListAdapter: ChatroomUserListAdapter

    private lateinit var chatroomReceiverFactory: ChatroomBroadcastReceiverFactory
    private lateinit var editChatroomReceiver: BroadcastReceiver
    private lateinit var deleteChatroomReceiver: BroadcastReceiver
    private lateinit var editUserReceiver: BroadcastReceiver
    private lateinit var joinUserReceiver: BroadcastReceiver
    private lateinit var leaveUserReceiver: BroadcastReceiver
    private lateinit var deleteEventReceiver: BroadcastReceiver
    private lateinit var editEventReceiver: BroadcastReceiver
    private lateinit var createEventReceiver: BroadcastReceiver

    private var currentEventGlideTarget: CustomTarget<Drawable>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        assertGooglePlayAvailability()

        binding.viewModel = viewModel

        if(viewModel.authUser.value == null && viewModel.authChatroom.value == null) {
            // deeplink situation
            lifecycleScope.launch {
                viewModel.sendIntent(UserIntent.FetchUser)
                viewModel.sendChatroomIntent(ChatroomIntent.FetchChatroom)
            }
        }

        userListAdapter = ChatroomUserListAdapter(
            viewModel.currentAdapterUsers.value ?: emptyList(),
            { _: View, user: User ->
                val bottomSheet = ChatroomUserBottomSheetDialogFragment.newInstance(user)
                bottomSheet.show(supportFragmentManager, bottomSheet.tag)
            }
        )

        with(binding) {
            usersList.addItemDecoration(VerticalSpaceItemDecoration(10))
            usersList.adapter = userListAdapter
            eventCard.background.alpha = 255 * (75 / 100) // 75% of 255 (255 = max alpha value)
            // TODO: Get GeoUser model from Cloud Firestore and observe. After fetch => set button visibility depending on user join
            joinLeaveEventButtonBar.leftButton.setOnClickListener { // leave event
                // unsubscribe from cloud firestore (through eventrepository, eventintent, etc.)
            }
            joinLeaveEventButtonBar.rightButton.setOnClickListener { // join event
                // navigate to maps activity
            }
            // TODO: add user recyclerview onclick + view user profile bottomsheet fragment
        }

        // TODO: On right navdrawer press (through activity listener), refresh users data source in viewmodel and fetch new users
        //  => triggers REFRESH loadstate in Mediator => check if users last fetch time is too long
        //  => doesn't delete old users (if no connection or time isn't long enough) => fetches users if time is long enough
        // TODO: Update users data source in Room upon notification (no need to refetch from network???)

        // if not deeplink: fire off 3 requests/load from db here -> event, messages, user
        // if deeplink: also fire off request/load from db here -> chatroom info

        // TODO: If called from deeplink, use chatroomliststate and fetch single chatroom

        // TODO: Ask for permission upon event card press for access to location
    }

    @ExperimentalPagingApi
    override fun onStart() {
        super.onStart()

        binding.navViewAdmin.setupWithNavController(navController)

        // TODO: Register delete/update BroadcastReceive with User intents and Event intents
        // TODO: First fetch messages from back-end then register for receiving messages

        observeChatroomState()
        observeUserState()
        observeUsersState()
        observeEventState()
        observeChatroom()
        observeChatroomOwnRights()
        observeConnectionRefresh()

        if(viewModel.currentAdapterUsers.value!!.isEmpty()) {
            lifecycleScope.launch {
                viewModel.sendUsersIntent(
                    UserListIntent.FetchUsers
                )
            }
        }
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
                        viewModel.setUser(it.user)
                    }
                    is UserState.Error -> {
                        handleAuthActionableError(it.error, it.shouldReauth)
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
                        // TODO: UI or smth
                    }
                    is ChatroomState.OnData.ChatroomDeleteResult, is ChatroomState.OnData.DeleteChatroomCacheResult -> {
                        Toast.makeText(this@ChatroomActivity, "Successfully deleted chatroom!", Toast.LENGTH_LONG)
                            .show()
                        sendBroadcast(Intent(Constants.CHATROOM_DELETED))
                        FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants
                            .chatroomTopic(viewModel.authChatroom.value!!.id)).addOnFailureListener(fcmTopicErrorFallback)
                        viewModel.setChatroom(null) // clear chatroom in any case
                        finish()
                    }
                    is ChatroomState.OnData.ChatroomUpdateResult -> {
                        Toast.makeText(this@ChatroomActivity, "Successfully updated chatroom!", Toast.LENGTH_LONG)
                            .show()
                    }
                    is ChatroomState.Error -> {
                        handleAuthActionableError(it.error, it.shouldExit)
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
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
                        userListAdapter.setUsers(it.users)
                    }
                    is UserListState.Error -> {
                        handleAuthActionableError(it.error, it.shouldReauth)
                    }
                }
            }
        }
    }

    private fun observeEventState() {
        lifecycleScope.launch {
            viewModel.eventState.collect {
                when(it) {
                    is EventState.Idle -> {

                    }
                    is EventState.Loading -> {
                        // TODO: Progressbar on event card
                    }
                    is EventState.OnData.OnEventReceived -> {
                        if(it.event.photoUrl != null) {
                            // FIXME: Is it bad to keep this in activity like this?
                            currentEventGlideTarget = Glide.with(this@ChatroomActivity)
                                .asDrawable()
                                .load(it.event.photoUrl)
                                .signature(ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_event_fetch_time)))
                                .into(object : CustomTarget<Drawable>() {
                                    override fun onResourceReady(
                                        resource: Drawable,
                                        transition: Transition<in Drawable>?
                                    ) {
                                        binding.eventCard.background = resource
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {
                                        Glide.with(this@ChatroomActivity).clear(currentEventGlideTarget)
                                    }
                                })
                        }
                    }
                    is EventState.Error -> {
                        handleAuthActionableError(it.error, it.shouldReauth)
                    }
                }
            }
        }
    }

    @ExperimentalPagingApi
    private fun observeChatroom() {
        viewModel.authChatroom.observe(this, Observer {
            if(supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
                title = it?.name
            }

            if(viewModel.isAuthUserChatroomOwner.value == true) {
                with(binding) {
                    navViewAdmin.menu.clear()
                    if(it?.lastEventId == null) {
                        navViewAdmin.inflateMenu(R.menu.chatroom_admin_nav_drawer_menu_create)
                    } else {
                        navViewAdmin.inflateMenu(R.menu.chatroom_admin_nav_drawer_menu_edit)
                    }
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
                }
            }

            if(it?.lastEventId != null && viewModel.authEvent.value == null) {
                lifecycleScope.launch {
                    viewModel.sendEventIntent(
                        EventIntent.FetchEvent
                    )
                }
            }
        })
    }

    private fun observeChatroomOwnRights() {
        viewModel.isAuthUserChatroomOwner.observe(this, Observer {
            initTopNavigation(it)
        })
    }

    private fun observeConnectionRefresh() {
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
                    viewModel.sendEventIntent(
                        EventIntent.FetchEvent
                    )
                }
            } else {
                Log.i("ChatroomActivity", "ChatroomActivity DIS-CONNECTED")
            }
        })
    }

    private fun initTopNavigation(chatroomOwner: Boolean) {
        with(binding) {
            toolbar.setNavigationIconTint(Color.WHITE)
            if(chatroomOwner) {
                Log.i("ChatroomActivity", "Current auth user is chatroom owner")

                navViewAdmin.setupWithNavController(navController)
                toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(R.id.chatroomMessageListFragment), drawerLayout))
                toolbar.navigationIcon = ContextCompat.getDrawable(this@ChatroomActivity, R.drawable.ic_baseline_admin_panel_settings_24)
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

        // TODO: Move register to after state received IF deeplink situation
        // TODO: Also send any broadcasts from push notification intents received after receiving state in deeplink
        when(intent.action) {
            // TODO: Handle push notification intents here with broadcastreceiver callbacks
            // TODO: Handle deeplink situation (per se) wherein there is no chatroom/user
            // TODO: And broadcastreceiver/notification routines have to wait until data is fetched and then updated
            // sendBroadcast(intent)
        }


        // TODO: Move receiver registration after chatroom/users/event fetches!!!
        chatroomReceiverFactory = ChatroomBroadcastReceiverFactory.getInstance(viewModel, this)
        editChatroomReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.EDIT_CHATROOM_TYPE)
        deleteChatroomReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.DELETE_CHATROOM_TYPE)
        editUserReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.EDIT_USER_TYPE)
        joinUserReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.JOIN_USER_TYPE)
        leaveUserReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.LEAVE_USER_TYPE)
        deleteEventReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.DELETE_EVENT_TYPE)
        editEventReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.EDIT_EVENT_TYPE)
        createEventReceiver = chatroomReceiverFactory.createActionatedReceiver(Constants.CREATE_EVENT_TYPE)

        registerReceiver(editChatroomReceiver, IntentFilter(Constants.EDIT_CHATROOM_TYPE))
        registerReceiver(deleteChatroomReceiver, IntentFilter(Constants.DELETE_CHATROOM_TYPE))
        registerReceiver(editUserReceiver, IntentFilter(Constants.EDIT_USER_TYPE))
        registerReceiver(joinUserReceiver, IntentFilter(Constants.JOIN_USER_TYPE))
        registerReceiver(leaveUserReceiver, IntentFilter(Constants.LEAVE_USER_TYPE))
        registerReceiver(deleteEventReceiver, IntentFilter(Constants.DELETE_EVENT_TYPE))
        registerReceiver(createEventReceiver, IntentFilter(Constants.CREATE_EVENT_TYPE))
        registerReceiver(editEventReceiver, IntentFilter(Constants.EDIT_EVENT_TYPE))
    }

    override fun onPause() {
        super.onPause()

        unregisterReceiver(editChatroomReceiver)
        unregisterReceiver(deleteChatroomReceiver)
        unregisterReceiver(joinUserReceiver)
        unregisterReceiver(leaveUserReceiver)
        unregisterReceiver(deleteEventReceiver)
        unregisterReceiver(createEventReceiver)
        unregisterReceiver(editEventReceiver)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(currentEventGlideTarget != null) {
            Log.i("ChatroomActivity", "ChatroomActivity onDestroy -> clearing glide event card target")
            Glide.with(this).clear(currentEventGlideTarget)
        }
    }

    private fun assertGooglePlayAvailability() {
        val googleApiInstance = GoogleApiAvailability.getInstance()
        val availability = googleApiInstance.isGooglePlayServicesAvailable(this)
        if(availability == SERVICE_MISSING || availability == SERVICE_INVALID
            || availability == SERVICE_DISABLED) {
            googleApiInstance.makeGooglePlayServicesAvailable(this)
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

    private fun handleAuthActionableError(error: String?, shouldExit: Boolean) {
        Toast.makeText(this@ChatroomActivity, "Whoops! Looks like something went wrong! $error", Toast.LENGTH_LONG)
            .show()
        if(shouldExit) {
            finish()
            // TODO: Add another field (shouldReauth) for REALLY bad errors
            sendBroadcast(Intent(Constants.LOGOUT))
        }
    }
}