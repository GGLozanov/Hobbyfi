package com.example.hobbyfi.ui.main

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.hobbyfi.R
import com.example.hobbyfi.adapters.tag.TagTypeAdapter
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
import com.google.gson.GsonBuilder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class UserProfileFragment : MainFragment(), TextFieldInputValidationOnus {
    private val viewModel: UserProfileFragmentViewModel by viewModels(factoryProducer = {
        TagListViewModelFactory(
            requireActivity().application,
            activityViewModel.authUser.value?.tags ?: UserProfileFragmentArgs.fromBundle(
                requireActivity().intent?.extras!!
            )
                .user?.tags ?: emptyList()
        )
    })

    private lateinit var binding: FragmentUserProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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

        with(binding) {
            lifecycleOwner = this@UserProfileFragment // in case livedata is needed to be observed from binding

            profileImage.setOnClickListener {
                Callbacks.requestImage(this@UserProfileFragment)
            }

            settingsButtonBar.leftButton.setOnClickListener { // delete account button
                AlertDialog.Builder(requireContext())
                    .setMessage("Are you certain you want to delete you account?")
                    .setPositiveButton("Yes") { dialogInterface: DialogInterface, _: Int ->
                        lifecycleScope.launch {
                            activityViewModel.sendIntent(UserIntent.DeleteUser)
                        }
                        dialogInterface.dismiss()
                    }
                    .setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.dismiss()
                    }
                    .create()
                    .show()
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
                    viewModel!!.selectedTags.toTypedArray(),
                    viewModel!!.tags.toTypedArray()
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
                        viewModel!!.setSelectedTags(selectedTags)
                        viewModel!!.appendNewSelectedTagsToTags(selectedTags)
                    }

                    if (it.photoUrl != null) {
                        Log.i("UserProfileFragment", "User photo url: ${it.photoUrl}")
                        profileImage.load(it.photoUrl)
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

                if((activityViewModel.authUser.value?.tags ?: emptyList()) != viewModel!!.selectedTags) {
                    fieldMap[Constants.TAGS + "[]"] = (GsonBuilder()
                        .registerTypeAdapter(Tag::class.java, TagTypeAdapter())
                        .create()) // TODO: Extract into DI/singleton/static var
                        .toJson(viewModel!!.selectedTags)
                }

                if(viewModel!!.base64Image != null) { // means user has changed their pfp
                    fieldMap[Constants.PHOTO_URL] = viewModel!!.base64Image
                }

                Log.i("UserProfileFragment", "FieldMap update: ${fieldMap}")
                if(fieldMap.isEmpty()) {
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
                viewModel.appendNewSelectedTagsToTags(selectedTags)
                viewModel.setSelectedTags(selectedTags)
            }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            textInputUsername.addTextChangedListener(
                Constants.usernameInputError,
                Constants.namePredicate
            )

            textInputDescription.addTextChangedListener(
                Constants.descriptionInputError,
                Constants.descriptionPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(textInputUsername) ||
                    FieldUtils.isTextFieldInvalid(textInputDescription)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Callbacks.handleImageRequestWithPermission(
            this,
            requireActivity(),
            requestCode,
            resultCode,
            data
        ) {
            binding.profileImage.load(
                it
            ) // set the new image resource to be decoded from the bitmap
            viewModel.setProfileImageBase64(
                ImageUtils.encodeImage(it)
            )
        }
    }
}