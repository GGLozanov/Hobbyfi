package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.models.TagBundle
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
    var tagBundle: TagBundle = TagBundle(initialTags)
    private var _originalSelectedTags: List<Tag> = tagBundle.selectedTags
    val originalSelectedTags get() = _originalSelectedTags

    fun setOriginalSelectedTags(tags: List<Tag>) {
        _originalSelectedTags = tags
    }

    private var _base64Image: String? = null
    val base64Image get() = _base64Image

    fun setProfileImageBase64(base64Image: String) {
        _base64Image = base64Image
    }
}