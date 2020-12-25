package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentLoginBinding
import com.example.hobbyfi.intents.FacebookIntent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addTextChangedListener
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.auth.LoginFragmentViewModel
import com.facebook.*
import com.facebook.login.LoginResult
import com.squareup.okhttp.Dispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest


@ExperimentalCoroutinesApi
class LoginFragment : AuthFragment(), TextFieldInputValidationOnus {

    companion object {
        val tag: String = "LoginFragment"
        fun newInstance() = LoginFragment()
    }

    private val viewModel: LoginFragmentViewModel by viewModels()
    private lateinit var binding: FragmentLoginBinding

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        initTextFieldValidators()

        binding.viewModel = viewModel
        with(binding) {
            lifecycleOwner = this@LoginFragment

            val view: View = root

            loginButton.setOnClickListener {
                if(assertTextFieldsInvalidity()) {
                    return@setOnClickListener
                }

                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.FetchLoginToken)
                }
            }

            return@onCreateView view
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initFacebookLogin()

        binding.switchToRegisterSubtitle.setOnClickListener {
            navController.navigate(R.id.action_loginFragment_to_registerFragment)
        }

        lifecycleScope.launch {
            viewModel.facebookState.collectLatest {
                when(it) {
                    is FacebookState.Idle -> {

                    }
                    is FacebookState.Loading -> {
                        // TODO: Progressbar
                        Log.i("LoginFragment", "Facebook state loading")
                    }
                    is FacebookState.OnData.ExistenceResultReceived -> {
                        if (it.exists) {
                            login(
                                LoginFragmentDirections.actionLoginFragmentToMainActivity(
                                    null,
                                )
                            )
                        } else {
                            viewModel.sendFacebookIntent(FacebookIntent.FetchFacebookUserEmail)
                        }
                    }
                    is FacebookState.OnData.EmailReceived -> {
                        Log.i("LoginFragment", "Email received ${it.email}")
                        viewModel.email.value = it.email
                        viewModel.sendFacebookIntent(FacebookIntent.FetchFacebookUserTags)
                    }
                    is FacebookState.OnData.TagsReceived -> { // if user cancels tags, just don't register them with tags
                        val action = LoginFragmentDirections.actionLoginFragmentToTagNavGraph(
                            viewModel.tagBundle.selectedTags.toTypedArray(),
                            viewModel.tagBundle.tags
                                .toTypedArray() + it.tags
                        )
                        navController.navigate(action)
                    }
                    is FacebookState.Error -> {
                        Toast.makeText(requireContext(), it.error, Toast.LENGTH_LONG)
                            .show()

                        if (it.error != Constants.noConnectionError) {
                            // TODO: No critical errors as of yet, so we can navigate to tags even if failed, but if the need arises, handle critical failure and cancel login
                            val action = LoginFragmentDirections.actionLoginFragmentToTagNavGraph(
                                viewModel.tagBundle.selectedTags.toTypedArray(),
                                viewModel.tagBundle.tags
                                    .toTypedArray()
                            )
                            navController.navigate(action)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.mainState.collect {
                when(it) {
                    is TokenState.Idle -> {

                    }
                    is TokenState.Loading -> {
                        // TODO: Progressbar
                    }
                    is TokenState.Error -> {
                        // TODO BIG: Extract into exceptions and change states to receive exceptions, not string texts so that said exceptions can be easily when()'d
                        when (it.error) {
                            Constants.missingDataError -> {
                                Log.wtf(
                                    LoginFragment.tag,
                                    "Should never reach here if everything is ok, wtf"
                                )
                                throw RuntimeException()
                            }
                            else -> {
                                Toast.makeText(context, it.error, Toast.LENGTH_LONG)
                                    .show() // means we've simply entered incorrect info for the normal login or something else is wrong
                            }
                        }
                    }
                    is TokenState.TokenReceived -> {
                        Log.i("LoginFragment", "${navController.currentBackStackEntry}")
                        login(
                            LoginFragmentDirections.actionLoginFragmentToMainActivity(
                                null
                            ),
                            it.token?.jwt,
                            it.token?.refreshJwt,
                        )
                    }
                    is TokenState.FacebookRegisterTokenSuccess -> {
                        val profile = Profile.getCurrentProfile()
                        login(
                            LoginFragmentDirections.actionLoginFragmentToMainActivity(
                                User(
                                    profile.id.toLong(), // this will freaking die if Facebook changes their ID schema
                                    viewModel.email.value,
                                    profile.name,
                                    null,
                                    BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir + "/" + profile.id + ".jpg", // FIXME: user PFP isn't in sync; fix in backend and client-side for future
                                    viewModel.tagBundle.selectedTags,
                                    null
                                )
                            )
                        )
                    }
                }
            }
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)?.observe(
            viewLifecycleOwner
        ) {
            viewModel.tagBundle.setSelectedTags(it)
            Log.i("SavedStateHandle LogFr", "Reached Facebook SavedStateHandle w/ tags $it")
            lifecycleScope.launch {
                val profile = Profile.getCurrentProfile()
                val bitmap = suspendCancellableCoroutine<Bitmap> { continuation ->
                    val glide = Glide.with(this@LoginFragment)

                    var bmapResource: Bitmap? = null

                    val target = object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            bmapResource = resource
                            continuation.resume(resource, null)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            bmapResource?.recycle()
                            continuation.cancel(Constants.ImageFetchException())
                        }
                    }

                    glide
                        .asBitmap()
                        .load(
                            profile.getProfilePictureUri(
                                Constants.profileImageWidth,
                                Constants.profileImageHeight
                            )
                        ).into(target)

                    continuation.invokeOnCancellation {
                        glide.clear(target)
                    }
                }

                val image = ImageUtils.encodeImage(
                    bitmap
                )
                viewModel.sendIntent(
                    TokenIntent.FetchFacebookRegisterToken(
                        AccessToken.getCurrentAccessToken().token,
                        profile.name,
                        image
                    )
                )
            }
        }
    }

    override fun initTextFieldValidators() {
        with(binding) {
            emailInputField.addTextChangedListener(
                Constants.emailInputError,
                Constants.emailPredicate
            )

            passwordInputField.addTextChangedListener(
                Constants.passwordInputError,
                Constants.passwordPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(
                emailInputField,
                Constants.emailInputError) ||
                    FieldUtils.isTextFieldInvalid(passwordInputField, Constants.passwordInputError)
        }
    }

    private fun initFacebookLogin() {
        with(binding.facebookButton) {
            setPermissions(listOf("email", "user_likes"))
            fragment = this@LoginFragment
            registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {
                private fun validateProfileExistence() {
                    lifecycleScope.launch {
                        Log.i("ID", Profile.getCurrentProfile().id)
                        viewModel.sendFacebookIntent(
                            FacebookIntent.ValidateFacebookUserExistence(
                                Profile.getCurrentProfile().id.toLong()
                            )
                        )
                    }
                }

                override fun onSuccess(loginResult: LoginResult?) {
                    if(Profile.getCurrentProfile() == null) {
                        object : ProfileTracker() {
                            override fun onCurrentProfileChanged(
                                oldProfile: Profile?,
                                profile: Profile
                            ) {
                                stopTracking()
                                Profile.setCurrentProfile(profile)
                                validateProfileExistence()
                            }
                        }
                    } else {
                        validateProfileExistence()
                    }
                }

                override fun onCancel() {
                    Toast.makeText(context, "Facebook login cancelled!", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onError(exception: FacebookException) {
                    Toast.makeText(
                        context,
                        "Facebook login error! ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_appbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}