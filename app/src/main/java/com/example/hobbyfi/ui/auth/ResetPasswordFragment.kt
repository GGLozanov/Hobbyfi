package com.example.hobbyfi.ui.auth

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.databinding.FragmentResetPasswordBinding
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.shared.removeAllEditTextWatchers
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.auth.ResetPasswordFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ResetPasswordFragment : AuthFragment(), TextFieldInputValidationOnus {

    companion object {
        fun newInstance() = ResetPasswordFragment()
    }

    @ExperimentalCoroutinesApi
    private val viewModel: ResetPasswordFragmentViewModel by viewModels()
    private lateinit var binding: FragmentResetPasswordBinding

    @ExperimentalCoroutinesApi
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_reset_password, container, false)
        binding.viewModel = viewModel

        with(binding) {
            lifecycleOwner = this@ResetPasswordFragment
            resetPasswordButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.ResetPassword)
                }
            }

            observeResetPasswordState()

            return@onCreateView root
        }
    }


    @ExperimentalCoroutinesApi
    private fun observeResetPasswordState() {
        lifecycleScope.launch {
            viewModel.mainState.collectLatest {
                when(it) {
                    is TokenState.Idle -> {

                    }
                    is TokenState.Loading -> {
                        // TODO: Progressbar
                    }
                    is TokenState.ResetPasswordSuccess -> {
                        Toast.makeText(requireContext(), "Successfully sent password reset e-mail!", Toast.LENGTH_LONG)
                            .show()// TODO: Reformat to actual message in fragment
                        viewModel.resetTokenState()
                        navController.popBackStack()
                    }
                    is TokenState.Error -> {
                        // FIXME: uuuuugh, BAAAAAAAAAAAAAAAAAD... This should be generified with exceptions,
                        // FIXME: not string errorsssssss nor should error transformation happen hereeeee
                        when(it.error) {
                            Constants.resourceNotFoundError -> {
                                Toast.makeText(requireContext(), Constants.emailNotFound, Toast.LENGTH_LONG)
                                    .show()
                            }
                            Constants.invalidDataError -> {
                                Toast.makeText(requireContext(), Constants.emailSendFail, Toast.LENGTH_LONG)
                                    .show()// TODO: Reformat to actual message in fragment
                            }
                            Constants.resourceExistsError -> {
                                Toast.makeText(requireContext(), Constants.facebookUserSendAttempt, Toast.LENGTH_LONG)
                                    .show()// TODO: Reformat to actual message in fragment
                            }
                            else -> Toast.makeText(requireContext(), Constants.emailSendFail, Toast.LENGTH_LONG)
                                .show()// TODO: Reformat to actual message in fragment
                        }
                        viewModel.resetTokenState()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    override fun initTextFieldValidators() {
        binding.emailInputField.addTextChangedListener(
            Constants.emailInputError,
            Constants.emailPredicate
        )
    }

    override fun assertTextFieldsInvalidity(): Boolean =
        FieldUtils.isTextFieldInvalid(binding.emailInputField, Constants.emailInputError)

    override fun onStart() {
        super.onStart()
        initTextFieldValidators()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            emailInputField.removeAllEditTextWatchers()
        }
    }
}