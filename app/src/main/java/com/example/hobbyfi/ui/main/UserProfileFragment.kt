package com.example.hobbyfi.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentUserProfileBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.factories.TagListViewModelFactory
import com.example.hobbyfi.viewmodels.main.UserProfileFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class UserProfileFragment : MainFragment(), TextFieldInputValidationOnus {
    private val viewModel: UserProfileFragmentViewModel by viewModels(factoryProducer = {
        val tags = activityViewModel.authUser.value?.tags ?:
            if(requireActivity().intent?.extras == null)
                arrayListOf()
            else MainActivityArgs.fromBundle(
                    requireActivity().intent?.extras!!
                ).user?.tags ?: arrayListOf()

        TagListViewModelFactory(
            requireActivity().application,
            tags
        )
    })

    private lateinit var binding: FragmentUserProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_user_profile,
            container,
            false
        )
        binding.viewModel = viewModel
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@UserProfileFragment // in case livedata is needed to be observed from binding

            // FIXME: Code dup w/ other profile selection Fragments
            profileImage.galleryOption.setOnClickListener {
                Callbacks.requestImage(this@UserProfileFragment)
            }

            profileImage.cameraOption.setOnClickListener {
                navController.navigate(R.id.action_userProfileFragment_to_camera_capture_nav_graph)
            }

            settingsButtonBar.leftButton.setOnClickListener { // delete account button
                requireContext().buildYesNoAlertDialog(
                    getString(R.string.confirm_acc_deletion),
                    { dialogInterface: DialogInterface, _: Int ->
                        lifecycleScope.launch {
                            activityViewModel.sendIntent(UserIntent.DeleteUser)
                        }
                        dialogInterface.dismiss()
                    },
                    { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.dismiss()
                    }
                )
            }

            askSwitch.isChecked = prefConfig.readChatroomJoinRememberNavigate() == Constants.NoRememberDualChoice.REMEMBER_YES.ordinal
            askSwitch.setOnCheckedChangeListener { _, checked ->
                prefConfig.writeChatroomJoinRememberNavigate(if(checked) Constants.NoRememberDualChoice.REMEMBER_YES.ordinal
                    else Constants.NoRememberDualChoice.NO_REMEMBER.ordinal)
            }

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            if (!Constants.isFacebookUserAuthd()) {
                authButtonBar.leftButton.setOnClickListener { // change email button
                    navController.safeNavigate(R.id.action_global_changeEmailDialogFragment)
                }

                authButtonBar.rightButton.setOnClickListener { // change password button
                    navController.safeNavigate(R.id.action_global_changePasswordDialogFragment)
                }
            } else {
                authButtonBar.leftButton.visibility = View.GONE
                authButtonBar.rightButton.visibility = View.GONE
            }

            settingsButtonBar.rightButton.setOnClickListener { // tag selection button
                val action = UserProfileFragmentDirections.actionGlobalTagNavGraph(
                    viewModel!!.tagBundle.selectedTags.toTypedArray(),
                    viewModel!!.tagBundle.tags.toTypedArray()
                )
                navController.safeNavigate(action)
            }

            lifecycleScope.launch {
                if (activityViewModel.authUser.value == null) {
                    activityViewModel.sendIntent(UserIntent.FetchUser)
                }
            }

            confirmButton.setOnClickListener {
                val fieldMap: MutableMap<String, String?> = mutableMapOf()

                if (activityViewModel.authUser.value?.name != viewModel!!.name.value) {
                    fieldMap[Constants.USERNAME] = viewModel!!.name.value
                }

                if (activityViewModel.authUser.value?.description != viewModel!!.description.value) {
                    fieldMap[Constants.DESCRIPTION] = viewModel!!.description.value ?: ""
                }

                if ((activityViewModel.authUser.value?.tags
                        ?: arrayListOf()) != viewModel!!.tagBundle.selectedTags
                ) {
                    fieldMap[Constants.TAGS + "[]"] = Constants.jsonConverter
                        .toJson(viewModel!!.tagBundle.selectedTags)
                }

                if (viewModel!!.base64Image.originalUri != null) { // means user has changed their pfp
                    fieldMap[Constants.IMAGE] = viewModel!!.base64Image.originalUri
                }

                Log.i("UserProfileFragment", "FieldMap update: ${fieldMap}")
                if (fieldMap.isEmpty()) {
                    context?.showWarningToast(getString(R.string.no_fields))
                    return@setOnClickListener
                } else if(fieldMap.size == 1 && fieldMap.containsKey(Constants.IMAGE)) {
                    WorkerUtils.buildAndEnqueueImageUploadWorker(
                        activityViewModel.authUser.value!!.id,
                        prefConfig.getAuthUserToken()!!,
                        Constants.EDIT_USER_TYPE,
                        viewModel!!.base64Image.originalUri!!,
                        requireContext(),
                        R.string.pref_last_user_fetch_time
                    )
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    activityViewModel.sendIntent(UserIntent.UpdateUser(fieldMap))
                }

                activityViewModel.setIsUserProfileUpdateButtonEnabled(false)
            }

            observeUpdateButtonEnabled()
            observeAuthUser()
            observeTagsFail()

            // FIXME: Code dup with other tag fragments
            navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)
                ?.observe(viewLifecycleOwner) { selectedTags ->
                    viewModel!!.tagBundle.appendNewSelectedTagsToTags(selectedTags)
                    viewModel!!.tagBundle.setSelectedTags(selectedTags)
                }

            navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Uri?>(Constants.CAMERA_URI)
                ?.observe(viewLifecycleOwner) { uri ->
                    uri?.let {
                        binding.profileImage.image
                            .loadUriIntoGlideAndSaveInImageHolder(it, viewModel!!.base64Image)
                    }
                }
        }
    }

    private fun observeTagsFail() {
        activityViewModel.latestTagUpdateFail.observe(viewLifecycleOwner, Observer {
            if(it) {
                viewModel.tagBundle.setSelectedTags(viewModel.originalSelectedTags)
            } else {
                viewModel.setOriginalSelectedTags(viewModel.tagBundle.selectedTags)
            }
        })
    }

    private fun observeAuthUser() {
        activityViewModel.authUser.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                viewModel.description.value = it.description
                viewModel.name.value = it.name
                it.tags?.let { selectedTags ->
                    viewModel.tagBundle.setSelectedTags(selectedTags)
                    viewModel.tagBundle.appendNewSelectedTagsToTags(selectedTags)
                }

                if (it.photoUrl != null && navController.currentBackStackEntry
                        ?.savedStateHandle?.get<Uri?>(Constants.CAMERA_URI) == null) {
                    Log.i("UserProfileFragment", "User photo url: ${it.photoUrl}")
                    it.photoUrl!!.asFirebaseStorageReference()?.let { ref ->
                        ref.metadata.addOnSuccessListener { metadata ->
                            Log.i("UserProfileFragment", "metadata create time: ${metadata.creationTimeMillis}")
                            Glide.with(requireContext()).loadReferenceWithMetadataSignature(
                                ref, metadata
                            ).placeholder(binding.profileImage.image.drawable)
                                .into(binding.profileImage.image)
                        }
                    }
                } else {
                    // receivedCameraImageCapture = false
                    // load default img (needed if img deletion is added)
                }
            }
        })
    }

    private fun observeUpdateButtonEnabled() {
        activityViewModel.isUserProfileUpdateButtonEnabled.observe(viewLifecycleOwner, Observer {
            binding.confirmButton.isEnabled = it
        })
    }

    override fun observePredicateValidators() {
        with(viewModel) {
            name.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.usernameInputField, getString(R.string.name_input_error))
            )

            description.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.descriptionInputField, getString(R.string.description_input_error))
            )
        }
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.confirmButton))
    }

    override fun onStart() {
        super.onStart()
        observePredicateValidators()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        navController.currentBackStackEntry
            ?.savedStateHandle?.set(Constants.CAMERA_URI, null)
    }

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