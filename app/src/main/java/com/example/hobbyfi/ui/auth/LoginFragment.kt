package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.util.Predicate
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.hobbyfi.BuildConfig
import com.example.hobbyfi.R
import com.example.hobbyfi.databinding.FragmentLoginBinding
import com.example.hobbyfi.intents.FacebookIntent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.User
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.ui.base.BaseFragment
import com.example.hobbyfi.ui.base.TextFieldInputValidationOnus
import com.example.hobbyfi.utils.FieldUtils
import com.example.hobbyfi.utils.ImageUtils
import com.example.hobbyfi.viewmodels.auth.LoginFragmentViewModel
import com.example.spendidly.utils.PredicateTextWatcher
import com.facebook.*
import com.facebook.login.LoginResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class LoginFragment : BaseFragment(), TextFieldInputValidationOnus {

    companion object {
        fun newInstance() = LoginFragment()
    }

    private val viewModel: LoginFragmentViewModel by viewModels(factoryProducer = {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    })
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

        val view: View = binding.root

        binding.switchToRegisterSubtitle.setOnClickListener {
            navController.navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.loginButton.setOnClickListener {
            if(FieldUtils.isTextFieldInvalid(binding.textInputEmail) ||
                FieldUtils.isTextFieldInvalid(binding.textInputPassword)) {
                return@setOnClickListener
            }

            lifecycleScope.launch {
                viewModel.sendIntent(TokenIntent.FetchLoginToken)
            }
        }

        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        initFacebookLogin()

        lifecycleScope.launch {
            viewModel.state.collect {
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
                    is TokenState.OnTokenReceived -> {
                        prefConfig.writeToken(it.token?.jwt)
                        prefConfig.writeRefreshToken(it.token?.refreshJwt)
                        prefConfig.writeLoginStatus(true)

                        // TODO: Fix code repetition
                        val action = LoginFragmentDirections.actionLoginFragmentToMainActivity(
                            null
                        )
                        navController.navigate(action)
                    }
                    is TokenState.OnFacebookRegisterTokenSuccess -> {
                        prefConfig.writeLoginStatus(true)

                        val profile = Profile.getCurrentProfile()
                        val action = LoginFragmentDirections.actionLoginFragmentToMainActivity(
                            User(
                                Integer.parseInt(Profile.getCurrentProfile().id)
                                    .toLong(), // this will freaking die if Facebook changes their ID schema
                                viewModel.email.value,
                                profile.name,
                                null,
                                BuildConfig.BASE_URL + "uploads/" + Constants.userProfileImageDir + "/" + id + ".jpg", // FIXME: user PFP isn't in sync; fix in backend and client-side for future
                                viewModel.selectedTags,
                                null
                            )
                        )
                        navController.navigate(action)
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
                    is FacebookState.OnData -> {
                        when(it) {
                            is FacebookState.OnData.OnEmailReceived -> {
                                viewModel.email.value = it.email
                                lifecycleScope.launch {
                                    viewModel.sendFacebookIntent(FacebookIntent.FetchFacebookUserTags)
                                }
                            }
                            is FacebookState.OnData.OnTagsReceived -> { // if user cancels tags, just don't register them with tags
                                val action = LoginFragmentDirections.actionLoginFragmentToTagSelectionDialogFragment(
                                    viewModel.selectedTags.toTypedArray(),
                                    viewModel.tags
                                        .toTypedArray() + it.tags
                                )
                                navController.navigate(action)
                            }
                        }

                    }
                    is FacebookState.Error -> {
                        if(it.equals(Constants.FACEBOOK_TAGS_FAILED_EXCEPTION)) {
                            val action = LoginFragmentDirections.actionLoginFragmentToTagSelectionDialogFragment(
                                viewModel.selectedTags.toTypedArray(),
                                viewModel.tags
                                    .toTypedArray()
                            )
                            navController.navigate(action)
                        }
                        if(it.equals(Constants.FACEBOOK_EMAIL_FAILED_EXCEPTION)) {
                            // TODO: Invalidate access token
                            // TODO: Cancel login attempt
                        }
                    }
                }
            }
        }

        // FIXME: Possible shared manipulaton of "selectedTags" saveStateHandle from Register and Login fragment?
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)?.observe(viewLifecycleOwner) {
            viewModel.setSelectedTags(it)
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
                    return@Predicate it.isEmpty() || it.length >= 15
                })
        )
    }

    fun initFacebookLogin() {
        val shouldRegister: Boolean = AccessToken.getCurrentAccessToken() == null && Profile.getCurrentProfile() == null

        binding.facebookButton.setPermissions(listOf("email", "user_likes"))
        binding.facebookButton.fragment = this
        binding.facebookButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {
            override fun onSuccess(loginResult: LoginResult?) {
                // TODO: Should request for tags be intent or not since user doesn't know about it?

                if(shouldRegister) {
                    lifecycleScope.launch {
                        viewModel.sendFacebookIntent(FacebookIntent.FetchFacebookUserEmail)
                    }
                } else {
                    // TODO: repetition
                    prefConfig.writeLoginStatus(true)
                    val action = LoginFragmentDirections.actionLoginFragmentToMainActivity(
                        null
                    )
                    navController.navigate(action)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.login_appbar_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        if(item.itemId == R.id.action_register) {
            navController.navigate(R.id.action_loginFragment_to_registerFragment)
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}