package com.example.hobbyfi.ui.auth

import android.util.Log
import com.example.hobbyfi.models.User
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.OnAuthStateChanged

abstract class AuthFragment : BaseFragment(), OnAuthStateChanged {
    override fun login(user: User?, token: String?, refreshToken: String?) {
        if(token != null) {
            prefConfig.writeToken(token)
        }

        if(refreshToken != null) {
            prefConfig.writeRefreshToken(refreshToken)
        }

        prefConfig.writeLoginStatus(true)
        val action = LoginFragmentDirections.actionLoginFragmentToMainActivity(
            user
        )
        navController.navigate(action)
    }
}