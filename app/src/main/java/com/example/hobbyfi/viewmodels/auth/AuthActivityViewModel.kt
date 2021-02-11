package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class AuthActivityViewModel(application: Application) : BaseViewModel(application) {
    private var _restartedFromDeepLink: Boolean = false
    val restartedFromDeepLink: Boolean get() = _restartedFromDeepLink

    fun setRestartedFromDeepLink(res: Boolean) {
        _restartedFromDeepLink = res
    }

}