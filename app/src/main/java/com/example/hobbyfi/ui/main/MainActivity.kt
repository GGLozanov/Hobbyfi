package com.example.hobbyfi.ui.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.state.ResponseState
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
    // TODO: Viewpager 2 & bottomnav setup
    // TODO: Fetch viewmodel by delegation *everywhere*

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            viewModel.state.collect {
                when(it) {
                    is UserState.Idle -> {

                    }
                    is UserState.Loading -> {
                        // TODO: Progressbar
                    }
                    is UserState.OnData.UserResult -> {

                    }
                    is UserState.OnData.UserDeleteResult -> {
                        logout()
                    }
                    is UserState.OnData.UserUpdateResult -> {
                        viewModel.updateAndSaveUser(it.userFields)
                    }
                    is UserState.Error -> {
                        Toast.makeText(this@MainActivity, "Couldn't get user!", Toast.LENGTH_LONG)
                            .show()
                        // TODO: Logout
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    override fun logout() {
        prefConfig.writeLoginStatus(false)
        prefConfig.writeToken(null)
        prefConfig.writeRefreshToken(null)
        navController.popBackStack(R.id.registerFragment, false)
    }
}