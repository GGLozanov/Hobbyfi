package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.R
import com.example.hobbyfi.viewmodels.main.UserProfileFragmentViewModel

class UserProfileFragment : MainFragment() {

    companion object {
        fun newInstance() = UserProfileFragment()
    }

    private val viewModel: UserProfileFragmentViewModel by viewModels()
    private lateinit var args: UserProfileFragmentArgs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        args = UserProfileFragmentArgs.fromBundle(requireActivity().intent?.extras!!)

        // TODO: Handle expired token error & logout

        return inflater.inflate(R.layout.user_profile_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
    }

}