package com.example.hobbyfi.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.viewmodels.main.ChangePasswordDialogFragmentViewModel

class ChangeEmailDialogFragment : AuthChangeDialogFragment() {

    private val viewModel: ChangePasswordDialogFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout

        return inflater.inflate(R.layout.fragment_change_email_dialog, container, false)
    }

}