package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.onNavDestinationSelected
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
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
import com.example.hobbyfi.utils.TokenUtils
import com.example.hobbyfi.viewmodels.auth.LoginFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import com.facebook.*
import com.facebook.login.LoginResult
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance


@ExperimentalCoroutinesApi
class LoginFragment : AuthFragment(), TextFieldInputValidationOnus {

    companion object {
        val tag: String = "LoginFragment"
        fun newInstance() = LoginFragment()
    }

    private val viewModel: LoginFragmentViewModel by viewModels()
    private lateinit var binding: FragmentLoginBinding

    private val callbackManager: CallbackManager = CallbackManager.Factory.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

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
                        if(it.exists) {
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
                            viewModel.selectedTags.toTypedArray(),
                            viewModel.tags
                                .toTypedArray() + it.tags
                        )
                        navController.navigate(action)
                    }
                    is FacebookState.Error -> {
                        Toast.makeText(requireContext(), it.error, Toast.LENGTH_LONG)
                            .show()

                        if(it.error != Constants.noConnectionError) {
                            // TODO: No critical errors as of yet, so we can navigate to tags even if failed, but if the need arises, handle critical failure and cancel login
                            val action = LoginFragmentDirections.actionLoginFragmentToTagNavGraph(
                                viewModel.selectedTags.toTypedArray(),
                                viewModel.tags
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
                        when(it.error) {
                            Constants.missingDataError -> {
                                Log.wtf(LoginFragment.tag, "Should never reach here if everything is ok, wtf")
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
                            LoginFragmentDirections.actionLoginFragmentToMainActivity(User(
                                profile.id.toLong(), // this will freaking die if Facebook changes their ID schema
                                viewModel.email.value,
                                profile.name,
                                null,
                                BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir + "/" + id + ".jpg", // FIXME: user PFP isn't in sync; fix in backend and client-side for future
                                viewModel.selectedTags,
                                null
                            ))
                        )
                    }
                }
            }
        }

        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)?.observe(viewLifecycleOwner) {
            viewModel.setSelectedTags(it)
            Log.i("SavedStateHandle LogFr", "Reached Facebook SavedStateHandle w/ tags $it")
            lifecycleScope.launch {
                val profile = Profile.getCurrentProfile()
                val loader = ImageLoader(requireContext())
                val request: ImageRequest = ImageRequest.Builder(requireContext())
                    .data(profile.getProfilePictureUri(Constants.profileImageWidth, Constants.profileImageHeight))
                    .build()
                val image = ImageUtils.encodeImage(
                    (loader.execute(request) as SuccessResult)
                        .drawable.toBitmap(Constants.profileImageWidth, Constants.profileImageHeight)
                ) // FIXME: Might not be correct sizes

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
            textInputEmail.addTextChangedListener(
                Constants.emailInputError,
                Constants.emailPredicate
            )

            textInputPassword.addTextChangedListener(
                Constants.passwordInputError,
                Constants.passwordPredicate
            )
        }
    }

    override fun assertTextFieldsInvalidity(): Boolean {
        with(binding) {
            return@assertTextFieldsInvalidity FieldUtils.isTextFieldInvalid(textInputEmail) ||
                    FieldUtils.isTextFieldInvalid(textInputPassword)
        }
    }

    private fun initFacebookLogin() {
        with(binding.facebookButton) {
            setPermissions(listOf("email", "user_likes"))
            fragment = this@LoginFragment
            registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    Log.i("LoginFragment", "Triggered w/ user profile name ${Profile.getCurrentProfile().name}")

                    lifecycleScope.launch {
                        viewModel.sendFacebookIntent(FacebookIntent.ValidateFacebookUserExistence(
                            Profile.getCurrentProfile().id.toLong()
                        ))
                    }
                }

                override fun onCancel() {
                    Toast.makeText(context, "Facebook login cancelled!", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onError(exception: FacebookException) {
                    Toast.makeText(context, "Facebook login error! ${exception.message}", Toast.LENGTH_LONG)
                        .show()
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