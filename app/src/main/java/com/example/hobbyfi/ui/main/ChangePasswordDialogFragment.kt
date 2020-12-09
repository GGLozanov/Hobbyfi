package com.example.hobbyfi.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentChangePasswordDialogBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.BaseDialogFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.main.AuthChangeDialogFragmentViewModel
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class ChangePasswordDialogFragment : AuthChangeDialogFragment() {
    private val viewModel: AuthChangeDialogFragmentViewModel by viewModels()
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
        with(binding) {
            lifecycleOwner = this@ChangePasswordDialogFragment

            buttonBar.leftButton.setOnClickListener { dismiss() }
            buttonBar.rightButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                viewModel!!.email.value = activityViewModel.authUser.value?.email // set user email to AuthUser Activity VM email

                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.FetchLoginToken)
                }
            }

            lifecycleScope.launch {
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
                                Pair(
                                    Constants.PASSWORD, viewModel!!.password.value!!)
                            )))
                        }
                        is TokenState.Error -> {
                            Toast.makeText(requireContext(), it.error, Toast.LENGTH_LONG)
                                .show()
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
            textInputPassword.addTextChangedListener(
                Constants.passwordInputError,
                Constants.passwordPredicate
            )
            textInputNewPassword.addTextChangedListener(
                Constants.passwordInputError,
                Constants.passwordPredicate
            )
            textInputConfirmNewPassword.addTextChangedListener(
                Constants.confirmPasswordInputError,
                Constants.confirmPasswordPredicate(viewModel!!.password.value)
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(textInputPassword)
                    || FieldUtils.isTextFieldInvalid(textInputNewPassword) || FieldUtils.isTextFieldInvalid(textInputConfirmNewPassword)
        }
    }
}