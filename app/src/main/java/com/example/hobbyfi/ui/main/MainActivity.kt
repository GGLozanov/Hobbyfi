package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.OnAuthStateReset
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class MainActivity : BaseActivity(), OnAuthStateReset {
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        setSupportActionBar(binding.toolbar)
    }

    override fun onStart() {
        super.onStart()
        binding.bottomNav.setupWithNavController(navController)
        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(
            R.id.userProfileFragment,
            R.id.chatroomListFragment
        )))

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
        prefConfig.writeLoginStatus(false)
        prefConfig.resetToken()
        prefConfig.resetRefreshToken()
        navController.popBackStack()
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
}