package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class TagSelectionDialogFragmentViewModel(application: Application) : BaseViewModel(application) {
//    private val _tags: MutableLiveData<MutableList<Tag>> = MutableLiveData()
//    val tags: LiveData<MutableList<Tag>> get() = _tags
//
    private var initialSelectedTags: MutableList<Tag>? = null

    // TODO: Probably convert this to primitive list without livedata wrapping around it
//    private val _selectedTags: MutableLiveData<MutableList<Tag>> = MutableLiveData(mutableListOf())
//    val selectedTags: LiveData<MutableList<Tag>> get() = _selectedTags

    var customTagCreateCounter: Int = 0
    // TODO: Change to List<Int> for tag ids?
    // respond to recyclerview clicks here and update tags list

    fun setInitialSelectedTags(tags: MutableList<Tag>?) {
        initialSelectedTags = tags
    }

    fun getInitialSelectedTags() : MutableList<Tag> {
        return initialSelectedTags!!
    }
//
//    fun setSelectedTags(tags: MutableList<Tag>) {
//        _selectedTags.value = tags
//    }
//
//    fun addTag(tag: Tag) {
//        _tags.value?.add(tag)
//    }
//
//    fun addTagToSelected(tag: Tag) {
//        _selectedTags.value?.add(tag)
//    }
//
//    fun removeTagFromSelected(tag: Tag) {
//        _selectedTags.value?.remove(tag)
//    }
    
    fun incrementCustomTagCounter() {
        customTagCreateCounter++
    }
}