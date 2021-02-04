package com.example.hobbyfi.ui.auth

import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.navigation.NavDirections
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.OnAuthStateChanged
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class AuthFragment : BaseFragment(), OnAuthStateChanged, TextFieldInputValidationOnus {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    @ExperimentalCoroutinesApi
    override fun login(action: NavDirections, token: String?, refreshToken: String?) {
        if(token != null) {
            prefConfig.writeToken(token)
        }

        if(refreshToken != null) {
            prefConfig.writeRefreshToken(refreshToken)
        }

        val activity = (requireActivity() as AuthActivity)
        if(activity.restartedFromDeepLink) {
            startActivity(android.content.Intent(requireContext(), ChatroomActivity::class.java).apply {
                putExtras(activity.intent)
            })

            finishAffinity(activity)
            activity.setRestartedFromDeeplink(false)
        } else navController.navigate(action)
    }
}