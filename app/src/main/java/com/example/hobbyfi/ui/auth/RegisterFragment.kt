package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.utils.WorkerUtils
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

            viewModel!!.base64Image.loadUriIntoWithoutSignature(requireContext(), profileImage)

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
            navController.safeNavigate(action)
        }

        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collectLatestWithLoadingAndNonIdleReset(listOf(TokenState.Idle::class),
                        viewModel::resetTokenState, viewLifecycleOwner, navController,
                    RegisterFragmentDirections.actionRegisterFragmentToLoadingNavGraph(R.id.registerFragment),
                    TokenState.Loading::class) {
                when(it) {
                    is TokenState.Idle -> {
                    }
                    is TokenState.Error -> {
                        binding.buttonBar.rightButton.isEnabled = true
                        view.showFailureSnackbar(it.error ?: getString(R.string.something_wrong))
                    }
                    is TokenState.TokenReceived -> {
                        val id = TokenUtils.getTokenUserIdFromPayload(it.token?.jwt)
                        Callbacks.hideKeyboardFrom(requireContext(), view)

                        viewModel.base64Image.originalUri?.let { image ->
                            WorkerUtils.buildAndEnqueueImageUploadWorker(
                                id,
                                it.token!!.jwt!!,
                                Constants.USERS,
                                image,
                                requireContext(),
                                R.string.pref_last_user_fetch_time
                            )
                        }

                        login(
                            RegisterFragmentDirections.actionRegisterFragmentToMainActivity(
                                User(
                                    id,
                                    viewModel.email.value!!,
                                    viewModel.name.value!!,
                                    viewModel.description.value,
                                    null,
                                    viewModel.tagBundle.selectedTags,
                                    null,
                                    null
                                ) // image upload schema dependent on WorkManager, repo modifications, & SSOT refetches (which is why URL is null here)
                            ),
                            it.token?.jwt,
                            it.token?.refreshJwt
                        )
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
                TextInputLayoutFocusValidatorObserver(binding.emailInputField, getString(R.string.email_input_error))
            )

            password.invalidity
                .observe(viewLifecycleOwner, object : TextInputLayoutFocusObserver<Boolean>(binding.passwordInputField) {
                override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                    textInputLayout.error = if(t) getString(R.string.password_input_error) else null
                    binding.confirmPasswordInputField.error =
                        if(t && confirmPassword.invalidity.value == true) getString(R.string.confirm_password_input_error) else null
                }
            })

            confirmPassword.invalidity
                .observe(viewLifecycleOwner, object : TextInputLayoutFocusObserver<Boolean>(binding.confirmPasswordInputField) {
                override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
                    textInputLayout.error = if(t) getString(R.string.confirm_password_input_error) else null
                    binding.passwordInputField.error =
                        if(t && password.invalidity.value == true) getString(R.string.password_input_error) else null
                }
            })

            name.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.usernameInputField,
                    getString(R.string.username_input_error))
            )

            description.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.descriptionInputField,
                    getString(R.string.description_input_error))
            )
        }
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.buttonBar.rightButton))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Callbacks.handleImageRequestWithPermission(
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            Glide.with(requireContext())
                .load(data!!.data!!)
                .into(binding.profileImage)
            viewModel.base64Image.setOriginalUri(data.data!!.toString())
        }
    }
}