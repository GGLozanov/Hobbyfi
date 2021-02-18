package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChangePasswordDialogBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.TextInputLayoutFocusValidatorObserver
import com.example.hobbyfi.shared.ViewReverseEnablerObserver
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.main.ChangePasswordDialogFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
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
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@ChangePasswordDialogFragment

            buttonBar.leftButton.setOnClickListener { dismiss() }
            buttonBar.rightButton.setOnClickListener {
                if(viewModel!!.password.value == viewModel!!.newPassword.value) {
                    Toast.makeText(requireContext(), "Passwords must not be the same! Please enter a new, unique password!", Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }

                viewModel!!.email.value = activityViewModel.authUser.value?.email // set user email to AuthUser Activity VM email

                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.FetchLoginToken)
                }
            }

            lifecycleScope.launchWhenCreated {
                viewModel!!.mainState.collectLatest {
                    when(it) {
                        is TokenState.Idle -> {

                        }
                        is TokenState.Loading -> {
                            // TODO: Progress bar/thing
                        }
                        is TokenState.TokenReceived -> {
                            it.token?.jwt?.let { jwt -> prefConfig.writeToken(jwt) }
                            it.token?.refreshJwt?.let { refreshJwt -> prefConfig.writeToken(refreshJwt) }
                            activityViewModel.sendIntent(UserIntent.UpdateUser(mutableMapOf(
                                Pair(Constants.PASSWORD, viewModel!!.newPassword.value!!)
                            )))
                            dismiss()
                        }
                        is TokenState.Error -> {
                            Toast.makeText(requireContext(), "Invalid access! Please enter the correct ", Toast.LENGTH_LONG)
                                .show()
                        }
                        else -> throw State.InvalidStateException()
                    }
                }
            }

            return@onCreateView root
        }
    }

    override fun observePredicateValidators() {
        with(viewModel) {
            password.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.passwordInputField, Constants.passwordInputError)
            )

            val newPasswordObserver = TextInputLayoutFocusValidatorObserver(binding.newPasswordInputField, Constants.passwordInputError)
            val confirmPasswordObserver = TextInputLayoutFocusValidatorObserver(binding.confirmNewPasswordInputField, Constants.confirmPasswordInputError)

            newPasswordObserver.addDependent(confirmPasswordObserver)

            newPassword.invalidity.observe(
                viewLifecycleOwner,
                newPasswordObserver
            )

            confirmPassword.invalidity.observe(
                viewLifecycleOwner,
                confirmPasswordObserver
            )
        }
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.buttonBar.rightButton))
    }

    override fun onStart() {
        super.onStart()
        observePredicateValidators()
    }

}