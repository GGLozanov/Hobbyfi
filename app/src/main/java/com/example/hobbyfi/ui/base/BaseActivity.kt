package com.example.hobbyfi.ui.base

import android.net.ConnectivityManager
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.RefreshConnectivityMonitor
import com.example.hobbyfi.shared.PrefConfig
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class BaseActivity : AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    protected lateinit var navController: NavController

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")
    lateinit var refreshConnectivityMonitor: RefreshConnectivityMonitor
    protected val connectivityManager: ConnectivityManager by instance(tag = "connectivityManager")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshConnectivityMonitor = RefreshConnectivityMonitor(this)
    }

    override fun onStart() {
        super.onStart()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

}