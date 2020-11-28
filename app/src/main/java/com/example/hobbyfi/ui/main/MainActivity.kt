package com.example.hobbyfi.ui.main

import android.os.Bundle
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
        MainActivityViewModelFactory(application, args.isFacebookUser, args.user)
    })
    private lateinit var binding: ActivityMainBinding
    private val args: MainActivityArgs by navArgs()

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
                        logout()
                    }
                    is UserState.OnData.UserUpdateResult -> {
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

    override fun logout() {
        LoginManager.getInstance().logOut()
        prefConfig.resetToken()
        prefConfig.resetRefreshToken()
        finish()
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
        super.onBackPressed()
    }
}