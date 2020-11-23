package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthFragmentViewModel(application: Application)
    : StateIntentViewModel<TokenState, TokenIntent>(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {

    protected val tokenRepository: TokenRepository by instance(tag = "tokenRepository")

    // TODO: Extract tag functionality into interface+class delegate
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
        _selectedTags = tags
    }
}