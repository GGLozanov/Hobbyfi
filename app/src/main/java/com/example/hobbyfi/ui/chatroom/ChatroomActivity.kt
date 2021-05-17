package com.example.hobbyfi.ui.chatroom

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.*
import android.content.Intent
import android.os.Bundle
import android.provider.SearchRecentSuggestions
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
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
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.user.ChatroomUserListAdapter
import com.example.hobbyfi.databinding.ActivityChatroomBinding
import com.example.hobbyfi.databinding.NavHeaderChatroomBinding
import com.example.hobbyfi.intents.*
import com.example.hobbyfi.models.data.Chatroom
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.models.data.Message
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.ui.auth.AuthActivity
import com.example.hobbyfi.ui.base.ImageUploadContinuation
import com.example.hobbyfi.ui.base.NavigationActivity
import com.example.hobbyfi.ui.base.RefreshConnectionForegroundFCMReactivationListener
import com.example.hobbyfi.ui.base.ServerSocketAccessor
import com.example.hobbyfi.ui.custom.EventCalendarDecorator
import com.example.hobbyfi.ui.main.MainActivity
import com.example.hobbyfi.ui.onboard.OnboardingActivity
import com.example.hobbyfi.ui.shared.ChatroomTagViewBottomSheetDialogFragment
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.example.hobbyfi.work.DeviceTokenDeleteWorker
import com.example.spendidly.utils.VerticalSpaceItemDecoration
import com.facebook.login.LoginManager
import com.google.android.gms.common.ConnectionResult.*
import com.google.android.gms.common.GoogleApiAvailability
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.*
import io.branch.referral.Branch
import io.branch.referral.Branch.BranchReferralInitListener
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONObject
import java.util.*


@ExperimentalCoroutinesApi
class ChatroomActivity : NavigationActivity(),
        ChatroomMessageBottomSheetDialogFragment.OnMessageOptionSelected, ServerSocketAccessor,
        RefreshConnectionForegroundFCMReactivationListener, ImageUploadContinuation {
    private val viewModel: ChatroomActivityViewModel by viewModels(factoryProducer = {
        AuthUserChatroomViewModelFactory(application, args.user, args.chatroom)
    })

    lateinit var binding: ActivityChatroomBinding
    private val args: ChatroomActivityArgs by navArgs()

    private lateinit var headerBinding: NavHeaderChatroomBinding

    private var userListAdapter: ChatroomUserListAdapter? = null
    private var userStateCollectJob: Job? = null

    override val emitterListenerFactory: EmitterListenerFactory by lazy {
        EmitterListenerFactory(this)
    }

    private val socketEventErrorFallback = { _: Exception ->
        showFailureToast(
            getString(R.string.socket_emission_fail),
        )
        // leaveChatroom()
    }
    
    private val userJoinEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForCreate(
            ::User,
            { user ->
                lifecycleScope.launchWhenStarted {
                    viewModel.sendUsersIntent(
                        UserListIntent.AddAUserCache(
                            user
                        )
                    )
                }
            },
            errorFallback = socketEventErrorFallback
        )
    }

    private val userLeaveEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForCreate(
            ::User,
            { user ->
                lifecycleScope.launchWhenStarted {
                    if (viewModel.authUser.value?.id != user.id) {
                        // account for auth user here because they have to receive broadcast but NOT
                        // have their account deleted from cache
                        viewModel.sendUsersIntent(
                            UserListIntent.DeleteAUserCache(
                                user.id
                            )
                        )
                    } else {
                        viewModel.sendIntent(
                            UserIntent.UpdateUserCache(
                                mapOf(
                                    Constants.CHATROOM_IDS to Constants.jsonConverter.toJson(
                                        viewModel.authUser.value!!.chatroomIds?.filter { id ->
                                            (viewModel.authChatroom.value?.id
                                                ?: error("Leave chatroom Id must not be null in leave user broadcast action for AUTH user!"))
                                                .toLong() != id
                                        }
                                    )
                                )
                            )
                        )
                    }
                }
            },
            errorFallback = socketEventErrorFallback
        )
    }

    private val editChatroomEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForEdit(
            { editFields ->
                lifecycleScope.launchWhenStarted {
                    viewModel.sendChatroomIntent(
                        ChatroomIntent.UpdateChatroomCache(
                            editFields
                        )
                    )
                }
            },
            socketEventErrorFallback
        )
    }

    private val deleteChatroomEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForDelete(
            {
                if (viewModel.isAuthUserChatroomOwner.value == false) {
                    lifecycleScope.launchWhenStarted {
                        viewModel.sendChatroomIntent(
                            ChatroomIntent.DeleteChatroomCache()
                        )
                    }
                }
            },
            socketEventErrorFallback
        )
    }

    private val editUserEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForEdit(
            { editFields ->
                lifecycleScope.launchWhenStarted {
                    viewModel.sendUsersIntent(
                        UserListIntent.UpdateAUserCache(
                            editFields
                        )
                    )
                }
            },
            socketEventErrorFallback
        )
    }

    private val createEventEmitterListener: Emitter.Listener by lazy {
        emitterListenerFactory.createEmitterListenerForCreate(
            ::Event,
            { event ->
                lifecycleScope.launchWhenCreated {
                    viewModel.sendEventsIntent(
                        EventListIntent.AddAnEventCache(
                            event
                        )
                    )
                }
            },
            socketEventErrorFallback
        )
    }

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

    override val serverSocket: Socket? by lazy {
        initSocket()
    }
    @Volatile
    private var sentJoinChatroomSocketEvent = false
    private var initialServerSocketConnect: Boolean = true

    @ExperimentalPagingApi
    private val branchReferralInitListener =
        BranchReferralInitListener { linkProperties, error ->
            val comeFromAuthDeepLink = comeFromAuthDeepLink()
            Log.i(
                "ChatroomActivity",
                "BranchReferralListener triggered; comeFromAuthDeepLink: $comeFromAuthDeepLink; deepLink props: $linkProperties"
            )
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

                    if (getChatroomIdFromDeepLinkProps(safeLinkProps) != viewModel.authChatroom.value?.id) {
                        // force refetch if called from different chatroom
                        viewModel.setChatroom(null)
                        viewModel.setUser(null)
                        viewModel.setChatroomUsers(null)
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
                        sendChatroomFetchIntentOnCurrentNull(
                            getChatroomIdFromDeepLinkProps(safeLinkProps) ?: prefConfig.readLastEnteredChatroomId())
                    }
                }
            } else {
                sendUserIntentFetchIntentOnCurrentNull()
                sendChatroomFetchIntentOnCurrentNull(
                    getChatroomIdFromDeepLinkProps() ?: prefConfig.readLastEnteredChatroomId())
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
        // if activity is in foreground (or in backstack but partially visible), launching the same
        // activity will skip onStart; handle this case with reInitSession
        Branch.sessionBuilder(this)
            .withCallback(branchReferralInitListener)
            .withData(intent?.data).reInit()
        handleSearchQuery()
    }

    @ExperimentalPagingApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewModel = viewModel
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        headerBinding = NavHeaderChatroomBinding.bind(binding.navViewChatroom.getHeaderView(0))
        headerBinding.viewModel = viewModel

        initNavController()
        initDynamicToolbarTitleAndDrawerLock()
        initPushNotificationToggleSwitch()

        Log.i("ChatroomActivity", "intent extras: ${intent.extras?.toReadable()}")
        if (checkNotificationOrDeepLinkCall()) {
            return
        }

        handleSearchQuery()

        assertGooglePlayAvailability()

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

        localBroadcastManager.registerReceiver(
            foregroundFCMReceiver,
            IntentFilter(Constants.FOREGROUND_REACTIVATION_ACTION)
        )
        observeConnectionRefresh(savedInstanceState, refreshConnectivityMonitor)
    }

    // need to do this because it bugs out with up navigation
    private fun initDynamicToolbarTitleAndDrawerLock() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if(destination.id == R.id.loadingFragment) {
                binding.drawerLayout.closeDrawers()
                binding.drawerLayout.setDrawerLockMode(
                    DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                )
            } else binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

            setLabelOnDestination(destination)
        }
    }

    private fun initPushNotificationToggleSwitch() {
        // onClick as to not implicity trigger the switch w/ onCheckedChanged listener
        binding.notificationSwitch.setOnClickListener { view ->
            lifecycleScope.launchWhenCreated {
                viewModel.sendChatroomIntent(
                    ChatroomIntent.TogglePushNotificationForChatroomAuthUser(binding.notificationSwitch.isChecked)
                )
            }
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
            .withData(intent?.data).init()

        observeChatroomState()
        observeChatroom()
        observeChatroomOwnRights()
        observeUsers()
        observeUserState()
        observeUsersState()
        observeEventsState()
        observeEventState()
        observeEvents()

        EventBus.getDefault().register(this)
    }

    override fun onConnectedServerSocketFail() {
        Log.w(
            "ChatroomActivity",
            "Socket connection from current auth user: ${viewModel.authUser.value} failed!"
        )
        if(!viewModel.shownSocketError) {
            viewModel.setShownSocketError(true)
            runOnUiThread {
                buildYesNoAlertDialog(
                    getString(R.string.socket_connection_fail),
                    { _: DialogInterface, _: Int ->
                        leaveChatroom()
                    },
                    null
                )
            }
        }
    }

    @ExperimentalPagingApi
    override fun connectServerSocketListeners() {
        serverSocket?.on(Socket.EVENT_CONNECT) {
            viewModel.setShownSocketError(false)
            Log.i("ChatroomActivity", "initial server socket connect: $initialServerSocketConnect")
            if(!initialServerSocketConnect) {
                refreshDataOnConnectionRefresh()
                (supportFragmentManager
                    .primaryNavigationFragment?.childFragmentManager
                    ?.findFragmentByType<ChatroomMessageListFragment>())?.refreshDataOnConnectionRefresh()
            } else initialServerSocketConnect = false
        }

        serverSocket?.on(Socket.EVENT_DISCONNECT) {
            sentJoinChatroomSocketEvent = false
        }

        serverSocket?.on(Constants.JOIN_USER_TYPE, userJoinEmitterListener)
        serverSocket?.on(Constants.LEAVE_USER_TYPE, userLeaveEmitterListener)
        serverSocket?.on(Constants.EDIT_CHATROOM_TYPE, editChatroomEmitterListener)
        serverSocket?.on(Constants.DELETE_CHATROOM_TYPE, deleteChatroomEmitterListener)
        serverSocket?.on(Constants.EDIT_USER_TYPE, editUserEmitterListener)
        serverSocket?.on(Constants.CREATE_EVENT_TYPE, createEventEmitterListener)
        serverSocket?.on(Constants.EDIT_EVENT_TYPE, editEventEmitterListener)
        serverSocket?.on(Constants.DELETE_EVENT_TYPE, deleteEventEmitterListener)
        serverSocket?.on(Constants.DELETE_EVENT_BATCH_TYPE, deleteEventBatchEmitterListener)
    }

    override fun disconnectServerSocketListeners() {
        sentJoinChatroomSocketEvent = false

        serverSocket?.emit(
            Constants.USER_CEASE_TYPING, JSONObject(
                mapOf(
                    Constants.ID to viewModel.authUser.value?.id,
                    Constants.CHATROOM_ID to viewModel.authChatroom.value?.id
                )
            )
        )
    }

    @ExperimentalPagingApi
    override fun onForegroundReactivation(intent: Intent) {
        val data = JSONObject.wrap(intent.extras)
        // ASSOC MAP GO BRRR
        when(intent.action) {
            Constants.JOIN_USER_TYPE -> {
                userJoinEmitterListener.call(data)
            }
            Constants.LEAVE_USER_TYPE -> {
                userLeaveEmitterListener.call(data)
            }
            Constants.EDIT_CHATROOM_TYPE -> {
                editChatroomEmitterListener.call(data)
            }
            Constants.DELETE_CHATROOM_TYPE -> {
                deleteChatroomEmitterListener.call(data)
            }
            Constants.EDIT_USER_TYPE -> {
                editUserEmitterListener.call(data)
            }
            Constants.CREATE_EVENT_TYPE -> {
                createEventEmitterListener.call(data)
            }
            Constants.EDIT_EVENT_TYPE -> {
                editEventEmitterListener.call(data)
            }
            Constants.DELETE_EVENT_TYPE -> {
                deleteEventEmitterListener.call(data)
            }
            // Actual puke code
            Constants.CREATE_MESSAGE_TYPE -> {
                (supportFragmentManager
                    .primaryNavigationFragment?.childFragmentManager
                    ?.findFragmentByType<ChatroomMessageListFragment>())
                    ?.createMessageEmitterListener?.call(data)
            }
            Constants.EDIT_MESSAGE_TYPE -> {
                (supportFragmentManager
                    .primaryNavigationFragment?.childFragmentManager
                    ?.findFragmentByType<ChatroomMessageListFragment>())
                    ?.editMessageEmitterListener?.call(data)
            }
            Constants.DELETE_MESSAGE_TYPE -> {
                (supportFragmentManager
                    .primaryNavigationFragment?.childFragmentManager
                    ?.findFragmentByType<ChatroomMessageListFragment>())
                    ?.deleteMessageEmitterListener?.call(data)
            }
        }
    }

    private fun observeUserState() {
        userStateCollectJob = lifecycleScope.launchWhenCreated {
            viewModel.mainState.collectLatestWithLoadingAndNonIdleReset(listOf(UserState.Idle::class), viewModel::resetUserState,
                this@ChatroomActivity, navController,
                ChatroomMessageListFragmentDirections.actionChatroomMessageListFragmentToLoadingNavGraph(
                    R.id.chatroomMessageListFragment
                ),
                UserState.Loading::class
            ) {
                Log.i("ChatroomActivity", "Current UserState in Flow collect: ${it.toString()}")
                when (it) {
                    is UserState.Idle -> {

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
                                sendChatroomFetchIntentOnCurrentNull(deepLinkChatroomId)
                            } else {
                                this@ChatroomActivity.showFailureToast(
                                    getString(R.string.not_joined_chatroom_error)
                                )
                                leaveChatroom(sendExtrasBack = true)
                            }
                        }
                    }
                    is UserState.Error -> {
                        handleAuthActionableDeepLinkError(it.shouldReauth) {
                            handleAuthActionableError(it.error, true, it.shouldReauth)
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
        lifecycleScope.launchWhenCreated {
            viewModel.chatroomState.collectLatestWithLoadingAndNonIdleReset(
                listOf(ChatroomState.Idle::class, ChatroomState.OnData.ChatroomResult::class), viewModel::resetChatroomState,
                this@ChatroomActivity, navController,
                ChatroomMessageListFragmentDirections.actionChatroomMessageListFragmentToLoadingNavGraph(
                    R.id.chatroomMessageListFragment
                ),
                ChatroomState.Loading::class
            ) {
                when (it) {
                    is ChatroomState.Idle, is ChatroomState.OnData.ChatroomResult -> {

                    }
                    is ChatroomState.OnData.ChatroomDeleteResult -> {
                        this@ChatroomActivity.showSuccessToast(
                            getString(R.string.chatroom_delete_success)
                        )
                        leaveDeletedChatroom()
                    }
                    is ChatroomState.OnData.DeleteChatroomCacheResult -> {
                        this@ChatroomActivity.showSuccessToast(
                            getString(if(it.kicked) R.string.chatroom_kicked_message
                                else R.string.chatroom_deleted_message)
                        )
                        leaveDeletedChatroom()
                    }
                    is ChatroomState.OnData.ChatroomUpdateResult -> {
                        if (it.fieldMap.containsKey(Constants.IMAGE)) {
                            WorkerUtils.buildAndEnqueueImageUploadWorker(
                                viewModel.authChatroom.value!!.id,
                                prefConfig.getAuthUserToken()!!,
                                Constants.EDIT_CHATROOM_TYPE,
                                it.fieldMap[Constants.IMAGE]!!,
                                this@ChatroomActivity,
                                R.string.pref_last_chatrooms_fetch_time
                            )
                        }

                        this@ChatroomActivity.showSuccessToast(
                            getString(R.string.chatroom_updated),
                        )
                    }
                    is ChatroomState.Error -> {
                        handleAuthActionableDeepLinkError(it.shouldExit) {
                            handleAuthActionableError(it.error, true, it.shouldExit)
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
                    viewModel.authChatroom.value?.id ?: prefConfig.readLastEnteredChatroomId()
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
            val goBackToAuth = {
                startActivity(Intent(this, leaveDestination).apply {
                    linkParams?.toBundle()?.let {
                        putExtras(it)
                    }
                    putExtras(intent)
                })
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }

            viewModel.setChatroom(null) // clear chatroom in any case
            if (linkParams != null) {
                goBackToAuth()
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
                } else if(!prefConfig.isUserAuthenticated()) goBackToAuth() else finish()
            }
        }

        leave()
    }

    private fun leaveChatroomWithDelete() {
        lifecycleScope.launch {
            viewModel.sendChatroomIntent(ChatroomIntent.DeleteChatroomCache(true))
        }
    }

    // for deeplink & shouldExit errors
    private fun leaveChatroomWithRestart(
        exitMsg: String = getString(R.string.invalid_access_error),
        linkParams: JSONObject? = viewModel.currentLinkProperties,
        leaveDestination: Class<*> = AuthActivity::class.java
    ) {
        // TODO: More graceful way to show this error... like a separate screen? Activity?
        this@ChatroomActivity.showWarningToast(exitMsg)

        // log out of all accounts if logged in (use refresh token to authorise properly)
        if(prefConfig.isUserAuthenticated()) {
            WorkerUtils.buildAndEnqueueDeviceTokenWorker<DeviceTokenDeleteWorker>(
                prefConfig.getAuthUserTokenRefresh()!!,
                prefConfig.readDeviceToken(), this
            )
            LoginManager.getInstance().logOut()
            prefConfig.resetToken()
            prefConfig.resetRefreshToken()
        }

        leaveChatroom(linkParams, leaveDestination = leaveDestination)
    }

    private fun observeUsers() {
        viewModel.chatroomUsers.distinctUntilChanged().observe(this, Observer {
            Log.i("ChatroomActivity", "auth user in users observe: ${viewModel.authUser.value}")
            Log.i("ChatroomActivity", "observed users: $it")
            if (it.isNotEmpty() && !it.contains(viewModel.authUser.value) &&
                    viewModel.isAuthUserChatroomOwner.value == false) {
                leaveChatroomWithDelete()
                return@Observer
            }

            userListAdapter?.setUsers(it)
        })
    }

    private fun observeUsersState() {
        lifecycleScope.launchWhenStarted {
            viewModel.usersState.collect {
                when (it) {
                    is UserListState.Idle, is UserListState.OnData.UsersResult -> {

                    }
                    is UserListState.Loading -> {
                        // TODO: Progressbar on RecyclerView
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

                        this@ChatroomActivity.showSuccessToast(getString(R.string.user_kick_success))
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
                        this@ChatroomActivity.showSuccessToast(getString(R.string.delete_old_events_success))
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
            viewModel.eventState.collectLatestWithNonIdleReset(
                    listOf(EventState.Idle::class, EventState.Loading::class), viewModel::resetEventState) {
                when(it) {
                    is EventState.Idle -> {

                    }
                    is EventState.Loading -> {
                        // TODO: Progressbar? Somewhere?
                    }
                    is EventState.OnData.EventDeleteResult -> {
                        this@ChatroomActivity.showSuccessToast(
                            getString(R.string.event_delete_success)
                        )
                    }
                    is EventState.Error -> {
                        // TODO: Handle error
                        handleAuthActionableError(it.error, it.shouldReauth)
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
                            navController.safeNavigate(
                                ChatroomMessageListFragmentDirections.actionChatroomMessageListFragmentToEventDetailsFragment(
                                    deepLinkedEvent
                                )
                            )
                        }
                    } else {
                        this@ChatroomActivity.showWarningToast(getString(R.string.event_deleted))
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
                binding.notificationSwitch.isChecked =
                    viewModel.authUser.value?.allowedPushChatroomIds
                        ?.contains(chatroom.id) == true
                emitJoinChatroomEventOnChatroomObserve(chatroom)
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
                                    getString(R.string.confirm_chatroom_delete),
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
                    it.asFirebaseStorageReference()?.let { ref ->
                        ref.metadata.addOnSuccessListener { metadata ->
                            Glide.with(this@ChatroomActivity)
                                .loadReferenceWithMetadataSignature(ref, metadata)
                                .placeholder(headerBinding.chatroomImage.drawable)
                                .into(headerBinding.chatroomImage)
                        }
                    }
                }

                headerBinding.tagsViewButton.setOnClickListener {
                    supportFragmentManager.showDistinctDialog(
                        "ChatroomSheet" + chatroom.id.toString(),
                        {
                            ChatroomTagViewBottomSheetDialogFragment.newInstance(chatroom.name)
                        }
                    )
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
            // enter without internet (currently refetches old chatrooms in itiial joins)
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
                ChatroomIntent.FetchChatroom(prefConfig.readLastEnteredChatroomId(),
                    navController.currentDestination?.id)
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
        if(!initialServerSocketConnect && serverSocket?.connected() == false) {
            connectServerSocket()
        }
        assertGooglePlayAvailability()

        navController.currentDestination?.let {
            setLabelOnDestination(it)
        }
    }

    override fun onPause() {
        disconnectServerSocket()
        super.onPause()
        prefConfig.writeRestartedFromChatroomTaskRoot(false)
    }

    override fun onStop() {
        disconnectServerSocket()
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(foregroundFCMReceiver)
        prefConfig.writeRestartedFromChatroomTaskRoot(false)
        prefConfig.writeReachedBottomMessagesAfterSearch(true) // reset on, well, onDestroy
    }

    // checks if called from push notification while app is killed and restarts entire backstack in that case
    private fun checkNotificationOrDeepLinkCall(): Boolean {
        if (isTaskRoot) {
            if(!prefConfig.isUserAuthenticated()) {
                // CANNOT access this without auth
                leaveChatroomWithRestart(getString(R.string.no_connection_or_root_auth_error))
                return true
            }

            Log.i(
                "ChatroomActivity",
                "ChatroomActivity IS TASK ROOT. Regenerating parent activity backstack!"
            )
            val restartIntent = Intent(this, ChatroomActivity::class.java)

            restartIntent.data = intent.data
            restartIntent.putExtras(intent)

            // check if notification and open appropriate chatroom
            intent.getLongArrayExtra(Constants.ROOM_IDS)?.let {
                // one of them ought to be right
                // if just chatroom notification => array has only 1 element and will return the required chatroom id
                prefConfig.writeLastEnteredChatroomId(it.random())
            }

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
                navController.safeNavigate(R.id.action_chatroomMessageListFragment_to_chatroomMessageSearchViewFragment)
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

        return false
    }

    // by default: sholdLeave => shouldExit
    fun handleAuthActionableError(
        error: String?,
        shouldLeave: Boolean, shouldExit: Boolean = shouldLeave,
        context: Context? = null
    ) {
        showFailureToast(
            getString(R.string.something_wrong) + " $error"
        )

        if (shouldLeave) {
            finish()
        } else if(shouldExit || !prefConfig.isUserAuthenticated()) {
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
                    events.mapNotNull {
                        try {
                            it.calendarDayFromDate
                        } catch (ex: Exception) {
                            if (!emittedErrorForEventParsingAlready) {
                                emittedErrorForEventParsingAlready = true
                                this@ChatroomActivity.showFailureToast(
                                    getString(R.string.event_date_parse_fail)
                                )
                            }
                            null
                        }
                    }
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
    private fun sendChatroomFetchIntentOnCurrentNull(chatroomId: Long) {
        if (viewModel.authChatroom.value == null) {
            lifecycleScope.launch {
                viewModel.sendChatroomIntent(
                    ChatroomIntent.FetchChatroom(chatroomId, navController.currentDestination?.id)
                )
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

    private fun emitJoinChatroomEventOnChatroomObserve(chatroom: Chatroom) {
        viewModel.authUser.value?.id?.let { userId ->
            chatroom.id.let { chatroomId ->
                if(!sentJoinChatroomSocketEvent ||
                        chatroomId != viewModel.lastSentJoinChatroomSocketEventId) {
                    Log.i("ChatroomActivity", "Emitting join_chatroom event!!!!")

                    serverSocket?.emit(
                        Constants.JOIN_CHATROOM, JSONObject(
                            mapOf(
                                Constants.ID to userId,
                                Constants.CHATROOM_ID to chatroomId
                            )
                        )
                    )
                    viewModel.setLastSentJoinChatroomSocketEventId(chatroomId)
                    sentJoinChatroomSocketEvent = true
                } else {
                    Log.w(
                        "ChatroomActivity",
                        "Not emitting join_chatroom event due to it already having been emitted"
                    )
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
            // prefConfig.resetLastEnteredChatroomId()
            if (!isFinishing) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    // WorkManager worker used to only update repo but since Chatroom/User/Event isn't constantly being collected from both network & db
    // then the propagation of the update to the VM was nonexistent, and the Chatroom/User/Event instances remained with the old photo urls
    // so I just used EventBus from a WorkManager Worker :P
    // FIXME: Also slight code dup
    @Subscribe(threadMode = ThreadMode.MAIN)
    override fun onImageUploadEvent(event: WorkerUtils.ImageUploadEvent) {
        Log.i("ChatroomActivity", "Received onImageUploadEvent w/ event: ${event}")
        when(event.type) {
            Constants.CHATROOMS, Constants.EDIT_CHATROOM_TYPE -> {
                lifecycleScope.launch {
                    viewModel.sendChatroomIntent(
                        ChatroomIntent.UpdateChatroomCache(mapOf(Constants.IMAGE to event.response))
                    )
                }
            }
            Constants.EVENTS, Constants.EDIT_EVENT_TYPE -> {
                lifecycleScope.launch {
                    viewModel.sendEventsIntent(
                        EventListIntent.UpdateAnEventCache(mapOf(
                            Constants.ID to event.modelId.toString(), Constants.PHOTO_URL to event.response))
                    )
                }
            }
            Constants.USERS, Constants.EDIT_USER_TYPE -> {
                lifecycleScope.launch {
                    viewModel.sendIntent(
                        UserIntent.UpdateUserCache(mapOf(Constants.IMAGE to event.response))
                    )
                }
            }
        }
    }
}