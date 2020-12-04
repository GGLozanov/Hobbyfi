package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.appendNewSelectedTagsToTags
import com.example.hobbyfi.shared.getNewSelectedTagsWithTags
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.generic.instance

// TODO: Fix code dup with AuthFragmentViewModel through fake mixins or something
@ExperimentalCoroutinesApi
class UserProfileFragmentViewModel(application: Application, initialTags: List<Tag>)
    : BaseViewModel(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    // FIXME: Code dup >:(
    private val _tags: MutableList<Tag> = (Constants.predefinedTags.getNewSelectedTagsWithTags(initialTags))
        .toMutableList()
    val tags: List<Tag> get() = _tags

    private var _selectedTags: List<Tag> = initialTags
    // this list instance will have its value updated once the tag selection dialog fragment finishes its workTag
    val selectedTags: List<Tag> get() = _selectedTags

    private var _base64Image: String? = null
    val base64Image get() = _base64Image

    fun setProfileImageBase64(base64Image: String) {
        _base64Image = base64Image
    }

    @Bindable
    val username: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val description: MutableLiveData<String?> = MutableLiveData()

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