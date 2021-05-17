package com.example.hobbyfi.ui.base

import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RefreshConnectivityMonitor
import com.example.hobbyfi.shared.findLoadingDestinationAwareNavController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class NavigationActivity : BaseActivity() {
    protected lateinit var navController: NavController

    // called AFTER setContentView!
    @ExperimentalCoroutinesApi
    protected open fun initNavController() {
        findLoadingDestinationAwareNavController()?.apply {
            navController = this
        }
    }
}