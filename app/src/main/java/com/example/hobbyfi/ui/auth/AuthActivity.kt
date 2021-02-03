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
import com.example.hobbyfi.shared.toReadable
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.base.NavigationActivity
import com.facebook.applinks.AppLinkData
import io.branch.referral.Branch
import java.util.*

class AuthActivity : NavigationActivity() {

    private lateinit var binding: ActivityAuthBinding

    private val branchReferralInitListener =
        Branch.BranchReferralInitListener { linkProperties, error ->
            // do stuff with deep link data (nav to page, display content, etc)
            // TODO: Decode JSON object from Facebook that MUST contain query params
            // event id and chatroom id

            // if not contains => just yeet user out of app
            // also somehow get Facebook user id  => else yeet user to auth activity
            if (error != null) {
                Log.e("AuthActivity", "Deep-linking error: $error")
                // yeet
            } else {
                Log.i("AuthActivity", "Current link props: ${linkProperties}")

                if(linkProperties?.get("+clicked_branch_link") as Boolean) {
                    if(linkProperties.get("+is_first_session") as Boolean) {
                        Log.i("AuthActivity", "First session triggered")
                        // TODO: Show future onboarding
                    }


                    // get fb user who pressed the button their access token
                    // TODO: Use `is_first_session` to check whether it's first session and open onboarding
                    // not yeet and send to EventDetailsFragment if everything's ok

                    // TODO: Login with FB and wait for authorise
                    // TODO: If unsuccessful authorise => do nothing, I guess
                    // TODO: If successful authorise (or already authorised) => check if user exists with exists endpoint
                    // TODO: If user exists => send to ChatroomActivity with taskRoot (destroy this Activity)
                    // TODO: If user not exists => send to LoginFragment with AuthActivity VM flag whether they should register set to true
                    // TODO: Allow user to select tags (set the state manually to trigger that + get email) => then register
                    // TODO: After register, check in LoginFragment flag and if deep link = true => send to ChatroomActivity (destroy this Activity) and trigger taskRoot
                }
            }
        }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // if activity is in foreground (or in backstack but partially visible) launching the same
        // activity will skip onStart, handle this case with reInitSession
        Branch.sessionBuilder(this).withCallback(branchReferralInitListener).reInit()
    }

    override fun onStart() {
        super.onStart()
        Log.i("AuthActivity", "intent data: ${intent.data}")
        Log.i("AuthActivity", "intent extras: ${intent.extras}")
        Log.i("AuthActivity", "intent app link data: ${(intent.extras?.get("al_applink_data") as Bundle?)?.toReadable()}")
        Log.i("AuthActivity", "intent app link data: ${
            ((intent.extras?.get("al_applink_data") as Bundle?)?.get("extras") as Bundle?)?.toReadable()
        }")
        Log.i("AuthActivity", "intent app link data: ${
            ((intent.extras?.get("al_applink_data") as Bundle?)?.get("referer_app_link") as Bundle?)?.toReadable()
        }")

        AppLinkData.fetchDeferredAppLinkData(this) { data: AppLinkData? ->
            Log.i("AuthActiivty", "app link data: ${data?.refererData?.toReadable()}")

        }

        Branch.sessionBuilder(this).withCallback(branchReferralInitListener)
            .withData(if (intent != null) intent.data else null).init()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        with(binding) {
            val view = root
            setContentView(view)
            initNavController()

            binding.toolbar.setupWithNavController(navController, AppBarConfiguration(setOf(R.id.loginFragment, R.id.registerFragment)))

            setSupportActionBar(toolbar)
        }
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