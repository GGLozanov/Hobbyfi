package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class TagSelectionDialogFragmentViewModel(application: Application, val initialSelectedTags: List<Tag>) : BaseViewModel(application) {

    var customTagCreateCounter: Int = 0

    fun incrementCustomTagCounter() {
        customTagCreateCounter++
    }
}