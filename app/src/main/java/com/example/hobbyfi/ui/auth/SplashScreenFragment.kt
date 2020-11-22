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
import com.facebook.AccessToken

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

        view?.postDelayed({
            if(!prefConfig.readLoginStatus()) {  // TODO: Also check token expiry here? and Facebook
                Log.i("SplashScreen", "Not authenticated. Moving to login fragment")
                navController.navigate(R.id.action_splashScreenFragment_to_loginFragment)
            } else {
                Log.i("SplashScreen", "Authenticated. Moving to main activity")

                val action = SplashScreenFragmentDirections.actionSplashScreenFragmentToMainActivity(
                    null // fetches user in MainActivity; if fails due to token expiry => logout user
                )
                navController.navigate(action)
            }
        }, 3000)
    }
}