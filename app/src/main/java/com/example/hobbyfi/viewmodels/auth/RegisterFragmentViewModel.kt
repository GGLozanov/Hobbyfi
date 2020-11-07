package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalCoroutinesApi
class RegisterFragmentViewModel(application: MultiDexApplication) : AuthFragmentViewModel(application), Observable {
    override val _state: MutableStateFlow<TokenState> = MutableStateFlow(TokenState.Idle)

    override fun handleIntent() {
        TODO("Not yet implemented")
    }

    @delegate:Transient
    private val callBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    // TODO: Check encapsulation principles for exposing MutableLiveData for two-way databinding
    @Bindable
    val email: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val password: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val username: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val description: MutableLiveData<String> = MutableLiveData()

    var tags: LiveData<List<Tag>>? = null
    // this livedata instance will be initialised once the tag selection dialog fragment finishes its workTag
    // and sends its livedata instance through the fragment-dialog listener, which will be set to this instance

    private fun fetchRegisterToken() {

    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.remove(callback)
}