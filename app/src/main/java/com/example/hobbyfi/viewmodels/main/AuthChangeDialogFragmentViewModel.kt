package com.example.hobbyfi.viewmodels.main

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayBindable
import com.example.hobbyfi.viewmodels.base.TwoWayBindableViewModel
import java.util.*

abstract class AuthChangeDialogFragmentViewModel(application: Application)
    : BaseViewModel(application), TwoWayBindable by TwoWayBindableViewModel() {
    @Bindable
    val email: MutableLiveData<String> = MutableLiveData()


}