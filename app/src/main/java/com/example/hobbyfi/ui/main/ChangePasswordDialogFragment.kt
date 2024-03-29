package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChangePasswordDialogBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.main.ChangePasswordDialogFragmentViewModel
import com.google.android.material.textfield.TextInputLayout
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
                    context?.showWarningToast(getString(R.string.password_uniqeness))
                    return@setOnClickListener
                }

                viewModel!!.email.value = (requireActivity() as MainActivity).viewModel.authUser.value?.email
                    // set user email to AuthUser Activity VM email

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
                                Pair(Constants.PASSWORD, viewModel!!.newPassword.value!!)
                            )))
                            dismiss()
                        }
                        is TokenState.Error -> {
                            context?.showWarningToast(getString(R.string.enter_original_password))
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
                TextInputLayoutFocusValidatorObserver(binding.passwordInputField, getString(R.string.password_input_error))
            )

            newPassword.invalidity.observe(
                viewLifecycleOwner,
                object : TextInputLayoutFocusObserver<Boolean>(binding.newPasswordInputField) {
                    override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                        textInputLayout.error = if(t) getString(R.string.password_input_error) else null
                        binding.confirmNewPasswordInputField.error =
                            if(t && confirmPassword.invalidity.value == true) getString(R.string.confirm_password_input_error) else null
                    }
                }
            )

            confirmPassword.invalidity.observe(
                viewLifecycleOwner,
                object : TextInputLayoutFocusObserver<Boolean>(binding.confirmNewPasswordInputField) {
                    override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                        textInputLayout.error = if(t) getString(R.string.confirm_password_input_error) else null
                        binding.newPasswordInputField.error =
                            if(t && newPassword.invalidity.value == true) getString(R.string.password_input_error) else null
                    }
                }
            )
        }
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, {
            with(viewModel) {
                Log.i("ChangePassDFVM", "PASSWORD invalidity: ${password.invalidity.value}")
                Log.i("ChangePassDFVM", "NEWPASSWORD invalidity: ${newPassword.invalidity.value}")
                Log.i("ChangePassDFVM", "CONFIRMPASSWORD invalidity: ${confirmPassword.invalidity.value}")
            }

            binding.buttonBar.rightButton.isEnabled = it == false
            // ViewReverseEnablerObserver(binding.buttonBar.rightButton))
        })
    }

    override fun onStart() {
        super.onStart()
        observePredicateValidators()
    }

}