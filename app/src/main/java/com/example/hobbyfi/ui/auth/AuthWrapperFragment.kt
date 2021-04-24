package com.example.hobbyfi.ui.auth

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentAuthWrapperBinding
import com.example.hobbyfi.shared.safeNavigate
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.viewmodels.auth.AuthActivityViewModel

class AuthWrapperFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentAuthWrapperBinding =
            FragmentAuthWrapperBinding.inflate(inflater, container, false)

        with(binding) {
            signUpButton.setOnClickListener {
                navController.safeNavigate(R.id.action_authWrapperFragment_to_registerFragment)
            }

            loginButton.setOnClickListener {
                navController.safeNavigate(R.id.action_authWrapperFragment_to_loginFragment)
            }

            appNameHeader.text = SpannableString(getString(R.string.app_name)).apply {
                setSpan(
                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.colorAccent)),
                    6,
                    7,
                    Spannable.SPAN_EXCLUSIVE_INCLUSIVE
                )
            }

            return@onCreateView root
        }
    }
}