package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.viewmodels.auth.RegisterFragmentViewModel
import com.google.android.material.textfield.TextInputLayout
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
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@RegisterFragment

            val view = root

            profileImage.setOnClickListener { // viewbinding, WOOO! No Kotlin synthetics here
                Callbacks.requestImage(this@RegisterFragment)
            }

            buttonBar.rightButton.setOnClickListener { // register account
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
                            RegisterFragmentDirections.actionRegisterFragmentToMainActivity(
                                User(
                                id,
                                viewModel.email.value!!,
                                viewModel.name.value!!,
                                viewModel.description.value,
                                if(viewModel.base64Image.base64 != null) BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir
                                        + "/" + id + ".jpg" else null, // FIXME: Find a better way to do this; exposes API logic...
                                viewModel.tagBundle.selectedTags,
                                null
                            )
                            ),
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

    override fun observePredicateValidators() {
        with(viewModel) {
            email.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.emailInputField, Constants.emailInputError)
            )

            password.invalidity
                .observe(viewLifecycleOwner, object : TextInputLayoutFocusObserver<Boolean>(binding.passwordInputField) {
                override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                    textInputLayout.error = if(t) Constants.passwordInputError else null
                    binding.confirmPasswordInputField.error =
                        if(t && confirmPassword.invalidity.value == true) Constants.confirmPasswordInputError else null
                }
            })

            confirmPassword.invalidity
                .observe(viewLifecycleOwner, object : TextInputLayoutFocusObserver<Boolean>(binding.confirmPasswordInputField) {
                override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                    textInputLayout.error = if(t) Constants.confirmPasswordInputError else null
                    binding.passwordInputField.error =
                        if(t && password.invalidity.value == true) Constants.passwordInputError else null
                }
            })

            name.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.usernameInputField, Constants.nameInputError)
            )

            description.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.descriptionInputField, Constants.descriptionInputError)
            )
        }
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.buttonBar.rightButton))
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