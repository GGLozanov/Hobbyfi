package com.example.hobbyfi.shared

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.*
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class RefreshConnectivityMonitor(val context: Context) : LiveData<Boolean>(), KodeinAware {

    override val kodein: Kodein by kodein(context)
    private val connectivityManager: ConnectivityManager by instance(tag = "connectivityManager")

    private lateinit var connectivityManagerCallback: ConnectivityManager.NetworkCallback

    private var hadLostConnectionPrior: Boolean = false

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private val networkRequestBuilder: NetworkRequest.Builder = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)

    override fun onActive() {
        super.onActive()
        updateConnection()
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ->
                connectivityManager.registerDefaultNetworkCallback(getConnectivityMarshmallowManagerCallback())
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                marshmallowNetworkAvailableRequest()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
                lollipopNetworkAvailableRequest()
            else -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    context.registerReceiver(networkReceiver, IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")) // android.net.ConnectivityManager.CONNECTIVITY_ACTION
                }
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManager.unregisterNetworkCallback(connectivityManagerCallback)
        } else {
            context.unregisterReceiver(networkReceiver)
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun lollipopNetworkAvailableRequest() {
        connectivityManager.registerNetworkCallback(
            networkRequestBuilder.build(), getConnectivityLollipopManagerCallback())
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun marshmallowNetworkAvailableRequest() {
        connectivityManager.registerNetworkCallback(
            networkRequestBuilder.build(), getConnectivityMarshmallowManagerCallback())
    }

    private fun getConnectivityLollipopManagerCallback(): ConnectivityManager.NetworkCallback {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityManagerCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if(hadLostConnectionPrior) {
                        postValue(true)
                        hadLostConnectionPrior = false
                    }
                }

                override fun onLost(network: Network) {
                    hadLostConnectionPrior = true
                    postValue(false)
                }
            }
            return connectivityManagerCallback
        } else {
            throw IllegalAccessError("Accessing wrong API version!")
        }
    }

    private fun getConnectivityMarshmallowManagerCallback(): ConnectivityManager.NetworkCallback {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManagerCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    networkCapabilities.let { capabilities ->
                        if(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) && hadLostConnectionPrior) {
                            postValue(true)
                            hadLostConnectionPrior = false
                        }
                    }
                }
                override fun onLost(network: Network) {
                    hadLostConnectionPrior = true
                    postValue(false)
                }
            }
            return connectivityManagerCallback
        } else {
            throw IllegalAccessError("Accessing wrong API version!")
        }
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            hadLostConnectionPrior =
                intent.extras?.get(ConnectivityManager.EXTRA_NO_CONNECTIVITY) == true

            updateConnection()
        }
    }

    private fun updateConnection() {
        postValue(hadLostConnectionPrior && connectivityManager.isConnected())
    }
}