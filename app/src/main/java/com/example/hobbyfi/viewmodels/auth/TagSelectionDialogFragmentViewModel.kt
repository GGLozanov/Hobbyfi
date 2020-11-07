package com.example.hobbyfi.viewmodels.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class TagSelectionDialogFragmentViewModel(application: MultiDexApplication) : BaseViewModel(application) {
    private val _tags: MutableLiveData<List<Tag>> = MutableLiveData() // TODO: Change to tag ids or something?
    val tags: LiveData<List<Tag>>
        get() = _tags

    // respond to recyclerview clicks here and update tags list
}