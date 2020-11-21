package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentUserProfileBinding
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.viewmodels.main.UserProfileFragmentViewModel

class UserProfileFragment : MainFragment(), TextFieldInputValidationOnus {

    private val viewModel: UserProfileFragmentViewModel by viewModels(factoryProducer = {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    })
    private lateinit var args: UserProfileFragmentArgs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentUserProfileBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_profile, container, false)

        args = UserProfileFragmentArgs.fromBundle(requireActivity().intent?.extras!!)

        // TODO: Handle expired token error & logout

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
    }

    override fun initTextFieldValidators() {
        TODO("Not yet implemented")
    }
}