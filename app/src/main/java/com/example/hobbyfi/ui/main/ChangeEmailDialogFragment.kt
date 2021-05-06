package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChangeEmailDialogBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.main.ChangeEmailDialogFragmentViewModel
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
                    context?.showWarningToast(getString(R.string.same_email_validation))
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
                            context?.showFailureToast(it.error ?: getString(R.string.something_wrong))
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
                TextInputLayoutFocusValidatorObserver(binding.newEmailInputField, getString(R.string.email_input_error))
            )

            password.invalidity.observe(
                viewLifecycleOwner,
                object : TextInputLayoutFocusObserver<Boolean>(binding.passwordInputField) {
                    override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                        textInputLayout.error = if(t) getString(R.string.password_input_error) else null
                        binding.confirmPasswordInputField.error =
                            if(t && confirmPassword.invalidity.value == true) getString(R.string.confirm_password_input_error) else null
                    }
                }
            )

            confirmPassword.invalidity.observe(
                viewLifecycleOwner,
                object : TextInputLayoutFocusObserver<Boolean>(binding.confirmPasswordInputField) {
                    override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                        textInputLayout.error = if(t) getString(R.string.confirm_password_input_error) else null
                        binding.passwordInputField.error =
                            if(t && password.invalidity.value == true) getString(R.string.password_input_error) else null
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