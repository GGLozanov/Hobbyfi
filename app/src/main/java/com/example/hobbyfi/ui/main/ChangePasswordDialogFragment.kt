package com.example.hobbyfi.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChangeEmailDialogBinding
import com.example.hobbyfi.databinding.FragmentChangePasswordDialogBinding
import com.example.hobbyfi.viewmodels.main.ChangePasswordDialogFragmentViewModel

class ChangePasswordDialogFragment : AuthChangeDialogFragment() {
    
    private val viewModel: ChangePasswordDialogFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChangePasswordDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_change_password_dialog,
            container, false
        )
        binding.viewModel = viewModel
        binding.lifecycleOwner = this


        return binding.root
    }
}