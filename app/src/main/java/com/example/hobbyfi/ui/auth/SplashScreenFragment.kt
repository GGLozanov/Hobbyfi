package com.example.hobbyfi.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.hobbyfi.R
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.utils.TokenUtils
import com.facebook.AccessToken
import com.facebook.Profile

class SplashScreenFragment : BaseFragment() { // surely won't access sharedprefs (probably...) so it can stay Fragment() and not BaseFragment()

    companion object {
        fun newInstance() = SplashScreenFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.splash_screen_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as AppCompatActivity?)?.supportActionBar?.hide()

        view?.postDelayed({
            try {
                val isFacebookUserLogged = AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired
                        && Profile.getCurrentProfile().id != null
                if(isFacebookUserLogged || TokenUtils.getTokenUserIdFromStoredTokens(prefConfig)
                        .compareTo(0) != 0) { // assert jwt doesn't throw exception
                    Log.i("SplashScreen", "Authenticated. Moving to main activity")

                    val action = SplashScreenFragmentDirections.actionSplashScreenFragmentToMainActivity(
                        null, // fetches user in MainActivity; if fails due to token expiry => logout user
                        isFacebookUserLogged
                    )
                    navController.navigate(action)
                } else throw UnauthenticatedException()
            } catch(ex: Exception) {
                ex.printStackTrace()
                Log.i("SplashScreen", "Not authenticated. Moving to login fragment")
                navController.navigate(R.id.action_splashScreenFragment_to_loginFragment)
            }

            (activity as AppCompatActivity?)?.supportActionBar?.show()
        }, 3000)
    }

    class UnauthenticatedException : Exception()
}