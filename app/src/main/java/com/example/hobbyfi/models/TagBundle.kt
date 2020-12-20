package com.example.hobbyfi.models

import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.appendNewSelectedTagsToTags
import com.example.hobbyfi.shared.getNewSelectedTagsWithTags

class TagBundle(initialTags: List<Tag>? = null) {
    private val _tags: MutableList<Tag> =
        if(initialTags == null) Constants.predefinedTags.toMutableList() else
            Constants.predefinedTags.getNewSelectedTagsWithTags(initialTags)
    val tags: List<Tag> get() = _tags

    private var _selectedTags: List<Tag> = initialTags ?: emptyList()
    val selectedTags: List<Tag> get() = _selectedTags

    // FIXME: Code dup with RegisterFragmentViewModel. . .
    fun appendNewSelectedTagsToTags(selectedTags: List<Tag>) {
        _tags.appendNewSelectedTagsToTags(selectedTags)
    }

    fun setSelectedTags(tags: List<Tag>) {
        _selectedTags = tags
    }
}