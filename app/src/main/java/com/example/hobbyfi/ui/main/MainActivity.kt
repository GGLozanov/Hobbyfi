package com.example.hobbyfi.ui.main

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class MainActivity : BaseActivity() {
    // TODO: Viewpager 2 & bottomnav setup
    // TODO: Fetch viewmodel by delegation *everywhere*

    private val viewModel: MainActivityViewModel by viewModels(factoryProducer = {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    })
    val args: MainActivityArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.setUser(args.user)

        lifecycleScope.launch {
            viewModel.state.collect {
                when(it) {
                    is UserState.Idle -> {

                    }
                    is UserState.Loading -> {

                    }
                    is UserState.OnData -> {

                    }
                    is UserState.Error -> {

                    }
                }
            }
        }
    }
}