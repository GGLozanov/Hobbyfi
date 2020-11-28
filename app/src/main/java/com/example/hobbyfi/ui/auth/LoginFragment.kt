package com.example.hobbyfi.ui.auth

import android.content.Intent
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
import com.example.spendidly.utils.PredicateTextWatcher
import com.facebook.*
import com.facebook.login.LoginResult
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
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

            switchToRegisterSubtitle.setOnClickListener {
                navController.navigate(R.id.action_loginFragment_to_registerFragment)
            }

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

        lifecycleScope.launch {
            viewModel.mainState.collect {
                when(it) {
                    is TokenState.Idle -> {

                    }
                    is TokenState.Loading -> {
                        // TODO: Progressbar
                    }
                    is TokenState.Error -> {
                        Toast.makeText(context, it.error, Toast.LENGTH_LONG)
                            .show()
                    }
                    is TokenState.TokenReceived -> {
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
                                Integer.parseInt(Profile.getCurrentProfile().id)
                                    .toLong(), // this will freaking die if Facebook changes their ID schema
                                viewModel.email.value,
                                profile.name,
                                null,
                                BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir + "/" + id + ".jpg", // FIXME: user PFP isn't in sync; fix in backend and client-side for future
                                viewModel.selectedTags,
                                null
                            ), true)
                        )
                    }
                }
            }

            viewModel.facebookState.collect {
                when(it) {
                    is FacebookState.Idle -> {

                    }
                    is FacebookState.Loading -> {
                        // TODO: Progressbar
                    }
                    is FacebookState.OnData.EmailReceived -> {
                        viewModel.email.value = it.email
                        lifecycleScope.launch {
                            viewModel.sendFacebookIntent(FacebookIntent.FetchFacebookUserTags)
                        }
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

        // FIXME: Possible shared manipulaton of "selectedTags" saveStateHandle from Register and Login fragment?
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)?.observe(viewLifecycleOwner) {
            viewModel.setSelectedTags(it)
            Log.i("SavedStateHandle LogFr", "Reached Facebook SavedStateHandle w/ tags $it")
            lifecycleScope.launch {
                val profile = Profile.getCurrentProfile()
                val image = ImageUtils.encodeImage(
                    ImageUtils.getBitmapFromUri(requireActivity(), profile.getProfilePictureUri(
                        Constants.profileImageWidth, Constants.profileImageHeight))
                ) // FIXME: Might not be correct sizes

                viewModel.sendIntent(
                    TokenIntent.FetchFacebookRegisterToken(
                        AccessToken.getCurrentAccessToken().token,
                        profile.name,
                        image,
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
        val shouldRegister: Boolean = AccessToken.getCurrentAccessToken() == null && Profile.getCurrentProfile() == null

        with(binding.facebookButton) {
            setPermissions(listOf("email", "user_likes"))
            fragment = this@LoginFragment
            registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {
                override fun onSuccess(loginResult: LoginResult?) {
                    // TODO: Should request for tags be intent or not since user doesn't know about it?
                    Log.i("LoginFragment", "Triggered w/ loging result $loginResult")

                    if(shouldRegister) {
                        lifecycleScope.launch {
                            viewModel.sendFacebookIntent(FacebookIntent.FetchFacebookUserEmail)
                        }
                    } else {
                        // TODO: repetition
                        login(
                            LoginFragmentDirections.actionLoginFragmentToMainActivity(null)
                        )
                    }
                }

                override fun onCancel() {
                    Toast.makeText(context, "Facebook login cancelled!", Toast.LENGTH_LONG)
                        .show()
                }

                override fun onError(exception: FacebookException) {
                    Toast.makeText(context, "Facebook login error!", Toast.LENGTH_LONG)
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