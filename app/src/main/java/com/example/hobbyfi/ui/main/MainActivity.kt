package com.example.hobbyfi.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.auth.AuthActivity
import com.example.hobbyfi.ui.base.*
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.factories.AuthUserViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.example.hobbyfi.work.DeviceTokenDeleteWorker
import com.facebook.login.LoginManager
import io.socket.client.Socket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.json.JSONObject


@ExperimentalCoroutinesApi
class MainActivity : NavigationActivity(), OnAuthStateReset,
        ServerSocketAccessor {
    val viewModel: MainActivityViewModel by viewModels(factoryProducer = {
        AuthUserViewModelFactory(application, if(intent.extras != null) args.user else null)
    })
    lateinit var binding: ActivityMainBinding
    private val args: MainActivityArgs by navArgs()

    private var poppedFromLogoutButton: Boolean = false

    @Volatile
    private var sentEnterMainSocketEvent: Boolean = false

    private val chatroomDeletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == Constants.CHATROOM_DELETED) {
                viewModel.setLatestUserUpdateFields(mapOf(
                    Pair(Constants.CHATROOM_IDS,
                        Constants.jsonConverter.toJson(
                            viewModel.authUser.value?.chatroomIds?.filter
                            { it != intent.getLongExtra(Constants.CHATROOM_ID, 0) }
                        )
                    )))
                viewModel.setLeftChatroom(true)
            } else {
                Log.wtf(
                    "MainActivity",
                    "MainActivity chatroomDeletedReceiver called with incorrect intent action. THIS SHOULD NEVER HAPPEN!!!!"
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        Log.i("MainActivity", "intent extras: ${intent.extras?.toReadable()}")

        localBroadcastManager.registerReceiver(chatroomDeletedReceiver, IntentFilter(Constants.CHATROOM_DELETED))

        with(binding) {
            val view = root
            setContentView(view)
            setSupportActionBar(binding.toolbar)
            if(savedInstanceState == null) {
                initNavController()
            } // safeguard for dupping the navcontroller setup

            bottomNav.setOnNavigationItemReselectedListener {
                // avoid fragment recreation (do nothing here)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectServerSocket()
        observeAuthUser()
        viewModel.setDeepLinkExtras(if(comeFromAuthDeepLink()
            && viewModel.deepLinkExtras == null) intent.extras else null
        )
        Log.i("MainActivity", "VM deeplink extras: ${viewModel.deepLinkExtras?.toReadable()}")
    }

    override fun onResume() {
        super.onResume()

        if(!sentEnterMainSocketEvent && serverSocket?.connected() == false) {
            connectServerSocket()
        }

        initNavController()
    }

    override fun onPause() {
        disconnectServerSocket()
        super.onPause()
    }

    override fun initNavController() {
        binding.bottomNav.setupWithNavController(
            navGraphIds = listOf(
                R.navigation.user_profile_nav_graph,
                R.navigation.joined_chatroom_nav_graph,
                R.navigation.chatroom_list_nav_graph
            ),
            fragmentManager = supportFragmentManager,
            containerId = R.id.nav_host_fragment,
            intent = intent
        ).observe(this@MainActivity, Observer {
            navController = it
            binding.toolbar.setupWithNavController(
                navController, AppBarConfiguration(
                    setOf(
                        R.id.userProfileFragment,
                        R.id.chatroomListFragment,
                        R.id.joinedChatroomListFragment
                    )
                )
            )
            observeUserState()
            navController.addOnDestinationChangedListener { _, destination, _ ->
                if(destination.id == R.id.changeEmailDialogFragment || destination.id == R.id.changePasswordDialogFragment) {
                    binding.toolbar.title = "Your Profile"
                }

                binding.bottomNav.isVisible = destination.id != R.id.chatroomCreateFragment &&
                        destination.id != R.id.tagSelectionFragment && destination.id != R.id.customTagCreateDialogFragment
            }
        })
    }

    private fun observeUserState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collectLatestWithLoading(navController,
                    R.id.action_userProfileFragment_to_loading_nav_graph,
                    UserState.Loading::class, listOf(binding.bottomNav)) {
                when(it) {
                    is UserState.Idle, is UserState.OnData.UserResult -> {

                    }
                    is UserState.OnData.UserDeleteResult -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Successfully deleted account!",
                            Toast.LENGTH_LONG
                        ).show()
                        logout()
                    }
                    is UserState.OnData.UserUpdateResult -> {
                        // FIXME: This feels quite coupled as it exposes the knowledge of the fragment
                        //  ... but I can't think of any other alternative for now
                        if(it.userFields.containsKey(Constants.IMAGE)) {
                            WorkerUtils.buildAndEnqueueImageUploadWorker(
                                viewModel.authUser.value!!.id,
                                prefConfig.getAuthUserToken()!!,
                                Constants.EDIT_USER_TYPE,
                                it.userFields[Constants.IMAGE]!!,
                                this@MainActivity,
                                R.string.pref_last_user_fetch_time
                            )
                        }

                        val hasJoinedChatroom = it.userFields.containsKey(Constants.CHATROOM_ID)
                        val hasLeftChatroom = it.userFields.containsKey(Constants.LEAVE_CHATROOM_ID)
                        if (hasJoinedChatroom || hasLeftChatroom) {
                            // if user has updated only their chatroom and not left a room (though ChatroomListFragment)
                            lateinit var userChatroomFields: Map<String, String?>
                            if (hasJoinedChatroom) {
                                userChatroomFields = mapOf(
                                    Constants.CHATROOM_IDS to Constants.jsonConverter.toJson(
                                        viewModel.authUser.value!!.chatroomIds?.plus(it.userFields[Constants.CHATROOM_ID])
                                            ?: listOf(it.userFields[Constants.CHATROOM_ID])
                                    ),
                                )
                                viewModel.setLatestUserUpdateFields(userChatroomFields) // update later in observers in fragment
                                viewModel.setJoinedChatroom(hasJoinedChatroom)
                            }
                            if (hasLeftChatroom) {
                                userChatroomFields = mapOf(
                                    Constants.CHATROOM_IDS to Constants.jsonConverter.toJson(
                                        viewModel.authUser.value!!.chatroomIds?.filter { id -> (it.userFields[Constants.LEAVE_CHATROOM_ID]
                                                ?: error("Leave chatroom Id must not be null in collection of UpdateUser state in MainActivity"))
                                            .toLong() != id }
                                    ),
                                    Constants.ALLOWED_PUSH_CHATROOM_IDS to Constants.jsonConverter.toJson(
                                        viewModel.authUser.value!!.allowedPushChatroomIds?.filter { id -> (it.userFields[Constants.LEAVE_CHATROOM_ID]
                                                ?: error("Leave chatroom Id must not be null in collection of UpdateUser state in MainActivity"))
                                            .toLong() != id  }
                                    )
                                )
                                viewModel.setLatestUserUpdateFields(userChatroomFields) // update later in observers in fragment
                                viewModel.setLeftChatroom(hasLeftChatroom)
                            }
                        } else {
                            viewModel.sendIntent(
                                UserIntent.UpdateUserCache(it.userFields)
                            )
                            Toast.makeText(
                                this@MainActivity,
                                "Successfully updated fields!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        viewModel.resetState()
                        viewModel.setIsUserProfileUpdateButtonEnabled(true)
                    }
                    is UserState.Error -> {
                        Toast.makeText(
                            this@MainActivity,
                            "Something went wrong! ${it.error}",
                            Toast.LENGTH_LONG
                        ).show()
                        if (it.shouldReauth) {
                            logout()
                        }
                        viewModel.resetState()
                        viewModel.setIsUserProfileUpdateButtonEnabled(true)
                    }
                }
            }
        }
    }

    override fun logout() {
        poppedFromLogoutButton = true
        resetAuthProperties()
        onBackPressed()
    }

    private fun resetAuthProperties() {
        WorkerUtils.buildAndEnqueueDeviceTokenWorker<DeviceTokenDeleteWorker>(
            prefConfig.getAuthUserTokenRefresh()!!,
            prefConfig.readDeviceToken(), this
        )
        LoginManager.getInstance().logOut()
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_user_fetch_time)
        prefConfig.resetToken()
        prefConfig.resetRefreshToken()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_appbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if(item.itemId == R.id.action_logout) {
            Log.i("MainActivity", "current nav entry: ${navController.currentBackStackEntry?.destination?.toString()}")
            logout()
        }

        return true
    }

    // delegation not allowed in interfaces :(
    override val serverSocket: Socket? by lazy {
        initSocket()
    }
    override val emitterListenerFactory: EmitterListenerFactory by lazy {
        EmitterListenerFactory(this)
    }

    override fun onConnectedServerSocketFail() {
        Log.w("MainActivity", "User failed to connect to external FCM monitoring socket from MainActivity!")
    }

    override fun connectServerSocketListeners() {
        serverSocket?.on(Socket.EVENT_DISCONNECT) {
            sentEnterMainSocketEvent = false
        }
        
        serverSocket?.on(Socket.EVENT_CONNECT) {
            emitEnterMainEventOnUserObserve(viewModel.authUser.value)
        }
    }

    private fun observeAuthUser() {
        viewModel.authUser.observe(this, Observer {
            emitEnterMainEventOnUserObserve(it)
        })
    }

    private fun emitEnterMainEventOnUserObserve(user: User?) {
        user?.id?.let {
            if(!sentEnterMainSocketEvent) {
                Log.i("MainActivity", "Emitting enter_main event!!!!")

                serverSocket?.emit(Constants.ENTER_MAIN, JSONObject(mapOf(
                    Constants.ID to it,
                )))
                sentEnterMainSocketEvent = true
            } else {
                Log.w("MainActivity", "Not emitting enter_main event due to it already having been emitted")
            }
        }
    }

    override fun onBackPressed() {
        if(poppedFromLogoutButton) {
            // FIXME: Is restarting the activity again good? There shouldn't be any state preserved but this cheats the navcomponent backstack a bit
            startActivity(Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        } else {
            if(navController.currentBackStackEntry?.destination?.id == R.id.userProfileFragment) {
                resetAuthProperties()
                finishAffinity()
            } else super.onBackPressed()
        }
    }

    override fun onStop() {
        disconnectServerSocket()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(chatroomDeletedReceiver)
    }

    override fun disconnectServerSocketListeners() {
        sentEnterMainSocketEvent = false
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}