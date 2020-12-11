package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.appendNewSelectedTagsToTags
import com.example.hobbyfi.shared.getNewSelectedTagsWithTags
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.generic.instance

// TODO: Fix code dup with AuthFragmentViewModel through fake mixins or something
@ExperimentalCoroutinesApi
class UserProfileFragmentViewModel(application: Application, initialTags: List<Tag>) : BaseViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {
    // FIXME: Code dup with AuthFragmentViewModel >:(
    private val _tags: MutableList<Tag> = Constants.predefinedTags.getNewSelectedTagsWithTags(initialTags)
    val tags: List<Tag> get() = _tags

    private var _selectedTags: List<Tag> = initialTags
    // this list instance will have its value updated once the tag selection dialog fragment finishes its workTag
    val selectedTags: List<Tag> get() = _selectedTags

    private var _base64Image: String? = null
    val base64Image get() = _base64Image

    fun setProfileImageBase64(base64Image: String) {
        _base64Image = base64Image
    }

    // FIXME: Code dup with RegisterFragmentViewModel. . .
    fun appendNewSelectedTagsToTags(selectedTags: List<Tag>) {
        _tags.appendNewSelectedTagsToTags(selectedTags)
    }

    fun setSelectedTags(selectedTags: List<Tag>) {
        _selectedTags = selectedTags
    }

    fun addTags(tags: List<Tag>?) {
        if(tags != null) {
            _tags += tags
        }
    }
}