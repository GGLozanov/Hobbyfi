package com.example.hobbyfi.ui.main

import androidx.fragment.app.activityViewModels
import com.example.hobbyfi.ui.auth.AuthFragment
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.OnAuthStateReset
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class MainFragment : BaseFragment() {
    @ExperimentalCoroutinesApi
    protected val activityViewModel: MainActivityViewModel by activityViewModels()
}