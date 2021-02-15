package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.shared.removeAllEditTextWatchers
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.viewmodels.auth.RegisterFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class RegisterFragment : AuthFragment() {
    private val viewModel: RegisterFragmentViewModel by viewModels()
    private lateinit var binding: FragmentRegisterBinding

    companion object {
        const val tag: String = "RegisterFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false)

        binding.viewModel = viewModel

        with(binding) {
            lifecycleOwner = this@RegisterFragment

            val view = root

            profileImage.setOnClickListener { // viewbinding, WOOO! No Kotlin synthetics here
                Callbacks.requestImage(this@RegisterFragment)
            }

            buttonBar.rightButton.setOnClickListener { // register account
                if(assertTextFieldsInvalidity()) {
                    Toast.makeText(context, "Invalid information entered!", Toast.LENGTH_LONG)
                        .show() // TODO: Extract into separate error text
                    return@setOnClickListener
                }
                
                buttonBar.rightButton.isEnabled = false

                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.FetchRegisterToken)
                }
            }

            return view
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonBar.leftButton.setOnClickListener { // select tags
            val action = RegisterFragmentDirections.actionRegisterFragmentToTagNavGraph(
                viewModel.tagBundle.selectedTags.toTypedArray(),
                viewModel.tagBundle.tags.toTypedArray()
            )
            navController.navigate(action)
        }

        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collect {
                when(it) {
                    is TokenState.Idle -> {
                    }
                    is TokenState.Error -> {
                        binding.buttonBar.rightButton.isEnabled = true
                        Toast.makeText(context, it.error, Toast.LENGTH_LONG)
                            .show()
                    }
                    is TokenState.Loading -> {
                        // TODO: Show a progress indicator or something
                    }
                    is TokenState.TokenReceived -> {
                        val id = TokenUtils.getTokenUserIdFromPayload(it.token?.jwt)
                        Callbacks.hideKeyboardFrom(requireContext(), requireView())

                        login(
                            RegisterFragmentDirections.actionRegisterFragmentToMainActivity(User(
                                id,
                                viewModel.email.value!!,
                                viewModel.name.value!!,
                                viewModel.description.value,
                                if(viewModel.base64Image.base64 != null) BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir
                                        + "/" + id + ".jpg" else null, // FIXME: Find a better way to do this; exposes API logic...
                                viewModel.tagBundle.selectedTags,
                                null
                            )),
                            it.token?.jwt,
                            it.token?.refreshJwt
                        )
                        viewModel.resetTokenState()
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)
            ?.observe(viewLifecycleOwner) { selectedTags ->
            viewModel.tagBundle.appendNewSelectedTagsToTags(selectedTags)
            viewModel.tagBundle.setSelectedTags(selectedTags)
        }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            emailInputField.addTextChangedListener(
                Constants.emailInputError,
                Constants.emailPredicate
            )

            passwordInputField.addTextChangedListener(
                Constants.passwordInputError,
                Constants.passwordPredicate(confirmPasswordInputField.editText)
            )

            confirmPasswordInputField.addTextChangedListener(
                Constants.confirmPasswordInputError,
                Constants.confirmPasswordPredicate(passwordInputField.editText!!)
            )

            usernameInputField.addTextChangedListener(
                Constants.usernameInputError,
                Constants.namePredicate
            )

            descriptionInputField.addTextChangedListener(
                Constants.descriptionInputError,
                Constants.descriptionPredicate
            )
        }
    }

    override fun onStart() {
        super.onStart()
        initTextFieldValidators()
    }

    override fun onPause() {
        super.onPause()
        with(binding) {
            emailInputField.removeAllEditTextWatchers()
            passwordInputField.removeAllEditTextWatchers()
            confirmPasswordInputField.removeAllEditTextWatchers()
            usernameInputField.removeAllEditTextWatchers()
            descriptionInputField.removeAllEditTextWatchers()
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(emailInputField, Constants.emailInputError) ||
                    FieldUtils.isTextFieldInvalid(passwordInputField, Constants.passwordInputError) ||
                    FieldUtils.isTextFieldInvalid(usernameInputField, Constants.usernameInputError) ||
                    FieldUtils.isTextFieldInvalid(confirmPasswordInputField, Constants.confirmPasswordInputError) ||
                    FieldUtils.isTextFieldInvalid(descriptionInputField, Constants.descriptionInputError)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.register_appbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Callbacks.handleImageRequestWithPermission(
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            binding.profileImage.setImageBitmap(it) // set the new image resource to be decoded from the bitmap
            lifecycleScope.launch {
                viewModel.base64Image.setImageBase64(
                    ImageUtils.encodeImage(it)
                )
            }
        }
    }
}