package com.example.hobbyfi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentResetPasswordBinding
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.TextInputLayoutFocusValidatorObserver
import com.example.hobbyfi.shared.ViewReverseEnablerObserver
import com.example.hobbyfi.shared.collectLatestWithLoading
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.viewmodels.auth.ResetPasswordFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ResetPasswordFragment : AuthFragment() {

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
        super.onCreateView(inflater, container, savedInstanceState)
        binding = DataBindingUtil.inflate(layoutInflater, R.layout.fragment_reset_password, container, false)
        binding.viewModel = viewModel
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@ResetPasswordFragment
            resetPasswordButton.setOnClickListener {
                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.ResetPassword)
                }
            }
            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeResetPasswordState()
    }

    @ExperimentalCoroutinesApi
    private fun observeResetPasswordState() {
        lifecycleScope.launch {
            viewModel.mainState.collectLatestWithLoading(navController,
                    ResetPasswordFragmentDirections.actionResetPasswordFragmentToLoadingNavGraph(
                        R.id.resetPasswordFragment), TokenState.Loading::class) {
                when(it) {
                    is TokenState.Idle -> {

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

    @ExperimentalCoroutinesApi
    override fun observePredicateValidators() {
        viewModel.email.invalidity.observe(
            viewLifecycleOwner, TextInputLayoutFocusValidatorObserver(binding.emailInputField, Constants.emailInputError)
        )
    }

    @ExperimentalCoroutinesApi
    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.resetPasswordButton))
    }
}