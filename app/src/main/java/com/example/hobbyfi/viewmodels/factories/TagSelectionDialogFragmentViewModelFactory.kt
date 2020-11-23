package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.ui.shared.TagSelectionDialogFragment
import com.example.hobbyfi.viewmodels.shared.TagSelectionDialogFragmentViewModel

class TagSelectionDialogFragmentViewModelFactory(val application: Application, val initialTags: MutableList<Tag>) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(TagSelectionDialogFragment::class.java)) {
            return TagSelectionDialogFragmentViewModel(application, initialTags) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}