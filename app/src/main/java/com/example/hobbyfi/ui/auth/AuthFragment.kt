package com.example.hobbyfi.ui.auth

import android.os.Bundle
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavDirections
import com.example.hobbyfi.R
import com.example.hobbyfi.shared.safeNavigate
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.OnAuthStateChanged
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.ui.chatroom.ChatroomActivity
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.auth.AuthActivityViewModel
import com.example.hobbyfi.work.DeviceTokenUploadWorker
import com.facebook.AccessToken
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class AuthFragment : BaseFragment(), OnAuthStateChanged {

    private val activityViewModel: AuthActivityViewModel by activityViewModels()

    @ExperimentalCoroutinesApi
    override fun login(action: NavDirections, token: String?, refreshToken: String?) {
        if (token != null) {
            prefConfig.writeToken(token)
        }

        if (!prefConfig.readCurrentDeviceTokenUploaded()) {
            WorkerUtils.buildAndEnqueueDeviceTokenWorker<DeviceTokenUploadWorker>(
                token
                    ?: AccessToken.getCurrentAccessToken().token, // not pref because pref write is async
                prefConfig.readDeviceToken(), requireContext()
            )
        }

        if (refreshToken != null) {
            prefConfig.writeRefreshToken(refreshToken)
        }

        if (activityViewModel.restartedFromDeepLink) {
            startActivity(
                android.content.Intent(requireContext(), ChatroomActivity::class.java).apply {
                    putExtras(requireActivity().intent)
                })

            finishAffinity(requireActivity())
            activityViewModel.setRestartedFromDeepLink(false)
        } else {
            if (navController.currentDestination?.id == R.id.loadingFragment) {
                navController.popBackStack()
            }

            navController.safeNavigate(action)
        }
    }
}