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
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
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
                emptyList()
            else MainActivityArgs.fromBundle(
                    requireActivity().intent?.extras!!
                ).user?.tags ?: emptyList()

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

        initTextFieldValidators()

        with(binding) {
            lifecycleOwner = this@UserProfileFragment // in case livedata is needed to be observed from binding

            profileImage.setOnClickListener {
                Callbacks.requestImage(this@UserProfileFragment)
            }

            settingsButtonBar.leftButton.setOnClickListener { // delete account button
                Constants.buildYesNoAlertDialog(
                    requireContext(),
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
            if(!Constants.isFacebookUserAuthd()) {
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
                if(activityViewModel.authUser.value == null) {
                    activityViewModel.sendIntent(UserIntent.FetchUser)
                }
            }

            // observe
            activityViewModel.authUser.observe(viewLifecycleOwner, Observer {
                if (it != null) {
                    viewModel!!.description.value = it.description
                    viewModel!!.name.value = it.name
                    it.tags?.let { selectedTags ->
                        viewModel!!.tagBundle.setSelectedTags(selectedTags)
                        viewModel!!.tagBundle.appendNewSelectedTagsToTags(selectedTags)
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

            confirmButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                val fieldMap: MutableMap<String?, String?> = mutableMapOf()

                if(activityViewModel.authUser.value?.name != viewModel!!.name.value) {
                    fieldMap[Constants.USERNAME] = viewModel!!.name.value
                }

                if(activityViewModel.authUser.value?.description != viewModel!!.description.value) {
                    fieldMap[Constants.DESCRIPTION] = viewModel!!.description.value
                }

                if((activityViewModel.authUser.value?.tags ?: emptyList()) != viewModel!!.tagBundle.selectedTags) {
                    fieldMap[Constants.TAGS + "[]"] = Constants.tagJsonConverter
                        .toJson(viewModel!!.tagBundle.selectedTags)
                }

                if(viewModel!!.base64Image.base64 != null) { // means user has changed their pfp
                    fieldMap[Constants.IMAGE] = viewModel!!.base64Image.base64
                }

                Log.i("UserProfileFragment", "FieldMap update: ${fieldMap}")
                if(fieldMap.isEmpty()) {
                    Toast.makeText(requireContext(), Constants.noUpdateFields, Toast.LENGTH_LONG)
                        .show()
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    activityViewModel.sendIntent(UserIntent.UpdateUser(fieldMap))
                }

                it.isEnabled = false // antispam
                it.postDelayed({ // append delayed message to internal handler's message queue to reenable the button
                    it.isEnabled = true
                }, 1000 * 5) // reenable after delay
            }
        }

        // FIXME: Code dup with other tag fragments
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)
            ?.observe(viewLifecycleOwner) { selectedTags ->
                viewModel.tagBundle.appendNewSelectedTagsToTags(selectedTags)
                viewModel.tagBundle.setSelectedTags(selectedTags)
            }

        activityViewModel.latestTagUpdateFail.observe(viewLifecycleOwner, Observer {
            if(it) {
                viewModel.tagBundle.setSelectedTags(viewModel.originalSelectedTags)
            } else {
                viewModel.setOriginalSelectedTags(viewModel.tagBundle.selectedTags)
            }
        })
    }

    override fun initTextFieldValidators() {
        with(binding) {
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

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(usernameInputField, Constants.usernameInputError) ||
                    FieldUtils.isTextFieldInvalid(descriptionInputField, Constants.descriptionInputError)
        }
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