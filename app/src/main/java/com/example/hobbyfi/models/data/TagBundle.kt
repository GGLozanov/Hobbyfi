package com.example.hobbyfi.models.data

import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.addAllDistinct
import com.example.hobbyfi.shared.newListWithDistinct

class TagBundle(initialTags: List<Tag>? = null) {
    private val _tags: MutableList<Tag> =
        if(initialTags == null) Constants.predefinedTags.toMutableList() else
            Constants.predefinedTags.newListWithDistinct(initialTags)
    val tags: List<Tag> get() = _tags

    private var _selectedTags: List<Tag> = initialTags ?: arrayListOf()
    val selectedTags: List<Tag> get() = _selectedTags

    // FIXME: Code dup with RegisterFragmentViewModel. . .
    fun appendNewSelectedTagsToTags(selectedTags: List<Tag>) {
        _tags.addAllDistinct(selectedTags)
    }

    fun setSelectedTags(tags: List<Tag>) {
        _selectedTags = tags
    }
}