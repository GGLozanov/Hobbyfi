package com.example.hobbyfi.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentResetPasswordBinding
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.viewmodels.auth.ResetPasswordFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    @ExperimentalCoroutinesApi
    override fun onStart() {
        super.onStart()
        observePredicateValidators()
    }

    @ExperimentalCoroutinesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeResetPasswordState()
    }

    @ExperimentalCoroutinesApi
    private fun observeResetPasswordState() {
        lifecycleScope.launch {
            viewModel.mainState.collectLatestWithLoadingAndNonIdleReset(listOf(TokenState.Idle::class),
                    viewModel::resetTokenState,
                viewLifecycleOwner, navController,
                    ResetPasswordFragmentDirections.actionResetPasswordFragmentToLoadingNavGraph(
                        R.id.resetPasswordFragment), TokenState.Loading::class) {
                when(it) {
                    is TokenState.Idle -> {

                    }
                    is TokenState.ResetPasswordSuccess -> {
                        context?.showSuccessToast(getString(R.string.sent_password_email))
                        navController.popBackStack()
                    }
                    is TokenState.Error -> {
                        // FIXME: uuuuugh, BAAAAAAAAAAAAAAAAAD... This should be generified with exceptions,
                        // FIXME: not string errorsssssss nor should error transformation happen hereeeee
                        when(it.error) {
                            getString(R.string.resource_not_found_error) -> {
                                context?.showFailureToast(getString(R.string.user_email_not_found))
                            }
                            getString(R.string.invalid_data) -> {
                                context?.showFailureToast(getString(R.string.email_send_fail))
                            // TODO: Reformat to actual message in fragment
                            }
                            getString(R.string.resource_exists_error) -> {
                                context?.showFailureToast(getString(R.string.facebook_user_reset_password_email))
                            // TODO: Reformat to actual message in fragment
                            }
                            else -> context?.showFailureToast(getString(R.string.email_send_fail))
                            // TODO: Reformat to actual message in fragment
                        }
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    @ExperimentalCoroutinesApi
    override fun observePredicateValidators() {
        viewModel.email.invalidity.observe(
            viewLifecycleOwner, TextInputLayoutFocusValidatorObserver(binding.emailInputField, getString(R.string.email_input_error))
        )
    }

    @ExperimentalCoroutinesApi
    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.resetPasswordButton))
    }
}