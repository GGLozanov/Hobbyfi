package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthFragmentViewModel(application: Application) : StateIntentViewModel<TokenState, TokenIntent>(application), Observable {

    protected val tokenRepository: TokenRepository by instance(tag = "tokenRepository")

    protected val _tags: MutableList<Tag> = Constants.predefinedTags.toMutableList()
    val tags: List<Tag> get() = _tags
    protected var _selectedTags: List<Tag> = emptyList()
    // this livedata instance will have its value updated once the tag selection dialog fragment finishes its workTag
    val selectedTags: List<Tag> get() = _selectedTags

    @Bindable
    val email: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val password: MutableLiveData<String> = MutableLiveData()

    fun setSelectedTags(tags: List<Tag>) {
        this._selectedTags = tags
    }

    fun addTag(tag: Tag) {
        _tags.add(tag)
    }

    @delegate:Transient
    private val callBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.remove(callback)

}