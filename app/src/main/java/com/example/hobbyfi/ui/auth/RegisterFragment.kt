package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentRegisterBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
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


@ExperimentalCoroutinesApi
class RegisterFragment : AuthFragment(), TextFieldInputValidationOnus {
    private val viewModel: RegisterFragmentViewModel by viewModels()
    private lateinit var binding: FragmentRegisterBinding

    private var imageRequestCode: Int = 777
    private var bitmap: Bitmap? = null

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
        binding.lifecycleOwner = this

        val view = binding.root

        initTextFieldValidators()

        binding.profileImage.setOnClickListener { // viewbinding, WOOO! No Kotlin synthetics here
            // TODO: Ask for Read storage permission here
            val selectImageIntent = Intent()
            selectImageIntent.type = "image/*" // set MIME data type to all images

            selectImageIntent.action =
                Intent.ACTION_GET_CONTENT // set the desired action to get image

            startActivityForResult(
                selectImageIntent,
                imageRequestCode
            ) // start activity and await result
        }

        binding.registerAccountButton.setOnClickListener {
            if(FieldUtils.isTextFieldInvalid(binding.textInputEmail) ||
                FieldUtils.isTextFieldInvalid(binding.textInputPassword) ||
                FieldUtils.isTextFieldInvalid(binding.textInputUsername) ||
                FieldUtils.isTextFieldInvalid(binding.textInputConfirmPassword) ||
                    FieldUtils.isTextFieldInvalid(binding.textInputDescription)) {
                Toast.makeText(context, "Invalid information entered!", Toast.LENGTH_LONG)
                    .show() // TODO: Extract into separate error text
                return@setOnClickListener
            }

            lifecycleScope.launch {
                viewModel.sendIntent(TokenIntent.FetchRegisterToken)
            }
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.tagSelectButton.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToTagSelectionDialogFragment(
                viewModel.selectedTags.toTypedArray(),
                viewModel.tags.toTypedArray()
            )
            navController.navigate(action)
        }

        lifecycleScope.launch {
            viewModel.state.collect {
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
                        prefConfig.writeToken(it.token?.jwt!!)
                        prefConfig.writeRefreshToken(it.token.refreshJwt!!)

                        val id = TokenUtils.getTokenUserIdFromPayload(it.token.jwt)

                        login(
                            User(
                                id,
                                viewModel.email.value!!,
                                viewModel.username.value!!,
                                viewModel.description.value,
                                BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir
                                        + "/" + id + ".jpg", // FIXME: Find a better way to do this; exposes API logic...
                                viewModel.selectedTags, // TODO: User has null tags or just an empty list?
                                null
                            )
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
        binding.textInputEmail.addTextChangedListener(
            PredicateTextWatcher(
                binding.textInputEmail,
                Constants.emailInputError,
                Predicate {
                    return@Predicate it.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(it).matches()
                })
        )

        binding.textInputPassword.addTextChangedListener(
            PredicateTextWatcher(
                binding.textInputPassword,
                Constants.passwordInputError,
                Predicate {
                    return@Predicate it.isEmpty() || it.length <= 4 || it.length >= 15
                })
        )

        binding.textInputConfirmPassword.addTextChangedListener(
            PredicateTextWatcher(
                binding.textInputConfirmPassword,
                Constants.confirmPasswordInputError,
                Predicate {
                    return@Predicate it.isEmpty() || it != viewModel.password.value
                })
        )

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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.register_appbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if(item.itemId == R.id.action_login) {
            navController.navigate(R.id.action_registerFragment_to_loginFragment)
        }

        return true
    }
}