package com.example.hobbyfi.viewmodels.main

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.UserIntent
import com.example.hobbyfi.models.User
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.state.UserState
import com.example.hobbyfi.viewmodels.base.AuthUserHolderViewModel
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
class MainActivityViewModel(
    application: Application,
    user: User?
) : AuthUserHolderViewModel(application, user) {
    private val _joinedChatroom: MutableLiveData<Boolean> = MutableLiveData(false)
    val joinedChatroom: LiveData<Boolean> get() = _joinedChatroom
    private val _leftChatroom: MutableLiveData<Boolean> = MutableLiveData(false)
    val leftChatroom: LiveData<Boolean> get() = _leftChatroom
    private var latestUserUpdateFields: Map<String?, String?>? = null
    
    private val _isUserProfileUpdateButtonEnabled: MutableLiveData<Boolean> = MutableLiveData(true)
    val isUserProfileUpdateButtonEnabled: LiveData<Boolean> get() = _isUserProfileUpdateButtonEnabled

    private var _deepLinkExtras: Bundle? = null
    val deepLinkExtras: Bundle? get() = _deepLinkExtras
    fun setDeepLinkExtras(b: Bundle?) {
        _deepLinkExtras = b
    }

    fun resetState() {
        mainStateIntent.setState(UserState.Idle)
    }

    init {
        if(user != null) {
            Log.i("AuthUserHolderVM", "Saving auth user instance")
            viewModelScope.launch {
                userRepository.saveUser(user)
            }
        }

        handleIntent()
    }

    fun setJoinedChatroom(joinedCatroom: Boolean) {
        _joinedChatroom.value = joinedCatroom
    }

    fun setLeftChatroom(leftChatroom: Boolean) {
        _leftChatroom.value = leftChatroom
    }

    fun setLatestUserUpdateFields(updateFields: Map<String?, String?>?) {
        latestUserUpdateFields = updateFields
    }

    fun updateUserWithLatestFields() {
        if(latestUserUpdateFields != null) {
            viewModelScope.launch {
                updateAndSaveUser(latestUserUpdateFields!!)
            }
        } else {
            Log.wtf("MainActivityVM", "Called updateUserWithLatestFields incorrectly!")
        }
    }
    
    fun setIsUserProfileUpdateButtonEnabled(en: Boolean) {
        _isUserProfileUpdateButtonEnabled.value = en
    }
}