package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.TwoWayBindable
import com.example.hobbyfi.viewmodels.base.TwoWayBindableViewModel

class UserProfileFragmentViewModel(application: Application)
    : MainFragmentViewModel(application), TwoWayBindable by TwoWayBindableViewModel() {
    // TODO: Implement the ViewModel

    @Bindable
    val username: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val description: MutableLiveData<String?> = MutableLiveData()

}