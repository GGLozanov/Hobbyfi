package com.example.hobbyfi.ui.auth

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.LoginFragmentBinding
import com.example.hobbyfi.databinding.RegisterFragmentBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.auth.LoginFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import kotlinx.android.synthetic.main.login_fragment.*
import kotlinx.android.synthetic.main.login_fragment.text_input_email
import kotlinx.android.synthetic.main.login_fragment.text_input_password
import kotlinx.android.synthetic.main.register_fragment.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class LoginFragment : AuthFragment() {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private val viewModel: LoginFragmentViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        val binding: LoginFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.login_fragment, container, false)

        binding.viewModel = viewModel

        val view: View = binding.root

        login_button.setOnClickListener {
            lifecycleScope.launch {
                viewModel.sendIntent(TokenIntent.FetchLoginToken)
            }
        }

        facebook_button.setOnClickListener {

        }

        lifecycleScope.launch {
            viewModel.state.collect {
                when(it) {
                    is TokenState.Idle -> {

                    }
                    is TokenState.Loading -> {

                    }
                    is TokenState.Error -> {

                    }
                    is TokenState.OnTokenReceived -> {

                    }
                }
            }
        }

        return view
    }


    override fun initTextFieldValidators() {
        text_input_email.addTextChangedListener(
            PredicateTextWatcher(
                text_input_email,
                Constants.emailInputError,
                Predicate {
                    return@Predicate it.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(it).matches()
                })
        )

        text_input_password.addTextChangedListener(
            PredicateTextWatcher(
                text_input_password,
                Constants.passwordInputError,
                Predicate {
                    return@Predicate it.isEmpty() || it.length >= 15
                })
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.login_appbar_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)

    }
}