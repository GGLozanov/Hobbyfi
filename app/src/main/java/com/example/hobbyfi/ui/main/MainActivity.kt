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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.currentNavigationFragment
import com.example.hobbyfi.shared.setupWithNavController
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.OnAuthStateReset
import com.example.hobbyfi.viewmodels.factories.AuthUserViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.facebook.login.LoginManager
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import org.kodein.di.generic.instance


@ExperimentalCoroutinesApi
class MainActivity : BaseActivity(), OnAuthStateReset {
    val viewModel: MainActivityViewModel by viewModels(factoryProducer = {
        AuthUserViewModelFactory(application, if(intent.extras != null) args.user else null)
    })
    lateinit var binding: ActivityMainBinding
    private val args: MainActivityArgs by navArgs()

    private var poppedFromNavController: Boolean = false

    private val fcmTopicErrorFallback: OnFailureListener by instance(
        tag = "fcmTopicErrorFallback",
        MainApplication.applicationContext
    )

    private val authStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == Constants.LOGOUT) {
                logout()
            } else {
                Log.wtf("MainActivity", "MainActivity authStateReceiver called with incorrect intent action. THIS SHOULD NEVER HAPPEN!!!!")
            }
        }
    }

    private val chatroomDeletedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == Constants.CHATROOM_DELETED) {
                viewModel.setLatestUserUpdateFields(mapOf(
                    Pair(Constants.CHATROOM_ID, "0")
                ))
                viewModel.setLeftChatroom(true)
            } else {
                Log.wtf("MainActivity", "MainActivity chatroomDeletedReceiver called with incorrect intent action. THIS SHOULD NEVER HAPPEN!!!!")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        registerReceiver(chatroomDeletedReceiver, IntentFilter(Constants.CHATROOM_DELETED))
        registerReceiver(authStateReceiver, IntentFilter(Constants.LOGOUT))

        with(binding) {
            val view = root
            setContentView(view)
            setSupportActionBar(toolbar)

            bottomNav.setOnNavigationItemReselectedListener {
                // avoid fragment recreation (do nothing here)
            }

            bottomNav.selectedItemId = R.id.userProfileFragment

            bottomNav.setupWithNavController(
                navGraphIds = listOf(R.navigation.user_profile_nav_graph, R.navigation.chatroom_list_nav_graph),
                fragmentManager = supportFragmentManager,
                containerId = R.id.nav_host_fragment,
                intent = intent
            ).observe(this@MainActivity, Observer {
                navController = it
                toolbar.setupWithNavController(it, AppBarConfiguration(setOf(
                    R.id.userProfileFragment,
                    R.id.chatroomListFragment
                )))
            })
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {
                    is UserState.Idle -> {

                    }
                    is UserState.Loading -> {
                        // TODO: Progressbar
                    }
                    is UserState.OnData.UserResult -> {
                        // TODO: Something
                    }
                    is UserState.OnData.UserDeleteResult -> {
                        Toast.makeText(this@MainActivity, "Successfully deleted account!", Toast.LENGTH_LONG)
                            .show()
                        logout()
                    }
                    is UserState.OnData.UserUpdateResult -> {
                        // FIXME: This feels quite coupled as it exposes the knowledge of the fragment
                        //  ... but I can't think of any other alternative for now

                        if(it.userFields.size == 1 && it.userFields.containsKey(Constants.CHATROOM_ID)) {
                            if(it.userFields[Constants.CHATROOM_ID]?.toInt() != 0) {
                                // if user has updated only their chatroom and not left a room (though ChatroomListFragment)
                                viewModel.setJoinedChatroom(true)
                            } else {
                                viewModel.setLeftChatroom(true)
                            }
                            viewModel.setLatestUserUpdateFields(it.userFields) // update later in observers in fragment
                        } else {
                            viewModel.sendIntent(
                                UserIntent.UpdateUserCache(it.userFields)
                            )
                            Toast.makeText(this@MainActivity, "Successfully updated fields!", Toast.LENGTH_LONG)
                                .show()
                        }
                        viewModel.resetState()
                    }
                    is UserState.Error -> {
                        Toast.makeText(this@MainActivity, "Something went wrong! ${it.error}", Toast.LENGTH_LONG)
                            .show()
                        if(it.shouldReauth) {
                            logout()
                        }
                        viewModel.resetState()
                    }
                }
            }
        }
    }

    private fun resetAuthProperties() {
        LoginManager.getInstance().logOut()
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_user_fetch_time)
        prefConfig.resetToken()
        prefConfig.resetRefreshToken()
    }

    // FIXME: Bad way to handle backstack
    private fun resetAuth() {
        if(viewModel.authUser.value?.chatroomId != null) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(Constants.chatroomTopic(
                viewModel.authUser.value?.chatroomId!!)).addOnCompleteListener {
                resetAuthProperties()
            }.addOnFailureListener(fcmTopicErrorFallback)
        } else {
            resetAuthProperties()
        }
    }

    override fun logout() {
        poppedFromNavController = true
        onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_appbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if(item.itemId == R.id.action_logout) {
            logout()
        }

        return true
    }

    // TODO: Find a way to handle backstack when logout is pressed after register
    override fun onBackPressed() {
        if(poppedFromNavController) {
            resetAuth()
            finish()
            return
        }

        // FIXME: Fix this ass-backwards logic and backstack management hack aaaaaaaaaaaaaaaaaaaaaaa
        if(supportFragmentManager.currentNavigationFragment is ChatroomCreateFragment) {
            super.onBackPressed()
            return
        }

        resetAuth()
        finishAffinity()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chatroomDeletedReceiver)
        unregisterReceiver(authStateReceiver)
    }
}