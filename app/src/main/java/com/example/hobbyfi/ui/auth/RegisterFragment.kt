package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.RegisterFragmentBinding
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.shared.Callbacks
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.viewmodels.auth.RegisterFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import kotlinx.android.synthetic.main.register_fragment.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class RegisterFragment : AuthFragment() {

    companion object {
        fun newInstance() = RegisterFragment()
    }

    private val viewModel: RegisterFragmentViewModel by viewModels()

    private var imageRequestCode: Int = 777
    private var bitmap: Bitmap? = null

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

                    }
                    is TokenState.Loading -> {

                    }
                    is TokenState.OnTokenReceived -> {

                    }
                }
            }
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
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.register_appbar_menu, menu)
    }
}