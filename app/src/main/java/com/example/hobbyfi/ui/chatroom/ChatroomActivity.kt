package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityChatroomBinding
import com.example.hobbyfi.intents.ChatroomIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.currentNavigationFragment
import com.example.hobbyfi.state.ChatroomState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.viewmodels.chatroom.ChatroomActivityViewModel
import com.example.hobbyfi.viewmodels.factories.AuthChatroomViewModelFactory
import com.example.hobbyfi.viewmodels.factories.AuthUserViewModelFactory
import com.google.android.gms.common.ConnectionResult.*
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_chatroom.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomActivity : BaseActivity() {
    // TODO: Handle navdrawer, rendering of fragments, activity intent calls, button listeners, etc. . .
    // TODO: Have user & chatroom info passed in (or fetched from cache) & send request for messages, users, & event
    // TODO: Disable event create button if event is already created

    // TODO: research into integrating nav drawer different icons with navcomponent
    // TODO: need to integrate action bar menu and have drawable from there trigger right navigationview

    // TODO: Have this be the deeplink activity. Register it and handle intent extras and facebook/default user

    // TODO: If user leaves chatroom (not exits), delete messages saved in local db + users

    private val viewModel: ChatroomActivityViewModel by viewModels(factoryProducer = {
        AuthChatroomViewModelFactory(application, args.user, args.chatroom)
    })
    private lateinit var binding: ActivityChatroomBinding
    private val args: ChatroomActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatroomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        assertGooglePlayAvailability()

        if(viewModel.authUser.value == null && viewModel.authChatroom.value == null) {
            // deeplink situation
            lifecycleScope.launch {
                viewModel.sendIntent(UserIntent.FetchUser)
                viewModel.sendChatroomIntent(ChatroomIntent.FetchChatroom)
            }
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

    override fun onStart() {
        super.onStart()

        nav_view_chatroom.setupWithNavController(navController)

        lifecycleScope.launch {
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

                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.chatroomState.collect {
                when(it) {
                    is ChatroomState.Idle -> {

                    }
                    is ChatroomState.OnData.ChatroomResult -> {
                        viewModel.setChatroom(it.chatroom)

                    }
                    is ChatroomState.OnData.ChatroomDeleteResult -> {

                    }
                    is ChatroomState.OnData.ChatroomUpdateResult -> {

                    }
                    is ChatroomState.OnNotification.DeleteChatroomNotification -> {

                    }
                    is ChatroomState.OnNotification.UpdateChatroomNotification -> {
                        // update name and/or tags + description in right navigation view
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }

        observeChatroom()
        observeChatroomOwnRights()
    }

    private fun observeChatroom() {
        viewModel.authChatroom.observe(this, Observer {
            title = it?.name

            if(it?.lastEventId == null) {
                nav_view_admin.inflateMenu(R.menu.chatroom_admin_nav_drawer_menu_create)
            } else {
                nav_view_admin.inflateMenu(R.menu.chatroom_admin_nav_drawer_menu_edit)
            }
        })
    }

    private fun observeChatroomOwnRights() {
        viewModel.isAuthUserChatroomOwner.observe(this, Observer {
            initTopNavigation(it)
        })
    }

    private fun initTopNavigation(chatroomOwner: Boolean) {
        with(binding) {
            if(chatroomOwner) {
                Log.i("ChatroomActivity", "Current auth user is chatroom owner")
                // FIXME: Randomly unresolvable bad Kotlin synthetics
                nav_view_admin.setupWithNavController(navController)
                nav_view_admin.menu.findItem(R.id.action_delete_chatroom).setOnMenuItemClickListener {
                    Constants.buildDeleteAlertDialog(
                        this@ChatroomActivity,
                        Constants.confirmChatroomDeletionMessage,
                        { dialogInterface: DialogInterface, _: Int ->
                            lifecycleScope.launch {
                                viewModel.sendChatroomIntent(ChatroomIntent.DeleteChatroom)
                            }
                            dialogInterface.dismiss()
                        },
                        { dialogInterface: DialogInterface, _: Int ->
                            dialogInterface.dismiss()
                        }
                    )
                    return@setOnMenuItemClickListener true
                }
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
    }

    private fun assertGooglePlayAvailability() {
        val googleApiInstance = GoogleApiAvailability.getInstance()
        val availability = googleApiInstance.isGooglePlayServicesAvailable(this)
        if(availability == SERVICE_MISSING || availability == SERVICE_INVALID
            || availability == SERVICE_DISABLED) {
            googleApiInstance.makeGooglePlayServicesAvailable(this)
        }
    }

    override fun onBackPressed() {
        // TODO: reset auth with no code dup from mainactivity
        // TODO: Fix this retarded backstack management againnnnnnnn
        if(supportFragmentManager.currentNavigationFragment is EventCreateFragment) {
            super.onBackPressed()
            return
        }

        finish()
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
}