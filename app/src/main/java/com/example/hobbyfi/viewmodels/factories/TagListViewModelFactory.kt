package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.Tag
import com.example.hobbyfi.ui.shared.TagSelectionDialogFragment
import com.example.hobbyfi.viewmodels.main.ChatroomCreateFragmentViewModel
import com.example.hobbyfi.viewmodels.main.UserProfileFragmentViewModel
import com.example.hobbyfi.viewmodels.shared.TagSelectionDialogFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class TagListViewModelFactory(val application: Application, val initialTags: List<Tag>) : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(UserProfileFragmentViewModel::class.java)) {
            return UserProfileFragmentViewModel(application, initialTags) as T
        } else if(modelClass.isAssignableFrom(TagSelectionDialogFragmentViewModel::class.java)) {
            return TagSelectionDialogFragmentViewModel(application, initialTags) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}