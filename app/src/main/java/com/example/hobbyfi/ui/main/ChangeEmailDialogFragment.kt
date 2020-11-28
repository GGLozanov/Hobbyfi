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
import com.example.hobbyfi.databinding.FragmentChangeEmailDialogBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
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

        with(binding) {
            lifecycleOwner = this@ChangeEmailDialogFragment

            cancelButton.setOnClickListener { dismiss() }
            confirmButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel.sendIntent(TokenIntent.FetchLoginToken)
                }
            }

            lifecycleScope.launch {
                viewModel.mainState.collect {
                    when(it) {
                        is TokenState.TokenReceived -> {
                            activityViewModel.sendIntent(UserIntent.UpdateUser(mutableMapOf(
                                Pair(Constants.EMAIL, viewModel!!.email.value!!)))
                            )
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
        TODO("Not yet implemented")
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        TODO("Not yet implemented")
    }
}