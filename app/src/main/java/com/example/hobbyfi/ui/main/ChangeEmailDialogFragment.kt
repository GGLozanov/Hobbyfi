package com.example.hobbyfi.ui.main

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
import com.example.hobbyfi.databinding.FragmentChangeEmailDialogBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.shared.removeAllEditTextWatchers
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.main.AuthChangeDialogFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChangeEmailDialogFragment : AuthChangeDialogFragment() {
    private val viewModel: AuthChangeDialogFragmentViewModel by viewModels()
    private lateinit var binding: FragmentChangeEmailDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // TODO: Handle expired token error & logout
        binding = DataBindingUtil.inflate(
            layoutInflater, R.layout.fragment_change_email_dialog,
            container, false
        )

        binding.viewModel = viewModel

        with(binding) {
            lifecycleOwner = this@ChangeEmailDialogFragment

            buttonBar.leftButton.setOnClickListener { dismiss() }
            buttonBar.rightButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                val newEmail = viewModel!!.email.value
                val originalEmail = activityViewModel.authUser.value?.email

                if(newEmail == originalEmail) {
                    Toast.makeText(requireContext(), "Emails must not be the same! Please enter a new, unique e-mail!", Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }

                // FIXME: Bad workaround to VM FetchLoginToken constraints
                viewModel!!.email.value = originalEmail
                viewModel!!.setNewEmail(newEmail)
                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.FetchLoginToken)
                }
            }

            lifecycleScope.launchWhenCreated {
                viewModel!!.mainState.collect {
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
                                Pair(Constants.EMAIL, viewModel!!.newEmail)))
                            )
                            dismiss()
                        }
                        is TokenState.Error -> {
                            Toast.makeText(requireContext(), it.error, Toast.LENGTH_LONG)
                                .show()
                            dismiss()
                        }
                        else -> throw State.InvalidStateException()
                    }
                }
            }

            return@onCreateView root
        }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            newEmailInputField.addTextChangedListener(
                Constants.emailInputError,
                Constants.newEmailPredicate(activityViewModel.authUser.value?.email)
            )

            passwordInputField.addTextChangedListener(
                Constants.passwordInputError,
                Constants.passwordPredicate(confirmPasswordInputField.editText)
            )

            confirmPasswordInputField.addTextChangedListener(
                Constants.confirmPasswordInputError,
                Constants.confirmPasswordPredicate(passwordInputField.editText!!)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        initTextFieldValidators()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            newEmailInputField.removeAllEditTextWatchers()
            passwordInputField.removeAllEditTextWatchers()
            confirmPasswordInputField.removeAllEditTextWatchers()
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(newEmailInputField, Constants.emailInputError) ||
                    FieldUtils.isTextFieldInvalid(passwordInputField, Constants.passwordInputError)
                || FieldUtils.isTextFieldInvalid(confirmPasswordInputField, Constants.confirmPasswordInputError)
        }
    }
}