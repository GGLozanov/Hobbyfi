package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.FacebookIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.data.StateIntent
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.TagBundle
import com.example.hobbyfi.shared.invalidateBy
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.AuthInclusiveViewModel
import com.example.hobbyfi.viewmodels.base.TagBundleHolder
import com.example.hobbyfi.viewmodels.base.TagBundleHolderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class LoginFragmentViewModel(application: Application) : AuthInclusiveViewModel(application),
        TagBundleHolder by TagBundleHolderViewModel() {
    init {
        handleIntent()
    }

    private var _sentFbTokenFetch = false
    fun setSentFbTokenFetch(fetch: Boolean) {
        _sentFbTokenFetch = fetch
    }
    val sentFbTokenFetch get() = _sentFbTokenFetch

    private lateinit var facebookStateIntent: StateIntent<FacebookState, FacebookIntent>

    fun resetFacebookState() = facebookStateIntent.setState(FacebookState.Idle)

    val facebookState get() = facebookStateIntent.state

    override fun handleIntent() {
        // different channels consumed as flows must be collected in different coroutine jobs
        viewModelScope.launch {
            facebookStateIntent = object : StateIntent<FacebookState, FacebookIntent>() {
                override val _state: MutableStateFlow<FacebookState> = MutableStateFlow(FacebookState.Idle)
            } // has to be init-ed here because this abstract method gets called/initialised before the intent somehow???

            facebookStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is FacebookIntent.FetchFacebookUserTags -> {
                        fetchFacebookTags()
                    }
                    is FacebookIntent.FetchFacebookUserEmail -> {
                        fetchFacebookEmail()
                    }
                    is FacebookIntent.ValidateFacebookUserExistence -> {
                        fetchUserExistence(it.id)
                    }
                }
            }
        }
        viewModelScope.launch {
            mainStateIntent.intentAsFlow().collect {
                when(it) {
                    is TokenIntent.FetchLoginToken -> {
                        fetchLoginToken()
                    }
                    is TokenIntent.FetchFacebookRegisterToken -> {
                        Log.i("LoginFragmentVM", "fetching FB register token")
                        fetchRegisterTokenFacebook(it.facebookToken, it.username, email.value,
                            tagBundle.selectedTags) // fixme: repeated unnecessary checks
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    suspend fun sendFacebookIntent(i: FacebookIntent) {
        facebookStateIntent.sendIntent(i)
    }

    private suspend fun fetchUserExistence(id: Long) {
        facebookStateIntent.setState(FacebookState.Loading)
        facebookStateIntent.setState(try {
            FacebookState.OnData.ExistenceResultReceived(
                tokenRepository.getUserExistence(id)
            )
        } catch(ex: Exception) {
            FacebookState.Error(ex.message)
        })
    }

    private suspend fun fetchFacebookTags() {
        facebookStateIntent.setState(FacebookState.Loading)
        facebookStateIntent.setState(try {
            FacebookState.OnData.TagsReceived(
                tokenRepository.getFacebookUserPageTitlesAsTags()
            )
        } catch(ex: Exception) {
            FacebookState.Error(ex.message)
        })
    }

    private suspend fun fetchFacebookEmail() {
        facebookStateIntent.setState(FacebookState.Loading)
        facebookStateIntent.setState(try {
            FacebookState.OnData.EmailReceived(
                tokenRepository.getFacebookUserEmail()
            )
        } catch(ex: Exception) {
            FacebookState.Error(ex.message)
        })
    }

    // TODO: Save facebook user picture on own back-end? Useless but easier?
    // ...if the Facebook user changes their profile picture, it won't be synced in the app...
    // Too bad!
    private suspend fun fetchRegisterTokenFacebook(facebookToken: String, username: String, email: String?, tags: List<Tag>) {
        mainStateIntent.setState(TokenState.Loading)
        mainStateIntent.setState(try {
            tokenRepository.getRegisterToken(
                facebookToken,
                email,
                null,
                username,
                null,
                tags
            )
            TokenState.FacebookRegisterTokenSuccess
        } catch(ex: Exception) {
            ex.printStackTrace()
            TokenState.Error(ex.message)
        })
    }

    override val combinedObserversInvalidity: LiveData<Boolean> get() = invalidateBy(
        password.invalidity,
        email.invalidity
    )
}