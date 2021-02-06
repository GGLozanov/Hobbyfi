package com.example.hobbyfi.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityAuthBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.toReadable
import com.example.hobbyfi.ui.base.NavigationActivity
import io.branch.referral.Branch

class AuthActivity : NavigationActivity() {
    private var _restartedFromDeepLink: Boolean = false
    val restartedFromDeepLink: Boolean get() = _restartedFromDeepLink

    fun setRestartedFromDeeplink(res: Boolean) {
        _restartedFromDeepLink = res
    }

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AuthActivity", "intent extras: ${intent.extras?.toReadable()}")
        _restartedFromDeepLink = savedInstanceState?.getBoolean(Constants.deepLinkCall)
            ?: intent.extras?.get("al_applink_data") != null

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
            putBoolean(Constants.deepLinkCall, _restartedFromDeepLink)
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