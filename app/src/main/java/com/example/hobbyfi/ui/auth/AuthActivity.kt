package com.example.hobbyfi.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityAuthBinding
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.toReadable
import com.example.hobbyfi.ui.base.NavigationActivity
import com.example.hobbyfi.viewmodels.auth.AuthActivityViewModel
import io.branch.referral.Branch

class AuthActivity : NavigationActivity() {
    private val viewModel: AuthActivityViewModel by viewModels()
    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AuthActivity", "intent extras: ${intent.extras?.toReadable()}")
        viewModel.setRestartedFromDeepLink(intent.extras?.get("al_applink_data") != null)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        with(binding) {
            val view = root
            setContentView(view)
            setSupportActionBar(toolbar)
            initNavController()

            toolbar.setupWithNavController(
                navController,
                AppBarConfiguration(setOf(R.id.authWrapperFragment))
            )

            navController.addOnDestinationChangedListener { _, destination, _ ->
                if(destination.id == R.id.authWrapperFragment) supportActionBar?.hide() else supportActionBar?.show()
            }
        }
    }
}