package com.example.hobbyfi.ui.base

import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.shared.RefreshConnectivityMonitor
import org.kodein.di.Kodein
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class NavigationActivity : BaseActivity() {
    protected lateinit var navController: NavController

    // called AFTER setContentView!
    protected fun initNavController() {
        try {
            val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
        } catch(ex: ClassCastException) {
            Log.w("NavigationActivity", "User has triggered ClassCastException on Activity restart from onStart(), possibly because they've nav'd to a fragment whose behaviour is not managed by Android Navigation.")
        }
    }
}