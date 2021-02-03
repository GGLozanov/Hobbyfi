package com.example.hobbyfi.ui.auth

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityAuthBinding
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.toReadable
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.NavigationActivity
import com.facebook.applinks.AppLinkData
import io.branch.referral.Branch
import java.util.*

class AuthActivity : NavigationActivity() {
    private var restartedFromDeeplink: Boolean = false
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Check link properties (lastReferringParams) and set persistent boolean in VM/savedInstanceState for deep link here
        // TODO: invalidate boolean after user has either: registered, logged in (Facebook/normal), registered w/ Facebook
        // TODO: still navigate to MainActivity on login/register/whatever but make the user join the chatroom they're interested in
        // TODO: If they're not in it yet. Then, invalidate all deeplink-related stuff
        // TODO: and trigger deeplink listener again (with auth this time) and navigate to EventDetailsFragment
        Log.i("AuthActivity", "intent extras: ${intent.extras}")
        restartedFromDeeplink = savedInstanceState?.getBoolean(Constants.deepLinkCall)
            ?: Branch.getInstance().latestReferringParams["+clicked_branch_link"] as Boolean
        // FIXME: Possible session conflicts here (getting clicked branch link as true after restarts)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        with(binding) {
            val view = root
            setContentView(view)
            initNavController()

            binding.toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(R.id.loginFragment, R.id.registerFragment)))

            setSupportActionBar(toolbar)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState.apply {
            putBoolean(Constants.deepLinkCall, restartedFromDeeplink)
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when(item.itemId) {
            R.id.action_register -> {
                if(navController.currentDestination?.id == R.id.loginFragment) {
                    navController.navigate(R.id.action_loginFragment_to_registerFragment)
                }
            }
            R.id.action_login -> {
                if(navController.currentDestination?.id == R.id.registerFragment) {
                    navController.navigate(R.id.action_registerFragment_to_loginFragment)
                }
            }
        }

        return true
    }
}