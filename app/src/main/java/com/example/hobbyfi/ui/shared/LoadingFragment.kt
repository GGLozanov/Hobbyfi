package com.example.hobbyfi.ui.shared

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentLoadingBinding
import com.example.hobbyfi.ui.base.BaseFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi

class LoadingFragment : BaseFragment() {

    private val loadingArgs: LoadingFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentLoadingBinding = FragmentLoadingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData(LOADING_KEY, false)
            ?.observe(viewLifecycleOwner, {
                if(it) {
                    navController.popBackStack(loadingArgs.poptoId, false)
                }
            })
        view.postDelayed({
            navController.popBackStack(loadingArgs.poptoId, false)
        }, 15000) // timeout
    }

    companion object {
        const val LOADING_KEY = "job_loading"
    }
}