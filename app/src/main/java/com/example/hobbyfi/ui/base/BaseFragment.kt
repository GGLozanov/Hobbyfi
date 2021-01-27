package com.example.hobbyfi.ui.base

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.example.hobbyfi.utils.ImageUtils
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import org.kodein.di.generic.instance
import pub.devrel.easypermissions.EasyPermissions

abstract class BaseFragment : Fragment(), KodeinAware, EasyPermissions.PermissionCallbacks {
    override val kodein: Kodein by kodein()

    protected val prefConfig: PrefConfig by instance(tag = "prefConfig")
        // no need for weakreference this time because PrefConfig will use appContext!
    protected lateinit var navController: NavController
    protected val connectivityManager: ConnectivityManager by instance(tag = "connectivityManager")
    protected val localBroadcastManager: LocalBroadcastManager by instance(tag = "localBroadcastManager")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        super.onActivityCreated(savedInstanceState)
        navController = findNavController()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if(requestCode == Constants.imagePermissionsRequestCode) {
            Callbacks.openImageSelection(this, Constants.imageRequestCode)
        }
        // handle location perm
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        // TODO: show toast that says user won't have access until they grant perm. . .
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        (requireActivity() as BaseActivity).refreshConnectivityMonitor.value?.let {
            outState.putBoolean(Constants.LAST_CONNECTIVITY,
                it
            )
        }
    }
}