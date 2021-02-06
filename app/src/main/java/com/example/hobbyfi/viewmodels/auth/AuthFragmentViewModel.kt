package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import com.example.hobbyfi.models.TagBundle
import com.example.hobbyfi.viewmodels.base.AuthInclusiveViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class AuthFragmentViewModel(application: Application) : AuthInclusiveViewModel(application) {
    var tagBundle: TagBundle = TagBundle()
}