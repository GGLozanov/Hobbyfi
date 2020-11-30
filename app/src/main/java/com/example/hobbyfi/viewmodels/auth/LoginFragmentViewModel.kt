package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.FacebookIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.TokenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class LoginFragmentViewModel(application: Application) : AuthFragmentViewModel(application) {
    init {
        handleIntent()
    }

    private lateinit var _facebookState: MutableStateFlow<FacebookState>
    val facebookState: StateFlow<FacebookState> get() = _facebookState

    private lateinit var facebookIntent: Channel<FacebookIntent>

    override fun handleIntent() {
        viewModelScope.launch {
            facebookIntent = Channel(Channel.UNLIMITED)
            _facebookState = MutableStateFlow(FacebookState.Idle) // has to be init-ed here for some reason; bruh

            facebookIntent.consumeAsFlow().collect {
                when(it) {
                    is FacebookIntent.FetchFacebookUserTags -> {
                        fetchFacebookTags()
                    }
                    is FacebookIntent.FetchFacebookUserEmail -> {
                        fetchFacebookEmail()
                    }
                    is FacebookIntent.ValidateFacebookUserExistence -> {
                        fetchUserExistence(it.username)
                    }
                }
            }
        }
        viewModelScope.launch {
            mainIntent.consumeAsFlow().collect {
                when(it) {
                    is TokenIntent.FetchLoginToken -> {
                        fetchLoginToken()
                    }
                    is TokenIntent.FetchFacebookRegisterToken -> {
                        Log.i("LoginFragmentVM", "fetching FB register token")
                        fetchRegisterTokenFacebook(it.facebookToken, it.username, email.value, it.image,
                            selectedTags) // fixme: repeated unnecessary checks
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    suspend fun sendFacebookIntent(i: FacebookIntent) {
        facebookIntent.send(i)
    }

    private suspend fun fetchUserExistence(username: String) {
        _facebookState.value = FacebookState.Loading
        _facebookState.value = try {
            FacebookState.OnData.ExistenceResultReceived(
                tokenRepository.getUserExistence(username)
            )
        } catch(ex: Exception) {
            FacebookState.Error(ex.message)
        }
    }

    private suspend fun fetchFacebookTags() {
        _facebookState.value = FacebookState.Loading
        _facebookState.value = try {
            FacebookState.OnData.TagsReceived(
                tokenRepository.getFacebookUserPageTitlesAsTags()
            )
        } catch(ex: Exception) {
            FacebookState.Error(ex.message)
        }
    }

    private suspend fun fetchFacebookEmail() {
        _facebookState.value = FacebookState.Loading
        _facebookState.value = try {
            FacebookState.OnData.EmailReceived(
                tokenRepository.getFacebookUserEmail()
            )
        } catch(ex: Exception) {
            FacebookState.Error(ex.message)
        }
    }

    // TODO: Save facebook user picture on own back-end? Useless but easier?
    // ...if the Facebook user changes their profile picture, it won't be synced in the app...
    // Too bad!
    private suspend fun fetchRegisterTokenFacebook(facebookToken: String, username: String, email: String?, image: String, tags: List<Tag>) {
        _mainState.value = TokenState.Loading
        _mainState.value = try {
            tokenRepository.getRegisterToken(
                facebookToken,
                email,
                null,
                username,
                null,
                image,
                tags
            )
            TokenState.FacebookRegisterTokenSuccess
        } catch(ex: Exception) {
            ex.printStackTrace()
            TokenState.Error(ex.message)
        }
    }
}