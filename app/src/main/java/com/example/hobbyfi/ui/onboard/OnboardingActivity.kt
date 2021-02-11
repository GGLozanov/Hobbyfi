package com.example.hobbyfi.ui.onboard

import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import com.example.hobbyfi.R

class OnboardingActivity : AppCompatActivity() {

    // TODO: Implement ViewPager2 + explanation fragments
    // TODO: Reroute from SplashScreen OR from ChatroomActivity by deep link
    // TODO: At the end, route to AuthActivity and branch should set the is_first_session to false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
    }
}