package com.example.hobbyfi.ui.main

import androidx.fragment.app.activityViewModels
import androidx.navigation.navArgs
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.viewmodels.factories.AuthUserViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class AuthChangeDialogFragment : BaseDialogFragment(), TextFieldInputValidationOnus {
    protected val activityViewModel: MainActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: MainActivityArgs by (activity as MainActivity).navArgs()
        AuthUserViewModelFactory(requireActivity().application, activityArgs.user)
    })
}