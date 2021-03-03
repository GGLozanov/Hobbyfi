package com.example.hobbyfi.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.hobbyfi.models.data.User
import com.example.hobbyfi.viewmodels.main.MainActivityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi

class AuthUserViewModelFactory(val application: Application, val user: User?) : ViewModelProvider.Factory {
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            return MainActivityViewModel(application, user) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}