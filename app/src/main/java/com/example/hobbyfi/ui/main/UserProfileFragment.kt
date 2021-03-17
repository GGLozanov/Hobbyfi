package com.example.hobbyfi.ui.main

import android.content.DialogInterface
import android.content.Intent
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

            profileImage.setOnClickListener {
                Callbacks.requestImage(this@UserProfileFragment)
            }

            settingsButtonBar.leftButton.setOnClickListener { // delete account button
                requireContext().buildYesNoAlertDialog(
                    Constants.confirmAccountDeletionMessage,
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

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            if (!Constants.isFacebookUserAuthd()) {
                authButtonBar.leftButton.setOnClickListener { // change email button
                    navController.navigate(R.id.action_global_changeEmailDialogFragment)
                }

                authButtonBar.rightButton.setOnClickListener { // change password button
                    navController.navigate(R.id.action_global_changePasswordDialogFragment)
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
                navController.navigate(action)
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

                if (viewModel!!.base64Image.base64 != null) { // means user has changed their pfp
                    fieldMap[Constants.IMAGE] = viewModel!!.base64Image.base64
                }

                Log.i("UserProfileFragment", "FieldMap update: ${fieldMap}")
                if (fieldMap.isEmpty()) {
                    Toast.makeText(requireContext(), Constants.noUpdateFields, Toast.LENGTH_LONG)
                        .show()
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
        // observe
        activityViewModel.authUser.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                viewModel.description.value = it.description
                viewModel.name.value = it.name
                it.tags?.let { selectedTags ->
                    viewModel.tagBundle.setSelectedTags(selectedTags)
                    viewModel.tagBundle.appendNewSelectedTagsToTags(selectedTags)
                }

                if (it.photoUrl != null) {
                    Log.i("UserProfileFragment", "User photo url: ${it.photoUrl}")
                    Glide.with(this@UserProfileFragment).load(
                        it.photoUrl!!
                    ).signature(ObjectKey(prefConfig.readLastPrefFetchTime(R.string.pref_last_user_fetch_time)))
                        .placeholder(binding.profileImage.drawable) // TODO: Hacky fix for always loading image in ANY user update. NEED to fix this beyond UI hack
                        .into(binding.profileImage)
                } else {
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
                TextInputLayoutFocusValidatorObserver(binding.usernameInputField, Constants.nameInputError)
            )

            description.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.descriptionInputField, Constants.descriptionInputError)
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
            lifecycleScope.launch {
                viewModel.base64Image.setImageBase64(
                    ImageUtils.encodeImage(it)
                )
            }
        }
    }
}