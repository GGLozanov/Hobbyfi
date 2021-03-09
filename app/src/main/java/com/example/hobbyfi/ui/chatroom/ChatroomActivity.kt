package com.example.hobbyfi.ui.chatroom

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.*
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.SearchRecentSuggestions
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
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupWithNavController
import androidx.paging.ExperimentalPagingApi
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagListAdapter
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.ActivityChatroomBinding
import com.example.hobbyfi.databinding.NavHeaderChatroomBinding
import com.example.hobbyfi.intents.*
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.auth.AuthActivity
import com.example.hobbyfi.ui.base.NavigationActivity
import com.example.hobbyfi.ui.base.RefreshConnectionAware
import com.example.hobbyfi.ui.base.ServerSocketAccessor
import com.example.hobbyfi.ui.custom.EventCalendarDecorator
import com.example.hobbyfi.ui.main.MainActivity
import com.example.hobbyfi.ui.onboard.OnboardingActivity
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.example.hobbyfi.work.DeviceTokenDeleteWorker
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.facebook.login.LoginManager
import com.google.android.gms.common.ConnectionResult.*
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnFailureListener
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.*
import io.branch.referral.Branch
import io.branch.referral.Branch.BranchReferralInitListener
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.client.SocketOptionBuilder
import io.socket.emitter.Emitter
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kodein.di.generic.instance
import java.net.URISyntaxException
import java.util.*


@ExperimentalCoroutinesApi
class ChatroomActivity : NavigationActivity(),
        ChatroomMessageBottomSheetDialogFragment.OnMessageOptionSelected, ServerSocketAccessor,
        RefreshConnectionAware {
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

    private var userStateCollectJob: Job? = null

    override val emitterListenerFactory: EmitterListenerFactory by lazy {
        EmitterListenerFactory(this)
    }

    override val serverSocket: Socket? by lazy {
        try {
            val socket = IO.socket(BuildConfig.SOCKET_BASE_URL,
                SocketOptionBuilder.builder().setTransports(arrayOf(WebSocket.NAME)).setForceNew(true).build())
            Log.i("ServerSocketAccessor", "Accessed socket with successful connection")
            socket
        } catch(e: URISyntaxException) {
            onConnectedServerSocketFail()
            null
        }
    }

    private var sentJoinChatroomEvent: Boolean = false

    @ExperimentalPagingApi
    private val branchReferralInitListener =
        BranchReferralInitListener { linkProperties, error ->
            val comeFromAuthDeepLink = comeFromAuthDeepLink()
            viewModel.setCurrentLinkProperties(linkProperties)
            if ((linkProperties != null && getClickedBranchLinkFromLinkProps(linkProperties)) ||
                    comeFromAuthDeepLink) {
                val safeLinkProps = linkProperties ?: Branch.getInstance().latestReferringParams
                Log.i("ChatroomActivity", "Safe link props: ${safeLinkProps}")
                viewModel.setConsumedEventDeepLink(false)
                if (error != null && !comeFromAuthDeepLink) {
                    Log.e("ChatroomActivity", "Deep-linking error: $error")
                    leaveChatroomWithRestart(linkParams = safeLinkProps)
                } else {
                    if (prefConfig.readOnboardingValid() && !comeFromAuthDeepLink) {
                        Log.i("ChatroomActivity", "First session triggered")
                        leaveChatroomWithRestart(
                            linkParams = safeLinkProps,
                            leaveDestination = OnboardingActivity::class.java
                        )
                        return@BranchReferralInitListener
                    }

                    if (prefConfig.isUserAuthenticated()) {
                        if (getChatroomIdFromDeepLinkProps(safeLinkProps) != viewModel.authChatroom.value?.id) {
                            // force refetch if called from different chatroom
                            viewModel.setChatroom(null)
                            viewModel.setUser(null)
                            viewModel.setAuthEvents(null)
                            (supportFragmentManager.currentNavigationFragment as ChatroomMessageListFragment?)
                                ?.resetMessages()
                        }
                        
                        if(viewModel.authUser.value != null && viewModel.authChatroom.value != null 
                                && viewModel.authEvents.value != null) {
                            // foreground activation
                            observeEventsForDeepLink()
                        } else {
                            // something is missing (most likely the auth user)
                            sendUserIntentFetchIntentOnCurrentNull()
                        }
                    } else {
                        // should only EVER happen for deeplink (can't receive notifications when not logged in)
                        leaveChatroomWithRestart(Constants.noConnectionError)
                    }
                }
            } else {
                sendUserIntentFetchIntentOnCurrentNull()
                sendChatroomFetchIntentOnCurrentNull()
            }
        }


    @ExperimentalPagingApi
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i(
            "ChatroomActivity",
            "onNewIntent triggered! intent extras: ${intent?.extras?.toReadable()}"
        )
        // if activity is in foreground (or in backstack but partially visible) launching the same
        // activity will skip onStart, handle this case with reInitSession
        Branch.sessionBuilder(this).withCallback(branchReferralInitListener).reInit()
        handleSearchQuery()
    }

    @ExperimentalPagingApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initNavController()
        initDynamicToolbarTitle()

        Log.i("ChatroomActivity", "intent extras: ${intent.extras?.toReadable()}")
        if (checkNotificationOrDeepLinkCall()) {
            return
        }

        handleSearchQuery()

        assertGooglePlayAvailability()

        binding.viewModel = viewModel

        headerBinding = NavHeaderChatroomBinding.bind(binding.navViewChatroom.getHeaderView(0))
        headerBinding.viewModel = viewModel

        userListAdapter = ChatroomUserListAdapter(
            viewModel.chatroomUsers.value ?: arrayListOf()
        ) { _: View, user: User ->
            supportFragmentManager.showDistinctDialog(
                "UserSheet" + user.id.toString(),
                {
                    ChatroomUserBottomSheetDialogFragment.newInstance(user)
                }
            )
        }

        with(binding) {
            usersList.addItemDecoration(VerticalSpaceItemDecoration(10))
            usersList.adapter = userListAdapter

            initCalendar()
        }

        observeConnectionRefresh(savedInstanceState, refreshConnectivityMonitor)
    }

    // need to do this because it bugs out with up navigation
    private fun initDynamicToolbarTitle() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            setLabelOnDestination(destination)
        }
    }

    private fun setLabelOnDestination(destination: NavDestination) {
        binding.toolbar.title = when(destination.id) {
            R.id.chatroomMessageListFragment -> {
                viewModel.authChatroom.value?.name
            }
            else -> navController.currentDestination?.label
        }
    }

    @ExperimentalPagingApi
    override fun onStart() {
        super.onStart()

        connectServerSocket()
        Branch.sessionBuilder(this).withCallback(branchReferralInitListener)
            .withData(if (intent != null) intent.data else null).init()

        observeChatroomState()
        observeChatroom()
        observeChatroomOwnRights()
        observeUsers()
        observeUserState()
        observeUsersState()
        observeEventsState()
        observeEventState()
        observeEvents()
    }

    override fun onConnectedServerSocketFail() {
        Log.w("ChatroomActivity", "Socket connection from current auth user: ${viewModel.authUser.value} failed!")
        runOnUiThread {
            Toast.makeText(
                this,
                Constants.socketConnectionError,
                Toast.LENGTH_LONG
            ).show()
            leaveChatroom()
        }
    }

    override fun connectServerSocketListeners() {
        serverSocket?.on(Socket.EVENT_CONNECT) {
            emitJoinChatroomEventOnCurrentNonNull()
        }
//        serverSocket?.on(Constants.LEAVE_USER_TYPE)
        // TODO: The rest of the socket events
    }

    override fun disconnectServerSocketListeners() {
        serverSocket?.off(Constants.LEAVE_USER_TYPE)
        serverSocket?.off(Constants.EDIT_CHATROOM_TYPE)
        serverSocket?.off(Constants.DELETE_CHATROOM_TYPE)
        serverSocket?.off(Constants.EDIT_USER_TYPE)
        serverSocket?.off(Constants.CREATE_EVENT_TYPE)
        serverSocket?.off(Constants.EDIT_EVENT_TYPE)
        serverSocket?.off(Constants.DELETE_EVENT_TYPE)
        serverSocket?.off(Constants.DELETE_EVENT_BATCH_TYPE)

        serverSocket?.off(Constants.CREATE_MESSAGE_TYPE)

        sentJoinChatroomEvent = false
    }

    private fun observeUserState() {
        userStateCollectJob = lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when (it) {
                    is UserState.Idle -> {

                    }
                    is UserState.Loading -> {
                        // TODO: Progressbar
                    }
                    is UserState.OnData.UserResult -> {
                        checkDeepLinkStatusAndPerform {
                            val deepLinkChatroomId = getChatroomIdFromDeepLinkProps()
                            Log.i(
                                "ChatroomActivity",
                                "Deep link chatroom Id: ${deepLinkChatroomId}"
                            )
                            Log.i("ChatroomActivity", "user chatroom ids: ${it.user.chatroomIds}")

                            if (it.user.chatroomIds?.contains(deepLinkChatroomId) == true && deepLinkChatroomId != null) {
                                prefConfig.writeLastEnteredChatroomId(deepLinkChatroomId)
                                sendChatroomFetchIntentOnCurrentNull()
                            } else {
                                Toast.makeText(
                                    this@ChatroomActivity,
                                    Constants.notJoinedChatroomError,
                                    Toast.LENGTH_LONG
                                ).show()
                                leaveChatroom(sendExtrasBack = true)
                            }
                            viewModel.resetUserState()
                        }
                    }
                    is UserState.Error -> {
                        handleAuthActionableDeepLinkError(it.shouldReauth) {
                            handleAuthActionableError(it.error, true, it.shouldReauth)
                            viewModel.resetUserState()
                        }
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
                when (it) {
                    is ChatroomState.Idle -> {

                    }
                    is ChatroomState.Loading -> {
                        // TODO: Loading
                    }
                    is ChatroomState.OnData.ChatroomResult -> {
                        emitJoinChatroomEventOnCurrentNonNull()
//                        checkDeepLinkStatusAndPerform {
//                            Callbacks.subscribeToChatroomTopicByCurrentConnectivity(
//                                null,
//                                it.chatroom.id,
//                                fcmTopicErrorFallback,
//                                connectivityManager
//                            )
//                        }
                        viewModel.resetChatroomState()
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
                            if (it.kicked) Constants.chatroomKickedMessage
                            else Constants.chatroomDeletedMessage,
                            Toast.LENGTH_LONG
                        ).show()
                        leaveDeletedChatroom()
                    }
                    is ChatroomState.OnData.ChatroomUpdateResult -> {
                        Toast.makeText(
                            this@ChatroomActivity,
                            "Successfully updated chatroom!",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetChatroomState()
                    }
                    is ChatroomState.Error -> {
                        handleAuthActionableDeepLinkError(it.shouldExit) {
                            handleAuthActionableError(it.error, true, it.shouldExit)
                            viewModel.resetChatroomState()
                        }
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
            }
        )
        leaveChatroom()
    }

    fun leaveChatroom(
        linkParams: JSONObject? = null, sendExtrasBack: Boolean = false,
        leaveDestination: Class<*> = AuthActivity::class.java
    ) {
        val leave = {
            viewModel.setChatroom(null) // clear chatroom in any case
            if (linkParams != null) {
                startActivity(Intent(this, leaveDestination).apply {
                    linkParams.toBundle()?.let {
                        putExtras(it)
                    }
                    putExtras(intent)
                })
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finishAffinity()
            } else {
                if (sendExtrasBack) {
                    val extras = Branch.getInstance().latestReferringParams.toBundle()!!
                    userStateCollectJob?.cancel() // bugs out in repeating the same user state collection otherwise
                    startActivity(Intent(this, AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                    })
                    overridePendingTransition(0, 0) // no anim
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtras(extras)
                    })
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                } else finish()
            }
        }

//        if (viewModel.authChatroom.value != null) {
//            viewModel.authChatroom.value?.let {
//                Callbacks.unsubscribeToChatroomTopicByCurrentConnectivity(
//                    leave,
//                    it.id,
//                    fcmTopicErrorFallback,
//                    connectivityManager
//                )
//            }
//        } else {
            leave()
        // }
    }

    private fun leaveChatroomWithDelete() {
        lifecycleScope.launch {
            viewModel.sendChatroomIntent(ChatroomIntent.DeleteChatroomCache(true))
        }
    }

    // for deeplink & shouldExit errors
    private fun leaveChatroomWithRestart(
        exitMsg: String = Constants.invalidAccessError,
        linkParams: JSONObject? = viewModel.currentLinkProperties,
        leaveDestination: Class<*> = AuthActivity::class.java
    ) {
        // TODO: More graceful way to show this error... like a separate screen? Activity?
        Toast.makeText(applicationContext, exitMsg, Toast.LENGTH_LONG)
            .show()

        // log out of all accounts
        WorkerUtils.buildAndEnqueueDeviceTokenWorker<DeviceTokenDeleteWorker>(
            prefConfig.readDeviceToken(),
            this
        )
        LoginManager.getInstance().logOut()
        prefConfig.resetToken()
        prefConfig.resetRefreshToken()

        leaveChatroom(linkParams, leaveDestination = leaveDestination)
    }

    private fun observeUsers() {
        viewModel.chatroomUsers.observe(this, Observer {
            if (viewModel.authUser.value?.chatroomIds?.contains(viewModel.authChatroom.value?.id) == false) {
                leaveChatroomWithDelete()
                return@Observer
            }

            userListAdapter!!.setUsers(it)
        })
    }

    private fun observeUsersState() {
        lifecycleScope.launchWhenStarted {
            viewModel.usersState.collect {
                when (it) {
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
                        viewModel.resetUserListState()
                    }
                    is UserListState.OnUserKick -> {
                        supportFragmentManager.findFragmentByTag("UserSheet" + it.userKickedId)
                            ?.let { frag ->
                                (frag as DialogFragment).dismiss()
                            }

                        Toast.makeText(
                            this@ChatroomActivity, Constants.userKickSuccess,
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetUserListState()
                    }
                }
            }
        }
    }

    private fun observeEventsState() {
        lifecycleScope.launchWhenStarted {
            viewModel.eventsState.collect {
                when (it) {
                    is EventListState.Idle, EventListState.Loading -> {

                    }
                    is EventListState.OnData.EventsResult -> {
                        checkDeepLinkStatusAndPerform {
                            observeEventsForDeepLink()
                        }

                        viewModel.sendUserGeoPointIntent(
                            UserGeoPointIntent.FetchAuthUserGeoPoint
                        )
                        viewModel.resetEventListState()
                    }
                    is EventListState.OnData.DeleteOldEventsResult -> {
                        Log.i("ChatroomActivity", "Received DeleteOldEventsResult state!")
                        Toast.makeText(
                            this@ChatroomActivity,
                            "Successfully deleted old events!",
                            Toast.LENGTH_LONG
                        ).show()
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
    
    private fun observeEventState() {
        lifecycleScope.launchWhenStarted {
            viewModel.eventState.collect {
                when(it) {
                    is EventState.Idle -> {

                    }
                    is EventState.Loading -> {
                        // TODO: Progressbar? Somewhere?
                    }
                    is EventState.OnData.EventDeleteResult -> {
                        Toast.makeText(
                            this@ChatroomActivity,
                            "Event successfully deleted!",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetEventState()
                    }
                    is EventState.Error -> {
                        // TODO: Handle error
                        handleAuthActionableError(it.error, it.shouldReauth)
                        viewModel.resetEventState()
                    }
                    else -> throw State.InvalidStateException()
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

    private fun observeEventsForDeepLink() {
        viewModel.authEvents.distinctUntilChanged().observe(this, Observer {
            if (!viewModel.consumedEventDeepLink) {
                checkDeepLinkStatusAndPerform {
                    val eventId = getEventIdFromDeepLinkProps()
                    Log.i("ChatroomActivity", "event id: $eventId")
                    Log.i("ChatroomActivity", "events: $it")

                    val deepLinkedEvent = it.find { event -> event.id == eventId }
                    if (deepLinkedEvent != null) {
                        if (supportFragmentManager.currentNavigationFragment !is EventDetailsFragment) {
                            navController.navigate(
                                ChatroomMessageListFragmentDirections.actionChatroomMessageListFragmentToEventDetailsFragment(
                                    deepLinkedEvent
                                )
                            )
                        }
                    } else {
                        Toast.makeText(this, Constants.eventAlreadyDeleted, Toast.LENGTH_LONG)
                            .show()
                    }
                    viewModel.setConsumedEventDeepLink(true)
                }
            }
        })
    }

    @ExperimentalPagingApi
    private fun observeChatroom() {
        viewModel.authChatroom.observe(this, Observer { chatroom ->
            if (chatroom != null) {
                Log.i("ChatroomActivity", "Observed chatroom: ${chatroom}")
                // TODO: Remove
                if (supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
                    title = chatroom.name
                }

                val authUserChatroomOwner = viewModel.isAuthUserChatroomOwner.value == true
                val chatroomHasEvents = chatroom.eventIds != null
                val chatroomHasEventsToFetch =
                    chatroomHasEvents && (viewModel.authEvents.value == null || viewModel.authEvents.value?.isEmpty() == true)

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
                    if (binding.tagsGridView.adapter == null) {
                        binding.tagsGridView.adapter = TagListAdapter(
                            it,
                            this@ChatroomActivity,
                            R.layout.chatroom_tag_card
                        )
                    } else {
                        (binding.tagsGridView.adapter as TagListAdapter).setTags(it)
                    }

                    binding.tagsGridView.isVisible = it.isNotEmpty()
                    binding.tagsGridView.setHeightBasedOnChildren(it.size)
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
                refreshDataOnConnectionRefresh()
            } else {
                Log.i("ChatroomActivity", "ChatroomActivity DIS-CONNECTED")
            }
        })
    }

    override fun refreshDataOnConnectionRefresh() {
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
    }

    @ExperimentalPagingApi
    private fun initTopNavigation(chatroomOwner: Boolean) {
        initTopNavigationNav(chatroomOwner)
        setToolbarAdminIconOnOwnershipAndCurrentFragment(chatroomOwner)
    }

    private fun initTopNavigationNav(chatroomOwner: Boolean) {
        with(binding) {
            navViewChatroom.setupWithNavController(navController)

            if (chatroomOwner) {
                navViewAdmin.setupWithNavController(navController)

                drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_UNDEFINED,
                    GravityCompat.START
                )
            } else {
                drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                    GravityCompat.START
                )
                binding.toolbar.setNavigationIconTint(
                    ContextCompat.getColor(
                        this@ChatroomActivity,
                        android.R.color.white
                    )
                )
            }
            toolbar.setupWithNavController(
                navController,
                AppBarConfiguration(setOf(R.id.chatroomMessageListFragment), drawerLayout)
            )
        }
    }

    @ExperimentalPagingApi
    override fun onResume() {
        super.onResume()
        assertGooglePlayAvailability()
        connectServerSocket()
        registerCRUDReceivers()
        // TODO: Move receiver registration after chatroom/users/event fetches!!!

        navController.currentDestination?.let {
            setLabelOnDestination(it)
        }
    }

    override fun onPause() {
        disconnectServerSocket()
        super.onPause()
        unregisterCRUDReceivers()
        prefConfig.writeRestartedFromChatroomTaskRoot(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectServerSocket()
        prefConfig.writeRestartedFromChatroomTaskRoot(false)
        prefConfig.writeReachedBottomMessagesAfterSearch(true) // reset on, well, onDestroy
    }

    // checks if called from push notification while app is killed and restarts entire backstack in that case
    private fun checkNotificationOrDeepLinkCall(): Boolean {
        if (isTaskRoot) {
            Log.i(
                "ChatroomActivity",
                "ChatroomActivity IS TASK ROOT. Regenerating parent activity backstack!"
            )
            val restartIntent = Intent(this, ChatroomActivity::class.java)

            restartIntent.data = intent.data
            restartIntent.putExtras(intent)

            // reset all props for re-fetch
            viewModel.setAuthEvents(null)

            TaskStackBuilder.create(this)
                .addNextIntent(Intent(this, AuthActivity::class.java))
                .addNextIntent(Intent(this, MainActivity::class.java).apply {
                    putExtras(restartIntent)
                })
                .addNextIntent(restartIntent)
                .startActivities(restartIntent.extras)

            prefConfig.writeRestartedFromChatroomTaskRoot(true)
            finishAffinity()
        }
        return isTaskRoot
    }

    // checks if a search query was initiated in ChatroomMessageSearchViewFragment and calls its method if true
    @ExperimentalPagingApi
    private fun handleSearchQuery() {
        if (Intent.ACTION_SEARCH == intent.action) {
            Log.i(
                "ChatroomActivity", "intent for search with query: ${
                    intent.getStringExtra(
                        SearchManager.QUERY
                    )
                }"
            )
            intent.getStringExtra(SearchManager.QUERY)?.also { query ->
                SearchRecentSuggestions(
                    this,
                    MessageSuggestionsProvider.AUTHORITY,
                    MessageSuggestionsProvider.MODE
                )
                    .saveRecentQuery(query, null) // save query

                val searchViewFragment = try {
                    supportFragmentManager.currentNavigationFragment as ChatroomMessageSearchViewFragment?
                } catch (classCast: ClassCastException) {
                    return
                }

                lifecycleScope.launch {
                    searchViewFragment?.filterMessages(query)
                }
            }
        }
    }

    private fun assertGooglePlayAvailability() {
        val googleApiInstance = GoogleApiAvailability.getInstance()
        val availability = googleApiInstance.isGooglePlayServicesAvailable(this)
        if (availability == SERVICE_MISSING || availability == SERVICE_INVALID
            || availability == SERVICE_DISABLED
        ) {
            googleApiInstance.makeGooglePlayServicesAvailable(this)
            // TODO: onBackPressed?
        }
    }

    @ExperimentalPagingApi
    private fun setToolbarAdminIconOnOwnershipAndCurrentFragment(owner: Boolean) {
        if(supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
            setToolbarAdminIconOnOwnership(owner)
        }
    }

    private fun setToolbarAdminIconOnOwnership(owner: Boolean) {
        with(binding.toolbar) {
            setNavigationIconTint(android.graphics.Color.WHITE)
            navigationIcon = if (owner) androidx.core.content.ContextCompat.getDrawable(
                this@ChatroomActivity,
                com.example.hobbyfi.R.drawable.ic_baseline_admin_panel_settings_24
            ) else null
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chatroom_appbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.action_search -> {
                navController.navigate(R.id.action_chatroomMessageListFragment_to_chatroomMessageSearchViewFragment)
            }
            R.id.action_info -> {
                binding.drawerLayout.openDrawer(GravityCompat.END)
            }
            android.R.id.home -> {
                if (!navController.navigateUp(
                        AppBarConfiguration(
                            setOf(R.id.chatroomMessageListFragment),
                            binding.drawerLayout
                        )
                    )
                ) {
                    if (!navController.navigateUp(binding.drawerLayout)) {
                        super.onSupportNavigateUp()
                    }
                }
            }
        }

        return true
    }

    // by default: sholdLeave => shouldExit
    fun handleAuthActionableError(
        error: String?,
        shouldLeave: Boolean, shouldExit: Boolean = shouldLeave,
        context: Context? = null
    ) {
        Toast.makeText(
            context ?: this@ChatroomActivity,
            "Whoops! Looks like something went wrong! $error", Toast.LENGTH_LONG
        ).show()

        if (shouldLeave) {
            finish()
        } else if(shouldExit) {
            leaveChatroomWithRestart()
        }
    }

    private fun handleAuthActionableDeepLinkError(
        shouldExit: Boolean,
        noDeepLinkBlock: () -> Unit
    ) =
        if (checkDeepLinkStatusAndPerform {
                if (shouldExit || !prefConfig.isUserAuthenticated()) {
                    leaveChatroomWithRestart()
                }
            }) {
            true
        } else {
            noDeepLinkBlock()
            false
        }

    private fun reinitCalendarDecorators(events: List<Event>) {
        var emittedErrorForEventParsingAlready = false
        with(binding) {
            eventCalendar.removeDecorators()
            eventCalendar.addDecorator(
                EventCalendarDecorator(
                    ContextCompat.getColor(this@ChatroomActivity, R.color.colorPrimary),
                    events.mapNotNull { try { it.calendarDayFromDate
                        } catch(ex: Exception) {
                            if(!emittedErrorForEventParsingAlready) {
                                emittedErrorForEventParsingAlready = true
                                Toast.makeText(this@ChatroomActivity,
                                    Constants.eventParsingError, Toast.LENGTH_LONG)
                            }
                            null
                        } }
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
                when (motionEvent.action) {
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

    private fun checkDeepLinkStatusAndPerform(block: (() -> Unit)?): Boolean {
        val clickedBranchLink = try {
            viewModel.currentLinkProperties?.get("+clicked_branch_link") as Boolean
        } catch (ex: Exception) {
            false
        }
        Log.i("ChatroomActivity", "latest LINK PROPS: ${viewModel.currentLinkProperties}")

        return if (comeFromAuthDeepLink() || clickedBranchLink) {
            block?.invoke()
            true
        } else false
    }

    // check notification or deeplink by intent extras
    private fun sendChatroomFetchIntentOnCurrentNull() {
        if (viewModel.authChatroom.value == null) {
            lifecycleScope.launch {
                viewModel.sendChatroomIntent(ChatroomIntent.FetchChatroom)
            }
        }
    }

    // deeplink/notification situation
    private fun sendUserIntentFetchIntentOnCurrentNull() {
        if (viewModel.authUser.value == null) {
            lifecycleScope.launch {
                viewModel.sendIntent(UserIntent.FetchUser)
            }
        }
    }

    private fun emitJoinChatroomEventOnCurrentNonNull() {
        viewModel.authUser.value?.id?.let { userId ->
            viewModel.authChatroom.value?.id?.let { chatroomId ->
                if(!sentJoinChatroomEvent) {
                    Log.i("ChatroomActivity", "Emitting join_chatroom event!!!!")

                    serverSocket?.emit(Constants.JOIN_CHATROOM, JSONObject(mapOf(
                        Constants.ID to userId,
                        Constants.CHATROOM_ID to chatroomId
                    )))
                    sentJoinChatroomEvent = true
                } else {
                    Log.w("ChatroomActivity", "Not emitting join_chatroom event due to it already having been emitted")
                }
            }
        }
    }

    private fun getChatroomIdFromDeepLinkProps(
        linkProperties: JSONObject? = viewModel.currentLinkProperties
    ) =
        if (comeFromAuthDeepLink())
            intent.extras?.getDouble(Constants.CHATROOM_ID)?.toLong() else try {
            (linkProperties?.get(Constants.CHATROOM_ID) as String?)?.toLong()
        } catch (ex: Exception) {
            null
        }

    private fun getEventIdFromDeepLinkProps(
        linkProperties: JSONObject? = viewModel.currentLinkProperties
    ) =
        if (comeFromAuthDeepLink()) intent.extras?.getDouble(Constants.EVENT_ID)
            ?.toLong() else try {
            (linkProperties?.get(Constants.EVENT_ID) as String?)?.toLong()
        } catch (ex: Exception) {
            Log.w(
                "ChatroomActivity",
                "Event id not found from extras/latestRefParams; is auth link? ${comeFromAuthDeepLink()}"
            )
            null
        }

    private fun getClickedBranchLinkFromLinkProps(
        linkProperties: JSONObject? = viewModel.currentLinkProperties
    ) = try {
        linkProperties?.get("+clicked_branch_link") as Boolean
    } catch (ex: Exception) {
        Log.w(
            "ChatroomActivity",
            "+clicked_branch_link found from extras/latestRefParams; is auth link? ${comeFromAuthDeepLink()}"
        )
        false
    }

    override fun onBackPressed() {
        if (viewModel.authChatroom.value != null) {
            prefConfig.resetLastEnteredChatroomId()
            if (!isFinishing) {
                super.onBackPressed()
            }

//            Callbacks.unsubscribeToChatroomTopicByCurrentConnectivity(
//                {
//                    prefConfig.resetLastEnteredChatroomId()
//                    if (!isFinishing) {
//                        super.onBackPressed()
//                    }
//                },
//                viewModel.authChatroom.value!!.id,
//                fcmTopicErrorFallback,
//                connectivityManager
//            )
        } else {
            super.onBackPressed()
        }
    }

    private fun registerCRUDReceivers() {
//        chatroomReceiverFactory = ChatroomBroadcastReceiverFactory.getInstance(viewModel, this)
//        editChatroomReceiver =
//            chatroomReceiverFactory!!.createActionatedReceiver(Constants.EDIT_CHATROOM_TYPE)
//        deleteChatroomReceiver =
//            chatroomReceiverFactory!!.createActionatedReceiver(Constants.DELETE_CHATROOM_TYPE)
//        editUserReceiver =
//            chatroomReceiverFactory!!.createActionatedReceiver(Constants.EDIT_USER_TYPE)
//        joinUserReceiver =
//            chatroomReceiverFactory!!.createActionatedReceiver(Constants.JOIN_USER_TYPE)
//        leaveUserReceiver =
//            chatroomReceiverFactory!!.createActionatedReceiver(Constants.LEAVE_USER_TYPE)
//        eventReceiverFactory = EventBroadcastReceiverFactory.getInstance(viewModel, this)
//        createEventReceiver =
//            eventReceiverFactory!!.createActionatedReceiver(Constants.CREATE_EVENT_TYPE)
//        editEventReceiver =
//            eventReceiverFactory!!.createActionatedReceiver(Constants.EDIT_EVENT_TYPE)
//        deleteBatchEventReceiver =
//            eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_BATCH_TYPE)
//        deleteEventReceiver =
//            eventReceiverFactory!!.createActionatedReceiver(Constants.DELETE_EVENT_TYPE)
//
//        with(localBroadcastManager) {
//            registerReceiver(editChatroomReceiver!!, IntentFilter(Constants.EDIT_CHATROOM_TYPE))
//            registerReceiver(deleteChatroomReceiver!!, IntentFilter(Constants.DELETE_CHATROOM_TYPE))
//            registerReceiver(editUserReceiver!!, IntentFilter(Constants.EDIT_USER_TYPE))
//            registerReceiver(joinUserReceiver!!, IntentFilter(Constants.JOIN_USER_TYPE))
//            registerReceiver(leaveUserReceiver!!, IntentFilter(Constants.LEAVE_USER_TYPE))
//            registerReceiver(createEventReceiver!!, IntentFilter(Constants.CREATE_EVENT_TYPE))
//            registerReceiver(editEventReceiver!!, IntentFilter(Constants.EDIT_EVENT_TYPE))
//            registerReceiver(
//                deleteBatchEventReceiver!!,
//                IntentFilter(Constants.DELETE_EVENT_BATCH_TYPE)
//            )
//            registerReceiver(deleteEventReceiver!!, IntentFilter(Constants.DELETE_EVENT_TYPE))
//        }
    }

    private fun unregisterCRUDReceivers() {
//        with(localBroadcastManager) {
//            unregisterReceiver(editChatroomReceiver!!)
//            unregisterReceiver(deleteChatroomReceiver!!)
//            unregisterReceiver(editUserReceiver!!)
//            unregisterReceiver(joinUserReceiver!!)
//            unregisterReceiver(leaveUserReceiver!!)
//            unregisterReceiver(createEventReceiver!!)
//            unregisterReceiver(editEventReceiver!!)
//            unregisterReceiver(deleteBatchEventReceiver!!)
//            unregisterReceiver(deleteEventReceiver!!)
//        }
    }
}