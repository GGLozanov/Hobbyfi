package com.example.hobbyfi.ui.auth

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.hobbyfi.ui.base.BaseFragment

abstract class AuthFragment : BaseFragment() {
    private lateinit var navController: NavController

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        navController = findNavController()
    }
}