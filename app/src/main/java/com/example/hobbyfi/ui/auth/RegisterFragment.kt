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
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.RegisterFragmentBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.viewmodels.auth.RegisterFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import kotlinx.android.synthetic.main.register_fragment.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class RegisterFragment : AuthFragment(), TextFieldInputValidationOnus {

    companion object {
        fun newInstance() = RegisterFragment()
    }

    private val viewModel: RegisterFragmentViewModel by viewModels()

    private var imageRequestCode: Int = 777
    private var bitmap: Bitmap? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: RegisterFragmentBinding =
            DataBindingUtil.inflate(inflater, R.layout.register_fragment, container, false)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        val view = binding.root

        initTextFieldValidators()

        profile_image.setOnClickListener {
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

        tag_select_button.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToTagSelectionDialogFragment(
                viewModel.selectedTags.value?.toTypedArray(),
                Constants.predefinedTags.toTypedArray()
            )
            navController.navigate(action)
        }

        register_account_button.setOnClickListener {
            if(FieldUtils.isTextFieldInvalid(text_input_email) ||
                FieldUtils.isTextFieldInvalid(text_input_password) ||
                FieldUtils.isTextFieldInvalid(text_input_username) ||
                    FieldUtils.isTextFieldInvalid(text_input_description)) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                viewModel.sendIntent(TokenIntent.FetchRegisterToken)
            }
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
                    is TokenState.OnTokenReceived -> {
                        prefConfig.writeToken(it.token?.jwt)
                        prefConfig.writeRefreshToken(it.token?.refreshJwt)
                        prefConfig.writeLoginStatus(true)

                        val action = RegisterFragmentDirections.actionRegisterFragmentToMainActivity(
                            User(
                                TokenUtils.getTokenUserIdFromPayload(it.token?.jwt).toLong(),
                                viewModel.email.value!!,
                                viewModel.username.value!!,
                                viewModel.description.value,
                                viewModel.getProfileImageBase64() != null,
                                viewModel.selectedTags.value,
                                null
                            )
                        )
                        navController.navigate(action)
                    }
                }
            }
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>("selectedTags")?.observe(viewLifecycleOwner) {
            viewModel.setSelectedTags(it)
        }

        return view
    }

    override fun initTextFieldValidators() {
        text_input_email.addTextChangedListener(
            PredicateTextWatcher(
                text_input_email,
                Constants.emailInputError,
                Predicate {
                    return@Predicate it.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(it).matches()
                })
        )

        text_input_password.addTextChangedListener(
            PredicateTextWatcher(
                text_input_password,
                Constants.passwordInputError,
                Predicate {
                    return@Predicate it.isEmpty() || it.length >= 15
                })
        )

        text_input_username.addTextChangedListener(
            PredicateTextWatcher(
                text_input_username,
                Constants.usernameInputError,
                Predicate {
                    return@Predicate it.isEmpty() || it.length >= 25
                })
        )

        text_input_description.addTextChangedListener(
            PredicateTextWatcher(
                text_input_description,
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
            profile_image.setImageBitmap(
                bitmap
            ) // set the new image resource to be decoded from the bitmap
            viewModel.setProfileImageBase64(
                ImageUtils.encodeImage(bitmap!!)
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.register_appbar_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if(item.itemId == R.id.action_login) {
            navController.navigate(R.id.action_registerFragment_to_loginFragment)
        }

        return true
    }
}