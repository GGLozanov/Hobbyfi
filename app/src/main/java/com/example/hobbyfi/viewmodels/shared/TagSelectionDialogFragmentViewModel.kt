package com.example.hobbyfi.viewmodels.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class TagSelectionDialogFragmentViewModel(application: MultiDexApplication) : BaseViewModel(application) {
    private val _tags: MutableLiveData<List<Tag>> = MutableLiveData(Constants.predefinedTags)
    val tags: LiveData<List<Tag>> get() = _tags
    // TODO: Change to List<Int> for tag ids?
    // respond to recyclerview clicks here and update tags list
}