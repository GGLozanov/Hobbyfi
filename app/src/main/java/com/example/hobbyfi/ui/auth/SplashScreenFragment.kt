package com.example.hobbyfi.ui.auth

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hobbyfi.R

class SplashScreenFragment : Fragment() { // surely won't access sharedprefs (probably...) so it can stay Fragment() and not BaseFragment()

    companion object {
        fun newInstance() = SplashScreenFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.splash_screen_fragment, container, false)
    }

}