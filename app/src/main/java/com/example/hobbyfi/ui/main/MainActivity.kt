package com.example.hobbyfi.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.get
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.NavigationActivity
import com.example.hobbyfi.ui.base.OnAuthStateReset
import com.example.hobbyfi.ui.chatroom.ChatroomMessageListFragment
import com.example.hobbyfi.viewmodels.factories.AuthUserViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.facebook.login.LoginManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect


@ExperimentalCoroutinesApi
class MainActivity : NavigationActivity(), OnAuthStateReset {
    val viewModel: MainActivityViewModel by viewModels(factoryProducer = {
        AuthUserViewModelFactory(application, if(intent.extras != null) args.user else null)
    })
    lateinit var binding: ActivityMainBinding
    private val args: MainActivityArgs by navArgs()

    private var poppedFromLogoutButton: Boolean = false

    private val authStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == Constants.LOGOUT) {
                logout()
            } else {
                Log.wtf(
                    "MainActivity",
                    "MainActivity authStateReceiver called with incorrect intent action. THIS SHOULD NEVER HAPPEN!!!!"
                )
            }
        }
    }

    private val chatroomDeletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == Constants.CHATROOM_DELETED) {
                // TODO: Remove chatroom ID here from user list one-to-many connection
                viewModel.setLatestUserUpdateFields(mapOf(
                    Pair(Constants.CHATROOM_IDS,
                        Constants.tagJsonConverter.toJson(
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

        viewModel.setDeepLinkExtras(if(comeFromAuthDeepLink()
            && viewModel.deepLinkExtras == null) intent.extras else null
        )
        Log.i("MainActivity", "VM deeplink extras: ${viewModel.deepLinkExtras?.toReadable()}")

        localBroadcastManager.registerReceiver(chatroomDeletedReceiver, IntentFilter(Constants.CHATROOM_DELETED))
        localBroadcastManager.registerReceiver(authStateReceiver, IntentFilter(Constants.LOGOUT))

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
        observeUserState()
    }

    override fun onResume() {
        super.onResume()
        initNavController()
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
        })
    }

    private fun observeUserState() {
        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {
                    is UserState.Idle, is UserState.OnData.UserResult -> {

                    }
                    is UserState.Loading -> {
                        // TODO: Progressbar
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
                        val hasJoinedChatroom = it.userFields.containsKey(Constants.CHATROOM_ID)
                        val hasLeftChatroom = it.userFields.containsKey(Constants.LEAVE_CHATROOM_ID)
                        if (hasJoinedChatroom || hasLeftChatroom) {
                            // if user has updated only their chatroom and not left a room (though ChatroomListFragment)
                            lateinit var userChatroomFields: Map<String?, String?>
                            if (hasJoinedChatroom) {
                                userChatroomFields = mapOf(
                                    Pair(
                                        Constants.CHATROOM_IDS, Constants.tagJsonConverter.toJson(
                                            viewModel.authUser.value!!.chatroomIds?.plus(it.userFields[Constants.CHATROOM_ID])
                                                ?: listOf(it.userFields[Constants.CHATROOM_ID])
                                        )
                                    )
                                )
                                viewModel.setLatestUserUpdateFields(userChatroomFields) // update later in observers in fragment
                                viewModel.setJoinedChatroom(hasJoinedChatroom)
                            }
                            if (hasLeftChatroom) {
                                userChatroomFields = mapOf(
                                    Pair(
                                        Constants.CHATROOM_IDS, Constants.tagJsonConverter.toJson(
                                            viewModel.authUser.value!!.chatroomIds?.filter { id -> (it.userFields[Constants.LEAVE_CHATROOM_ID]
                                                    ?: error("Leave chatroom Id must not be null in collection of UpdateUser state in MainActivity"))
                                                .toLong() != id }
                                        )
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

    // TODO: Find a way to handle backstack when logout is pressed after register
    override fun onBackPressed() {
        if(poppedFromLogoutButton) {
            finish()
        } else {
            if(navController.currentBackStackEntry?.destination?.id == R.id.userProfileFragment) {
                resetAuthProperties()
                finishAffinity()
            } else super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(chatroomDeletedReceiver)
        localBroadcastManager.unregisterReceiver(authStateReceiver)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}