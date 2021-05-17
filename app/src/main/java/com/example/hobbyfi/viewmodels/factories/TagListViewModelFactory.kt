package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.data.Tag
import com.example.hobbyfi.viewmodels.chatroom.ChatroomEditFragmentViewModel
import com.example.hobbyfi.viewmodels.main.UserProfileFragmentViewModel
import com.example.hobbyfi.viewmodels.shared.TagSelectionFragmentViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class TagListViewModelFactory(val application: Application, val initialTags: List<Tag>) : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(UserProfileFragmentViewModel::class.java) -> {
                UserProfileFragmentViewModel(application, initialTags) as T
            }
            modelClass.isAssignableFrom(TagSelectionFragmentViewModel::class.java) -> {
                TagSelectionFragmentViewModel(application, initialTags) as T
            }
            modelClass.isAssignableFrom(ChatroomEditFragmentViewModel::class.java) -> {
                ChatroomEditFragmentViewModel(application, initialTags) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
}