package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.FacebookIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.StateIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.state.FacebookState
import com.example.hobbyfi.state.TokenState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class LoginFragmentViewModel(application: Application) : AuthFragmentViewModel(application) {
    init {
        handleIntent()
    }

    private lateinit var facebookStateIntent: StateIntent<FacebookState, FacebookIntent>

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
                        fetchRegisterTokenFacebook(it.facebookToken, it.username, email.value, it.image,
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
    private suspend fun fetchRegisterTokenFacebook(facebookToken: String, username: String, email: String?, image: String, tags: List<Tag>) {
        mainStateIntent.setState(TokenState.Loading)
        mainStateIntent.setState(try {
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
        })
    }
}