package com.example.hobbyfi.ui.main

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.OnAuthStateReset
import com.example.hobbyfi.viewmodels.factories.MainActivityViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.facebook.login.LoginManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class MainActivity : BaseActivity(), OnAuthStateReset {
    private val viewModel: MainActivityViewModel by viewModels(factoryProducer = {
        MainActivityViewModelFactory(application, args.user)
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
        }
    }

    override fun onStart() {
        super.onStart()
        with(binding) {
            bottomNav.setupWithNavController(navController)
            toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(
                R.id.userProfileFragment,
                R.id.chatroomListFragment
            )))
        }

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
                        Toast.makeText(this@MainActivity, "Successfully updated fields!", Toast.LENGTH_LONG)
                            .show()
                        viewModel.updateAndSaveUser(it.userFields)
                    }
                    is UserState.Error -> {
                        Toast.makeText(this@MainActivity, "Something went wrong! ${it.error}", Toast.LENGTH_LONG)
                            .show()
                        logout()
                    }
                }
            }
        }
    }

    fun resetAuth() {
        LoginManager.getInstance().logOut()
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_user_fetch_time)
        prefConfig.resetToken()
        prefConfig.resetRefreshToken()
    }

    override fun logout() {
        resetAuth()
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
            super.onBackPressed()
        } else {
            resetAuth()
            finishAffinity()
        }
    }
}