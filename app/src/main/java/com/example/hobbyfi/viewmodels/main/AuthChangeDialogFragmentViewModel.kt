package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.repositories.TokenRepository
import com.example.hobbyfi.repositories.UserRepository
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayBindable
import com.example.hobbyfi.viewmodels.base.TwoWayBindableViewModel
import org.kodein.di.generic.instance
import java.util.*

abstract class AuthChangeDialogFragmentViewModel(application: Application)
    : BaseViewModel(application), TwoWayBindable by TwoWayBindableViewModel() {
    protected val userRepository: UserRepository by instance(tag = "userRepository")

    @Bindable
    val email: MutableLiveData<String> = MutableLiveData()
}