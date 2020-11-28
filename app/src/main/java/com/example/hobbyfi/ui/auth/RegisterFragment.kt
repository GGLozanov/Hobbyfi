package com.example.hobbyfi.ui.auth

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.onNavDestinationSelected
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.viewmodels.auth.RegisterFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.EasyPermissions


@ExperimentalCoroutinesApi
class RegisterFragment : AuthFragment(), TextFieldInputValidationOnus {
    private val viewModel: RegisterFragmentViewModel by viewModels()
    private lateinit var binding: FragmentRegisterBinding

    private val imageRequestCode: Int = 777
    private var bitmap: Bitmap? = null

    companion object {
        val tag: String = "RegisterFragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_register, container, false)

        binding.viewModel = viewModel

        with(binding) {
            lifecycleOwner = this@RegisterFragment

            val view = root

            initTextFieldValidators()

            profileImage.setOnClickListener { // viewbinding, WOOO! No Kotlin synthetics here
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
                    EasyPermissions.requestPermissions(this@RegisterFragment, getString(R.string.read_external_storage_rationale),
                        200, Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }

            registerAccountButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    Toast.makeText(context, "Invalid information entered!", Toast.LENGTH_LONG)
                        .show() // TODO: Extract into separate error text
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.FetchRegisterToken)
                }
            }

            return view
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tagSelectButton.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToTagNavGraph(
                viewModel.selectedTags.toTypedArray(),
                viewModel.tags.toTypedArray()
            )
            navController.navigate(action)
        }

        lifecycleScope.launch {
            viewModel.mainState.collect {
                when(it) {
                    is TokenState.Idle -> {
                    }
                    is TokenState.Error -> {
                        Toast.makeText(context, it.error, Toast.LENGTH_LONG)
                            .show()
                    }
                    is TokenState.Loading -> {
                        // TODO: Show a progress indicator or something
                    }
                    is TokenState.TokenReceived -> {
                        val id = TokenUtils.getTokenUserIdFromPayload(it.token?.jwt)

                        login(
                            RegisterFragmentDirections.actionRegisterFragmentToMainActivity(User(
                                id,
                                viewModel.email.value!!,
                                viewModel.username.value!!,
                                viewModel.description.value,
                                BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir
                                        + "/" + id + ".jpg", // FIXME: Find a better way to do this; exposes API logic...
                                viewModel.selectedTags, // FIXME: User has null tags or just an empty list?
                                null
                            )),
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
            viewModel.appendNewSelectedTagsToTags(selectedTags)
            viewModel.setSelectedTags(selectedTags)
        }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            textInputEmail.addTextChangedListener(
                Constants.emailInputError,
                Constants.emailPredicate
            )

            textInputPassword.addTextChangedListener(
                Constants.passwordInputError,
                Constants.passwordPredicate
            )

            textInputConfirmPassword.addTextChangedListener(
                Constants.confirmPasswordInputError,
                Constants.confirmPasswordPredicate(viewModel!!.password.value)
            )

            textInputUsername.addTextChangedListener(
                Constants.usernameInputError,
                Constants.usernamePredicate
            )

            textInputDescription.addTextChangedListener(
                Constants.descriptionInputError,
                Constants.descriptionPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(textInputEmail) ||
                    FieldUtils.isTextFieldInvalid(textInputPassword) ||
                    FieldUtils.isTextFieldInvalid(textInputUsername) ||
                    FieldUtils.isTextFieldInvalid(textInputConfirmPassword) ||
                    FieldUtils.isTextFieldInvalid(textInputDescription)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.register_appbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
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