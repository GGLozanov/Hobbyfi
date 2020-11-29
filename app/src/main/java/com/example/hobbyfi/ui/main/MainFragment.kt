package com.example.hobbyfi.ui.main

import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.navigation.navArgs
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.factories.MainActivityViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class MainFragment : BaseFragment() {
    protected val activityViewModel: MainActivityViewModel by activityViewModels(factoryProducer = {
        val activityArgs: MainActivityArgs by (activity as MainActivity).navArgs()
        MainActivityViewModelFactory(requireActivity().application, activityArgs.isFacebookUser, activityArgs.user)
    })

}