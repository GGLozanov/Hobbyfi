package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.BaseActivity
import com.example.hobbyfi.ui.main.MainActivity
import com.example.hobbyfi.ui.onboard.OnboardingActivity
import com.example.hobbyfi.utils.TokenUtils
import io.branch.referral.Branch
import kotlinx.coroutines.ExperimentalCoroutinesApi

class SplashScreenActivity : BaseActivity() {

    companion object {
        fun newInstance() = SplashScreenActivity()
    }

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // using theming the splash is automatically rendered
        try {
            Log.i("SplashScreenActivity", "${Branch.getInstance(this).latestReferringParams}")
            if((Branch.getInstance(this).latestReferringParams["+is_first_session"] as Boolean?) == true) {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
                return
            }

            val isFacebookUserLogged = Constants.isFacebookUserAuthd()
            if (isFacebookUserLogged || TokenUtils.getTokenUserIdFromStoredTokens(prefConfig)
                    .compareTo(0) != 0) { // assert jwt doesn't throw exception
                Log.i("SplashScreen", "Authenticated. Moving to main activity")

                startActivity(Intent(this, AuthActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                })
                overridePendingTransition(0, 0) // no anim
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            } else throw UnauthenticatedException()
        } catch (ex: Exception) {
            ex.printStackTrace()
            Log.i("SplashScreen", "Not authenticated. Moving to AuthActivity (LoginFragment)")
            startActivity(Intent(this, AuthActivity::class.java).apply {
                flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP
            })
        }
        finish()
    }

    private class UnauthenticatedException : Exception()
}