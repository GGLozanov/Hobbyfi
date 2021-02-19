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
import com.example.hobbyfi.shared.TextInputLayoutFocusObserver
import com.example.hobbyfi.shared.TextInputLayoutFocusValidatorObserver
import com.example.hobbyfi.shared.ViewReverseEnablerObserver
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.main.ChangeEmailDialogFragmentViewModel
import com.example.hobbyfi.viewmodels.main.ChangePasswordDialogFragmentViewModel
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChangeEmailDialogFragment : AuthChangeDialogFragment() {
    private val viewModel: ChangeEmailDialogFragmentViewModel by viewModels()
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
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@ChangeEmailDialogFragment

            buttonBar.leftButton.setOnClickListener { dismiss() }
            buttonBar.rightButton.setOnClickListener {
                val newEmail = viewModel!!.email.value
                val originalEmail = (requireActivity() as MainActivity).viewModel.authUser.value?.email

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
                            (requireActivity() as MainActivity).viewModel.sendIntent(UserIntent.UpdateUser(mutableMapOf(
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

    override fun observePredicateValidators() {
        with(viewModel) {
            email.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.newEmailInputField, Constants.emailInputError)
            )

            password.invalidity.observe(
                viewLifecycleOwner,
                object : TextInputLayoutFocusObserver<Boolean>(binding.passwordInputField) {
                    override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                        textInputLayout.error = if(t) Constants.passwordInputError else null
                        binding.confirmPasswordInputField.error =
                            if(t && confirmPassword.invalidity.value == true) Constants.confirmPasswordInputError else null
                    }
                }
            )

            confirmPassword.invalidity.observe(
                viewLifecycleOwner,
                object : TextInputLayoutFocusObserver<Boolean>(binding.confirmPasswordInputField) {
                    override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                        textInputLayout.error = if(t) Constants.confirmPasswordInputError else null
                        binding.passwordInputField.error =
                            if(t && password.invalidity.value == true) Constants.passwordInputError else null
                    }
                }
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