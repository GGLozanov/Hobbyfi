package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class TagSelectionFragmentViewModel(application: Application, val initialSelectedTags: List<Tag>) : BaseViewModel(application) {

    var customTagCreateCounter: Int = 0

    fun incrementCustomTagCounter() {
        customTagCreateCounter++
    }
}