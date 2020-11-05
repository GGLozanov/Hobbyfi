package com.example.hobbyfi.ui.auth

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.hobbyfi.R
import com.example.hobbyfi.viewmodels.auth.ExternalAuthFragmentViewModel

class ExternalAuthFragment : AuthFragment() {

    companion object {
        fun newInstance() = ExternalAuthFragment()
    }

    private lateinit var viewModel: ExternalAuthFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.external_auth_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(ExternalAuthFragmentViewModel::class.java)
        // TODO: Use the ViewModel
    }

}