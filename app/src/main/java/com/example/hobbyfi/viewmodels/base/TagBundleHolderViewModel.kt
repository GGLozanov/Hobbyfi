package com.example.hobbyfi.viewmodels.base

import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.models.data.TagBundle

class TagBundleHolderViewModel(initialTags: List<Tag>? = null) : TagBundleHolder {
    override var tagBundle: TagBundle = TagBundle(initialTags)
}