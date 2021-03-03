package com.example.hobbyfi.viewmodels.main

import android.app.Application
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.TagBundle
import com.example.hobbyfi.viewmodels.base.*
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
class UserProfileFragmentViewModel(application: Application, initialTags: List<Tag>) : BaseViewModel(application),
        NameDescriptionBindable by NameDescriptionBindableViewModel(), Base64ImageHolder by Base64ImageHolderViewModel() {
    var tagBundle: TagBundle = TagBundle(initialTags)
    private var _originalSelectedTags: List<Tag> = tagBundle.selectedTags
    val originalSelectedTags get() = _originalSelectedTags

    fun setOriginalSelectedTags(tags: List<Tag>) {
        _originalSelectedTags = tags
    }
}