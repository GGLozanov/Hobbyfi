package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class TagSelectionDialogFragmentViewModel(application: Application) : BaseViewModel(application) {

    private var initialSelectedTags: MutableList<Tag>? = null

    var customTagCreateCounter: Int = 0
    // TODO: Change to List<Int> for tag ids?
    // respond to recyclerview clicks here and update tags list

    fun setInitialSelectedTags(tags: MutableList<Tag>?) {
        initialSelectedTags = tags
    }

    fun getInitialSelectedTags() : MutableList<Tag> {
        return initialSelectedTags!!
    }
    fun incrementCustomTagCounter() {
        customTagCreateCounter++
    }
}