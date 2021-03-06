package com.example.hobbyfi.ui.base

import android.net.ConnectivityManager
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.RefreshConnectivityMonitor
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.work.DeviceTokenUploadWorker
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

abstract class BaseActivity : AppCompatActivity(), KodeinAware {
    override val kodein: Kodein by kodein()
    protected val localBroadcastManager: LocalBroadcastManager by instance(tag = "localBroadcastManager")

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")
    lateinit var refreshConnectivityMonitor: RefreshConnectivityMonitor
    protected val connectivityManager: ConnectivityManager by instance(tag = "connectivityManager")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshConnectivityMonitor = RefreshConnectivityMonitor(this)
    }
}