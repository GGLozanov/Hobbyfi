package com.example.hobbyfi.ui.auth

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.MenuItem
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.onNavDestinationSelected
import androidx.navigation.ui.setupWithNavController
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.ActivityAuthBinding
import com.example.hobbyfi.databinding.ActivityMainBinding
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.NavigationActivity

class AuthActivity : NavigationActivity() {

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        with(binding) {
            val view = root
            setContentView(view)

            setSupportActionBar(toolbar)
        }
    }

    override fun onStart() {
        super.onStart()
        binding.toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(R.id.loginFragment, R.id.registerFragment)))
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