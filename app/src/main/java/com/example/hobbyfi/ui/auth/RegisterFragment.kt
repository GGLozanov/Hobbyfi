package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.map
import com.bumptech.glide.Glide
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.models.ui.StepperButtonInput
import com.example.hobbyfi.models.ui.StepperFormInput
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.shared.ButtonStep
import com.example.hobbyfi.ui.shared.FormStep
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.auth.RegisterFragmentViewModel
import com.google.android.material.textfield.TextInputLayout
import ernestoyaquello.com.verticalstepperform.listener.StepperFormListener
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class RegisterFragment : AuthFragment(), StepperFormListener {
    private val viewModel: RegisterFragmentViewModel by viewModels()
    private lateinit var binding: FragmentRegisterBinding

    private val emailStep: FormStep get() =
        FormStep(getString(R.string.email), viewLifecycleOwner, getString(R.string.email_input_error),
            StepperFormInput(
                getString(R.string.email_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_email_white_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_CLEAR_TEXT,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                viewModel.email as PredicateMutableLiveData<String?>
            )
        )

    private val usernameStep: FormStep get() =
        FormStep(getString(R.string.username), viewLifecycleOwner, getString(R.string.username_input_error),
            StepperFormInput(
                getString(R.string.username_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_person_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_CLEAR_TEXT,
                InputType.TYPE_TEXT_FLAG_CAP_WORDS,
                viewModel.name as PredicateMutableLiveData<String?>
            )
        )

    private val passwordStep: FormStep get() =
        FormStep(getString(R.string.password), viewLifecycleOwner, getString(R.string.password_input_error),
            StepperFormInput(
                getString(R.string.password_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_password_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_PASSWORD_TOGGLE,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                viewModel.password as PredicateMutableLiveData<String?>
            )
        )

    private val confirmPasswordStep: FormStep get() =
        FormStep(getString(R.string.confirm_password), viewLifecycleOwner, getString(R.string.confirm_password_input_error),
            StepperFormInput(
                getString(R.string.confirm_password_hint),
                ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_password_24)
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_create_24)!!,
                TextInputLayout.END_ICON_PASSWORD_TOGGLE,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD,
                viewModel.confirmPassword as PredicateMutableLiveData<String?>
            )
        )

    private val descriptionStep: FormStep get() =
        FormStep(getString(R.string.description), viewLifecycleOwner, getString(R.string.description_input_error),
            StepperFormInput(
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

            val view = root

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
                .init()

            return view
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
                        binding.root.showFailureSnackbar(it.error ?: getString(R.string.something_wrong))
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
        Log.i("RegisterFragment", "Completed form w/ data: ${viewModel.email}")

        lifecycleScope.launch {
            viewModel.sendIntent(TokenIntent.FetchRegisterToken)
        }
    }

    override fun onCancelledForm() {}

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