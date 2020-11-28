package com.example.hobbyfi.ui.main

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

abstract class AuthChangeDialogFragment : BaseDialogFragment(), TextFieldInputValidationOnus {
    @ExperimentalCoroutinesApi
    protected val activityViewModel: MainActivityViewModel by activityViewModels()
}