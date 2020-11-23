package com.example.hobbyfi.ui.base

import com.example.hobbyfi.R
import com.example.hobbyfi.models.User
import com.example.hobbyfi.ui.auth.LoginFragmentDirections

abstract class AuthFragment : BaseFragment() {
    protected fun login(user: User?, token: String? = null, refreshToken: String? = null) {
        prefConfig.writeToken(token)
        prefConfig.writeRefreshToken(refreshToken)
        prefConfig.writeLoginStatus(true)
        val action = LoginFragmentDirections.actionLoginFragmentToMainActivity(
            user
        )
        navController.navigate(action)
    }

    protected fun logout() {
        prefConfig.writeLoginStatus(false)
        prefConfig.writeToken(null)
        prefConfig.writeRefreshToken(null)
        navController.popBackStack(R.id.registerFragment, false)
    }
}