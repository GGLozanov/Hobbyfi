package com.example.hobbyfi.ui.auth

import com.example.hobbyfi.models.User
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.OnAuthStateChanged

abstract class AuthFragment : BaseFragment(), OnAuthStateChanged {
    override fun login(user: User?, token: String?, refreshToken: String?) {
        prefConfig.writeToken(token)
        prefConfig.writeRefreshToken(refreshToken)
        prefConfig.writeLoginStatus(true)
        val action = LoginFragmentDirections.actionLoginFragmentToMainActivity(
            user
        )
        navController.navigate(action)
    }
}