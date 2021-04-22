package com.example.hobbyfi.ui.shared

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentLoadingBinding
import com.example.hobbyfi.ui.base.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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
        val callback = requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            Log.i("LoadingFragment", "onBackPressedDispatcher callback initiated")
            navController.currentBackStackEntry?.savedStateHandle?.set(BACK_KEY, true)
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData(LOADING_KEY, false)
            ?.observe(viewLifecycleOwner, {
                if(it) {
                    navController.popBackStack(loadingArgs.poptoId, false)
                }
            })
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            withContext(Dispatchers.IO) {
                delay(40000)
            }

            navController.popBackStack(loadingArgs.poptoId, false)
            navController.currentBackStackEntry?.savedStateHandle?.set(BACK_KEY, true)
        } // timeout
    }

    companion object {
        const val LOADING_KEY = "job_loading"
        const val BACK_KEY = "back_press"
    }
}