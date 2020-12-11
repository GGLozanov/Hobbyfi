package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.currentNavigationFragment
import com.example.hobbyfi.shared.setupWithNavController
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.OnAuthStateReset
import com.example.hobbyfi.viewmodels.factories.AuthUserViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.facebook.login.LoginManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch



@ExperimentalCoroutinesApi
class MainActivity : BaseActivity(), OnAuthStateReset {
    private val viewModel: MainActivityViewModel by viewModels(factoryProducer = {
        AuthUserViewModelFactory(application, args.user)
    })
    private lateinit var binding: ActivityMainBinding
    private val args: MainActivityArgs by navArgs()

    private var poppedFromNavController: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        with(binding) {
            val view = root
            setContentView(view)
            setSupportActionBar(toolbar)

            bottomNav.setOnNavigationItemReselectedListener {
                // avoid fragment recreation (do nothing here)
            }

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

    @ExperimentalPagingApi
    override fun onStart() {
        super.onStart()

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
                                navController.currentBackStackEntry?.savedStateHandle?.set(Constants.chatroomJoined, true)
                            } else {
                                navController.currentBackStackEntry?.savedStateHandle?.set(Constants.chatroomLeft, true)
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Successfully updated fields!", Toast.LENGTH_LONG)
                                .show()
                        }
                        viewModel.updateAndSaveUser(it.userFields)
                    }
                    is UserState.Error -> {
                        Toast.makeText(this@MainActivity, "Something went wrong! ${it.error}", Toast.LENGTH_LONG)
                            .show()
                        if(it.shouldReauth) {
                            logout()
                        }
                    }
                }
            }
        }
    }

    // FIXME: Bad way to handle backstack
    fun resetAuth() {
        LoginManager.getInstance().logOut()
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_user_fetch_time)
        prefConfig.resetToken()
        prefConfig.resetRefreshToken()
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
}