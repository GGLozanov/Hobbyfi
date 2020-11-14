package com.example.hobbyfi.ui.auth

import android.content.Intent
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val activity = (activity as AppCompatActivity)

        activity.supportActionBar?.hide()

        view?.postDelayed({
            if(prefConfig.readLoginStatus()) {
                navController.navigate(R.id.action_splashScreenFragment_to_loginFragment)
            } else {
                val action = SplashScreenFragmentDirections.actionSplashScreenFragmentToMainActivity(
                    null
                )
                navController.navigate(action)
            }

            activity.supportActionBar?.show()
        }, 3000)
    }
}