package com.example.hobbyfi.ui.main

import androidx.fragment.app.activityViewModels
import androidx.navigation.navArgs
import androidx.paging.ExperimentalPagingApi
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.factories.AuthUserViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
abstract class MainFragment : BaseFragment() {
    protected val activityViewModel: MainActivityViewModel by activityViewModels(factoryProducer = {
        val activity = activity as MainActivity

        return@activityViewModels if(activity.intent.extras != null) {
            val activityArgs: MainActivityArgs by activity.navArgs()
            AuthUserViewModelFactory(requireActivity().application, activityArgs.user)
        } else AuthUserViewModelFactory(requireActivity().application, null)
    })
}