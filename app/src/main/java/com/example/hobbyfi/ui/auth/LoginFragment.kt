package com.example.hobbyfi.ui.auth

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
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
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.State
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.utils.WorkerUtils
import com.example.hobbyfi.viewmodels.auth.LoginFragmentViewModel
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.generic.instance
import java.io.File


@ExperimentalCoroutinesApi
class LoginFragment : AuthFragment() {

    companion object {
        val tag: String = "LoginFragment"
    }

    private val viewModel: LoginFragmentViewModel by viewModels()
    private lateinit var binding: FragmentLoginBinding

    private val callbackManager: CallbackManager by instance(tag = "callbackManager")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_login, container, false)

        binding.viewModel = viewModel
        observeCombinedObserversInvalidity()

        with(binding) {
            lifecycleOwner = this@LoginFragment

            val root: View = root

            loginButton.setOnClickListener {
                lifecycleScope.launch {
                    viewModel!!.sendIntent(TokenIntent.FetchLoginToken)
                }
            }

            return@onCreateView root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initFacebookLogin()

        with(binding) {
            resetPasswordSubtitle.setOnClickListener {
                navController.navigate(R.id.action_loginFragment_to_resetPasswordFragment)
            }
        }

        observeFacebookState()
        observeTokenState()
        observePotentialTags()
    }

    override fun observePredicateValidators() {
        with(viewModel) {
            password.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.passwordInputField, Constants.passwordInputError)
            )

            email.invalidity.observe(
                viewLifecycleOwner,
                TextInputLayoutFocusValidatorObserver(binding.emailInputField, Constants.emailInputError)
            )
        }
    }

    override fun observeCombinedObserversInvalidity() {
        viewModel.combinedObserversInvalidity.observe(viewLifecycleOwner, ViewReverseEnablerObserver(binding.loginButton))
    }

    private fun initFacebookLogin() {
        with(binding.facebookButton) {
            setPermissions(listOf("email", "user_likes"))
            fragment = this@LoginFragment
            registerCallback(callbackManager, object : FacebookCallback<LoginResult?> {
                private fun validateProfileExistence() {
                    lifecycleScope.launch {
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

    private fun observeFacebookState() {
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
                        it.email?.let { it1 -> viewModel.email.setValue(it1) }
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

                        // don't log out if only the email couldn't have been fetched
                        if(!it.error.equals(Constants.FACEBOOK_EMAIL_FAILED_EXCEPTION)) {
                            LoginManager.getInstance().logOut()
                        }

                        if(connectivityManager.isConnected()) {
                            if (it.error != Constants.serverConnectionError) {
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
        }
    }

    private fun observeTokenState() {
        lifecycleScope.launch {
            viewModel.mainState.collectLatest {
                when(it) {
                    is TokenState.Idle -> {

                    }
                    is TokenState.Loading -> {
                        // TODO: Progressbar
                    }
                    is TokenState.Error -> {
                        // TODO BIG: Extract into exceptions and change states to receive exceptions, not string texts so that said exceptions can be easily when()'d
                        LoginManager.getInstance().logOut()
                        when (it.error) {
                            Constants.missingDataError -> {
                                Log.wtf(
                                    LoginFragment.tag,
                                    "Should never reach here if everything is ok, wtf"
                                )
                            }
                            else -> {
                                Toast.makeText(context, it.error, Toast.LENGTH_LONG)
                                    .show() // means we've simply entered incorrect info for the normal login or something else is wrong
                            }
                        }
                    }
                    is TokenState.TokenReceived -> {
                        Log.i("LoginFragment", "${navController.currentBackStackEntry}")
                        Callbacks.hideKeyboardFrom(requireContext(), requireView())
                        login(
                            LoginFragmentDirections.actionLoginFragmentToMainActivity(
                                null
                            ),
                            it.token?.jwt,
                            it.token?.refreshJwt,
                        )
                        viewModel.resetTokenState()
                    }
                    is TokenState.FacebookRegisterTokenSuccess -> {
                        val profile = Profile.getCurrentProfile()
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                val file = suspendCancellableCoroutine<File> { continuation ->
                                    val glide = Glide.with(this@LoginFragment)

                                    var fileResource: File? = null
                                    val target = object : CustomTarget<File>() {
                                        override fun onResourceReady(
                                            resource: File,
                                            transition: Transition<in File>?
                                        ) {
                                            fileResource = resource
                                            continuation.resume(resource, null)
                                        }

                                        override fun onLoadCleared(placeholder: Drawable?) {
                                            continuation.cancel(Constants.ImageFetchException())
                                        }
                                    }

                                    glide
                                        .download(
                                            profile.getProfilePictureUri(
                                                Constants.profileImageWidth,
                                                Constants.profileImageHeight
                                            )
                                        ).into(target)

                                    continuation.invokeOnCancellation {
                                        glide.clear(target)
                                    }
                                }

                                WorkerUtils.buildAndEnqueueImageUploadWorker(
                                    profile.id.toLong(),
                                    AccessToken.getCurrentAccessToken().token,
                                    Constants.USERS,
                                    file.toURI().toString(),
                                    requireContext(),
                                    R.string.pref_last_user_fetch_time
                                )

                                withContext(Dispatchers.Main) {
                                    login(
                                        LoginFragmentDirections.actionLoginFragmentToMainActivity(
                                            User(
                                                profile.id.toLong(), // this will freaking die if Facebook changes their ID schema
                                                viewModel.email.value,
                                                profile.name,
                                                null,
                                                BuildConfig.BASE_URL + "uploads/" +
                                                        Constants.userProfileImageDir + "/" + profile.id + ".jpg", // FIXME: user PFP isn't in sync; fix in backend and client-side for future
                                                viewModel.tagBundle.selectedTags,
                                                null,
                                                null
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }
                    else -> throw State.InvalidStateException()
                }
            }
        }
    }

    private fun observePotentialTags() {
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<List<Tag>>(Constants.selectedTagsKey)?.observe(
            viewLifecycleOwner
        ) {
            viewModel.tagBundle.setSelectedTags(it)
            Log.i("SavedStateHandle LogFr", "Reached Facebook SavedStateHandle w/ tags $it")
            lifecycleScope.launch {
                Profile.getCurrentProfile()?.let { profile ->
                    viewModel.sendIntent(
                        TokenIntent.FetchFacebookRegisterToken(
                            AccessToken.getCurrentAccessToken().token,
                            profile.name,
                        )
                    )
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}