package com.example.hobbyfi.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.navigation.NavDirections
import com.example.hobbyfi.models.User
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.OnAuthStateChanged
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus

abstract class AuthFragment : BaseFragment(), OnAuthStateChanged, TextFieldInputValidationOnus {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun login(action: NavDirections, token: String?, refreshToken: String?) {
        if(token != null) {
            prefConfig.writeToken(token)
        }

        if(refreshToken != null) {
            prefConfig.writeRefreshToken(refreshToken)
        }

        navController.navigate(action)
    }
}