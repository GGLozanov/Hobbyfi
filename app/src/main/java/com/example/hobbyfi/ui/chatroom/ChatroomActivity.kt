package com.example.hobbyfi.ui.chatroom

import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
import com.example.hobbyfi.viewmodels.factories.AuthUserChatroomViewModelFactory
import com.google.android.gms.common.ConnectionResult.*
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.android.synthetic.main.activity_chatroom.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChatroomActivity : BaseActivity() {
    // TODO: Have this be the deeplink activity. Register it and handle intent extras and facebook/default user
    // TODO: If user leaves chatroom (not exits), delete entire chatroom + cached other users (rip foreign key relations)

    private val viewModel: ChatroomActivityViewModel by viewModels(factoryProducer = {
        AuthUserChatroomViewModelFactory(application, args.user, args.chatroom)
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

        // TODO: Register delete/update BroadcastReceiver here that sends DeleteChatroomCache/UpdateChatroomCache intents and responds accordingly
        // https://stackoverflow.com/questions/41793525/how-to-get-fcm-onmessagereceived-event-in-activity-without-pendingintent
        // TODO: Do the same with User intents and Event intents
        // TODO: First fetch messages from back-end then register for receiving messages

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
                // no need for UserState.OnData.UserUpdateResult for null chatroom because user gets nulled chatroom in backend when it's deleted
            }
        }

        // whenever broadcast receiver triggered => sets the state in the viewmodel
        lifecycleScope.launch {
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
                    is ChatroomState.OnData.ChatroomDeleteResult -> {
                        Toast.makeText(this@ChatroomActivity, "Successfully deleted chatroom!", Toast.LENGTH_LONG)
                            .show()
                        onBackPressed()
                        // TODO: Should only show toast or something here in the future and have exiting chatroom and
                        //  nullifying user chatroom ID be handled by DeleteChatroomNotification STATE
                        // TODO: Delete chatroom from cache and clear user chatroom id
                    }
                    is ChatroomState.OnData.ChatroomUpdateResult -> {
                        Toast.makeText(this@ChatroomActivity, "Successfully updated chatroom!", Toast.LENGTH_LONG)
                            .show()
                        // TODO: Should only show toast or something here in the future and have exiting chatroom
                        //  updates be handled by UpdateChatroomNotification
                        // TODO: Update chatroom cache
                    }
                    is ChatroomState.OnData.DeleteChatroomCacheResult -> {
                        // can be called by both owner
                        onBackPressed()
                    }
                    is ChatroomState.Error -> {
                        Toast.makeText(this@ChatroomActivity, "Whoops! Looks like something went wrong! ${it.error}", Toast.LENGTH_LONG)
                            .show()
                        if(it.shouldExit) {
                            finish()
                        }
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
            if(supportFragmentManager.currentNavigationFragment is ChatroomMessageListFragment) {
                title = it?.name
            } else {
                // very, very hacky stuff but... that's life
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            if(viewModel.isAuthUserChatroomOwner.value == true) {
                nav_view_admin.menu.clear()
                if(it?.lastEventId == null) {
                    nav_view_admin.inflateMenu(R.menu.chatroom_admin_nav_drawer_menu_create)
                } else {
                    nav_view_admin.inflateMenu(R.menu.chatroom_admin_nav_drawer_menu_edit)
                }
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
            toolbar.setNavigationIconTint(Color.WHITE)
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