package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.models.ui.NonNullableStepperFormInput
import com.example.hobbyfi.models.ui.NullableStepperFormInput
import com.example.hobbyfi.models.ui.StepperButtonInput
import com.example.hobbyfi.models.ui.StepperFormInput
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.shared.ButtonStep
import com.example.hobbyfi.ui.shared.FormStep
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.auth.RegisterFragmentViewModel
import com.google.android.material.textfield.TextInputLayout
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener
import kotlinx.coroutines.*


@ExperimentalCoroutinesApi
class RegisterFragment : AuthFragment(), StepperFormListener {
    private val viewModel: RegisterFragmentViewModel by viewModels()
    private lateinit var binding: FragmentRegisterBinding

    private val emailStep: FormStep get() =
        FormStep(getString(R.string.email), viewLifecycleOwner, getString(R.string.email_input_error),
            NonNullableStepperFormInput(
                getString(R.string.email_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_email_white_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_CLEAR_TEXT,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                viewModel.email
            )
        )

    private val usernameStep: FormStep get() =
        FormStep(getString(R.string.username), viewLifecycleOwner, getString(R.string.username_input_error),
            NonNullableStepperFormInput(
                getString(R.string.username_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_person_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_CLEAR_TEXT,
                InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                viewModel.name
            )
        )

    private val passwordStep: FormStep get() =
        FormStep(getString(R.string.password), viewLifecycleOwner, getString(R.string.password_input_error),
            NonNullableStepperFormInput(
                getString(R.string.password_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_password_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_PASSWORD_TOGGLE,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                viewModel.password
            ), readableStepDataNotForbidden = false, emptyHint = getString(R.string.filled)
        )

    private val confirmPasswordStep: FormStep get() =
        FormStep(getString(R.string.confirm_password), viewLifecycleOwner, getString(R.string.confirm_password_input_error),
            NonNullableStepperFormInput(
                getString(R.string.confirm_password_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_password_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_PASSWORD_TOGGLE,
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,
                viewModel.confirmPassword
            ), readableStepDataNotForbidden = false, emptyHint = getString(R.string.filled)
        )

    private val descriptionStep: FormStep get() =
        FormStep(getString(R.string.description), viewLifecycleOwner, getString(R.string.description_input_error),
            NullableStepperFormInput(
                getString(R.string.description_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_CLEAR_TEXT,
                InputType.TYPE_TEXT_FLAG_CAP_SENTENCES,
                viewModel.description
            )
        )

    private val tagsStep: ButtonStep get() =
        ButtonStep(getString(R.string.tags), {
            val action = RegisterFragmentDirections.actionRegisterFragmentToTagNavGraph(
                viewModel.tagBundle.selectedTags.toTypedArray(),
                viewModel.tagBundle.tags.toTypedArray()
            )
            navController.safeNavigate(action)
        }, StepperButtonInput(getString(R.string.choose_tags), ContextCompat
            .getDrawable(requireContext(), R.drawable.ic_baseline_tag_faces_24)!!))

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

            profileImage.galleryOption.setOnClickListener { // viewbinding, WOOO! No Kotlin synthetics here
                Callbacks.requestImage(this@RegisterFragment)
            }

            profileImage.cameraOption.setOnClickListener {
                navController.navigate(R.id.action_registerFragment_to_camera_capture_nav_graph)
            }

            viewModel!!.base64Image.loadUriIntoWithoutSignature(requireContext(), profileImage.image)

            stepperForm
                .setup(this@RegisterFragment,
                    emailStep, usernameStep, passwordStep,
                    confirmPasswordStep, descriptionStep, tagsStep)
                .allowNonLinearNavigation(true)
                .allowStepOpeningOnHeaderClick(true)
                .init()

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launchWhenCreated {
            viewModel.mainState.collectLatestWithLoadingAndNonIdleReset(listOf(TokenState.Idle::class),
                        viewModel::resetTokenState, viewLifecycleOwner, navController,
                    RegisterFragmentDirections.actionRegisterFragmentToLoadingNavGraph(R.id.registerFragment),
                    TokenState.Loading::class) {
                when(it) {
                    is TokenState.Idle -> {
                    }
                    is TokenState.Error -> {
                        // bit of a hack due to race condition on RegisterFragment view w/ LoadingFragment pop from backstack
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                delay(1500)
                            }

                            this@RegisterFragment.context?.showFailureToast(it.error ?: getString(R.string.something_wrong))
                            binding.stepperForm.cancelFormCompletionOrCancellationAttempt()
                        }
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

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Uri>(Constants.CAMERA_URI)
            ?.observe(viewLifecycleOwner) { uri ->
                binding.profileImage.image
                    .loadUriIntoGlideAndSaveInImageHolder(uri, viewModel.base64Image)
            }
    }

    override fun onCompletedForm() {
        Log.i("RegisterFragment", "Completed form w/ data")

        lifecycleScope.launch {
            viewModel.sendIntent(TokenIntent.FetchRegisterToken)
        }
    }

    override fun onCancelledForm() {
        navController.popBackStack(R.id.authWrapperFragment, false)
    }

//    override fun onViewStateRestored(savedInstanceState: Bundle?) {
//        emailStep.restoreStepData(null)
//        usernameStep.restoreStepData(null)
//        passwordStep.restoreStepData(null)
//        confirmPasswordStep.restoreStepData(null)
//        descriptionStep.restoreStepData(null)
//        super.onViewStateRestored(savedInstanceState)
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Callbacks.handleImageRequestWithPermission(
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            binding.profileImage.image
                .loadUriIntoGlideAndSaveInImageHolder(data!!.data!!, viewModel.base64Image)
        }
    }
}