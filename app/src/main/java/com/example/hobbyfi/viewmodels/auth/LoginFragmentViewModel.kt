package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.FacebookIntent
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

    override val _state: MutableStateFlow<TokenState>
            = MutableStateFlow(TokenState.Idle)

    private val _facebookState: MutableStateFlow<FacebookState>
            = MutableStateFlow(FacebookState.Idle)
    val facebookState: StateFlow<FacebookState> get() = _facebookState

    private val facebookIntent: Channel<FacebookIntent> = Channel(Channel.UNLIMITED)

    override fun handleIntent() {
        viewModelScope.launch {
            intent.consumeAsFlow().collect {
                when(it) {
                    is TokenIntent.FetchLoginToken -> {
                        fetchLoginToken()
                    }
                    is TokenIntent.FetchFacebookRegisterToken -> {
                        fetchRegisterTokenFacebook(it.facebookToken, it.username, email.value, it.image,
                            selectedTags) // fixme: repeated unnecessary checks
                    }
                }
            }
            facebookIntent.consumeAsFlow().collect {
                when(it) {
                    is FacebookIntent.FetchFacebookUserTags -> {
                        fetchFacebookTags()
                    }
                    is FacebookIntent.FetchFacebookUserEmail -> {
                        fetchFacebookEmail()
                    }
                }
            }
        }
    }

    suspend fun sendFacebookIntent(i: FacebookIntent) {
        facebookIntent.send(i)
    }

    private fun fetchLoginToken() {
        viewModelScope.launch {
            _state.value = TokenState.Loading
            _state.value = try {
                TokenState.OnTokenReceived(tokenRepository.getLoginToken(
                    email.value!!,
                    password.value!!
                ))
            } catch(ex: Exception) {
                ex.printStackTrace()
                TokenState.Error(ex.localizedMessage)
            }
        }
    }

    private fun fetchFacebookTags() {
        viewModelScope.launch {
            _facebookState.value = FacebookState.Loading
            _facebookState.value = try {
                FacebookState.OnData.OnTagsReceived(
                    tokenRepository.getFacebookUserPageTitlesAsTags()
                )
            } catch(ex: Exception) {
                FacebookState.Error(ex.localizedMessage)
            }
        }
    }

    private fun fetchFacebookEmail() {
        viewModelScope.launch {
            _facebookState.value = FacebookState.Loading
            _facebookState.value = try {
                FacebookState.OnData.OnEmailReceived(
                    tokenRepository.getFacebookUserEmail()
                )
            } catch(ex: Exception) {
                FacebookState.Error(ex.localizedMessage)
            }
        }
    }

    // TODO: Save facebook user picture on own back-end? Useless but easier?
    // ...if the Facebook user changes their profile picture, it won't be synced in the app...
    // Too bad!
    private fun fetchRegisterTokenFacebook(facebookToken: String, username: String, email: String?, image: String, tags: List<Tag>) {
        viewModelScope.launch {
            _state.value = TokenState.Loading
            _state.value = try {
                tokenRepository.getRegisterToken(
                    facebookToken,
                    email,
                    null,
                    username,
                    null,
                    image,
                    tags
                )
                TokenState.OnFacebookRegisterTokenSuccess
            } catch(ex: Exception) {
                ex.printStackTrace()
                TokenState.Error(ex.message)
            }
        }
    }
}