package com.example.hobbyfi.ui.main

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentUserProfileBinding
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.factories.UserProfileFragmentViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.example.hobbyfi.viewmodels.main.UserProfileFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class UserProfileFragment : MainFragment(), TextFieldInputValidationOnus {

    private val viewModel: UserProfileFragmentViewModel by viewModels(factoryProducer = {
        UserProfileFragmentViewModelFactory(requireActivity().application,
            UserProfileFragmentArgs.fromBundle(requireActivity().intent?.extras!!)
            .user?.tags ?: emptyList())
    })

    private val activityViewModel: MainActivityViewModel by activityViewModels()

    private lateinit var binding: FragmentUserProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_profile, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this // in case livedata is needed to be observed

        // TODO: Handle expired token error & logout
        binding.profileImage.setOnClickListener {
            // TODO: Image selection
        }

        binding.deleteButton.setOnClickListener {
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
                .setIcon(R.drawable.ic_baseline_exit_to_app_24)
                .create()
        }

        binding.confirmButton.setOnClickListener {
            if(FieldUtils.isTextFieldInvalid(binding.textInputUsername) || FieldUtils.isTextFieldInvalid(binding.textInputDescription)) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                // activityViewModel.sendIntent(UserIntent.UpdateUser())
            }
        }

        val user = UserProfileFragmentArgs.fromBundle(requireActivity().intent?.extras!!)
            .user // fixme: code dup

        lifecycleScope.launch {
            if(user == null)
                activityViewModel.sendIntent(UserIntent.FetchUser) else activityViewModel.setAndSaveUser(user)
        }

        // observe
        activityViewModel.authUser.observe(viewLifecycleOwner, {
            if(it != null) {
                viewModel.description.value = it.description
                viewModel.username.value = it.name
                binding.profileImage.load(it.photoUrl)
                // TODO: Send email as argument to emailchangedialogfragment for autofill
            }
        })

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.emailChangeButton.setOnClickListener {
            navController.navigate(R.id.action_userProfileFragment_to_changeEmailDialogFragment)
        }

        binding.passwordChangeButton.setOnClickListener {
            navController.navigate(R.id.action_userProfileFragment_to_changePasswordDialogFragment)
        }

        binding.tagSelectButton.setOnClickListener {
            val action = UserProfileFragmentDirections.actionUserProfileFragmentToTagSelectionDialogFragment(
                viewModel.selectedTags.toTypedArray(),
                viewModel.tags.toTypedArray()
            )
            navController.navigate(action)
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)
            ?.observe(viewLifecycleOwner) { selectedTags ->
                viewModel.appendNewSelectedTagsToTags(selectedTags)
                viewModel.setSelectedTags(selectedTags)
            }
    }

    override fun initTextFieldValidators() {
        binding.textInputUsername.addTextChangedListener(
            PredicateTextWatcher(
                binding.textInputUsername,
                Constants.usernameInputError,
                Predicate {
                    return@Predicate it.isEmpty() || it.length >= 25
                })
        )
        binding.textInputDescription.addTextChangedListener(
            PredicateTextWatcher(
                binding.textInputDescription,
                Constants.descriptionInputError,
                Predicate {
                    return@Predicate it.length >= 30
                })
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // TODO: Image result
    }
}