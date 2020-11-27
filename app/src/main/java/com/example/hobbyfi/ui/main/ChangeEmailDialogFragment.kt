package com.example.hobbyfi.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChangeEmailDialogBinding
import com.example.hobbyfi.viewmodels.main.ChangeEmailDialogFragmentViewModel
import com.example.hobbyfi.viewmodels.main.ChangePasswordDialogFragmentViewModel
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class ChangeEmailDialogFragment : AuthChangeDialogFragment() {

    private val viewModel: ChangeEmailDialogFragmentViewModel by viewModels()
    @ExperimentalCoroutinesApi
    private val activityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var binding: FragmentChangeEmailDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_change_email_dialog,
            container, false
        )

        with(binding) {
            lifecycleOwner = this@ChangeEmailDialogFragment


            return@onCreateView root
        }
    }

}