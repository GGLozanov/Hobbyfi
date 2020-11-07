package com.example.hobbyfi.viewmodels.auth

import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class CustomTagCreateDialogFragmentViewModel(application: MultiDexApplication) : BaseViewModel(application) {
    // two-way db & color picker view from 3rd party library
    @Bindable
    val name: MutableLiveData<String> = MutableLiveData()
}