package com.example.hobbyfi.viewmodels.auth

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.intents.TokenIntent
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.appendNewSelectedTagsToTags
import com.example.hobbyfi.state.TokenState
import com.example.hobbyfi.viewmodels.base.AuthInclusiveViewModel
import com.example.hobbyfi.viewmodels.base.StateIntentViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.generic.instance

@ExperimentalCoroutinesApi
abstract class AuthFragmentViewModel(application: Application)
    : AuthInclusiveViewModel(application) {

    protected val _tags: MutableList<Tag> = Constants.predefinedTags.toMutableList()
    val tags: List<Tag> get() = _tags
    protected var _selectedTags: List<Tag> = emptyList()
    // this livedata instance will have its value updated once the tag selection dialog fragment finishes its workTag
    val selectedTags: List<Tag> get() = _selectedTags

    fun setSelectedTags(tags: List<Tag>) {
        _selectedTags = tags
    }

    fun appendNewSelectedTagsToTags(selectedTags: List<Tag>) {
        _tags.appendNewSelectedTagsToTags(selectedTags)
    }
}