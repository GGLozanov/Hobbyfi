package com.example.hobbyfi.ui.main

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.factories.UserProfileFragmentViewModelFactory
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import com.example.hobbyfi.viewmodels.main.UserProfileFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions

@ExperimentalCoroutinesApi
class UserProfileFragment : MainFragment(), TextFieldInputValidationOnus {

    private val viewModel: UserProfileFragmentViewModel by viewModels(factoryProducer = {
        UserProfileFragmentViewModelFactory(requireActivity().application,
            UserProfileFragmentArgs.fromBundle(requireActivity().intent?.extras!!)
            .user?.tags ?: emptyList())
    })

    private val activityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var binding: FragmentUserProfileBinding

    private val imageRequestCode: Int = 777
    private var bitmap: Bitmap? = null

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
        binding.lifecycleOwner = this // in case livedata is needed to be observed from binding

        // TODO: Handle expired token error & logout
        binding.profileImage.setOnClickListener {
            if(EasyPermissions.hasPermissions(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                val selectImageIntent = Intent()
                selectImageIntent.type = "image/*" // set MIME data type to all images

                selectImageIntent.action =
                    Intent.ACTION_GET_CONTENT // set the desired action to get image

                startActivityForResult(
                    selectImageIntent,
                    imageRequestCode
                ) // start activity and await result
            } else {
                EasyPermissions.requestPermissions(this, getString(R.string.read_external_storage_rationale),
                    200, Manifest.permission.READ_EXTERNAL_STORAGE)
            }
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
                .create()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

        binding.confirmButton.setOnClickListener {
            if(FieldUtils.isTextFieldInvalid(binding.textInputUsername) || FieldUtils.isTextFieldInvalid(binding.textInputDescription)) {
                return@setOnClickListener
            }
            val fieldMap: MutableMap<String?, String?> = mutableMapOf()

            if(user?.name != viewModel.username.value) {
                fieldMap[Constants.USERNAME] = viewModel.username.value
            }

            if(user?.description != viewModel.description.value) {
                fieldMap[Constants.DESCRIPTION] = viewModel.description.value
            }

            if(viewModel.base64Image != null) { // means user has changed their pfp
                fieldMap[Constants.PHOTO_URL] = viewModel.base64Image
            }

            lifecycleScope.launch {
                activityViewModel.sendIntent(UserIntent.UpdateUser(fieldMap))
            }
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // FIXME: Code dup from RegisterFragment
        if(Callbacks.getBitmapFromImageOnActivityResult(
                requireActivity(),
                imageRequestCode,
                requestCode,
                resultCode,
                data).also { bitmap = it } != null) {
            binding.profileImage.setImageBitmap(
                bitmap
            ) // set the new image resource to be decoded from the bitmap
            viewModel.setProfileImageBase64(
                ImageUtils.encodeImage(bitmap!!)
            )
        }
    }
}