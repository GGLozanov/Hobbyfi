package com.example.hobbyfi.viewmodels.create

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel

class ChatroomCreateFragmentViewModel(application: Application) : BaseViewModel(application),
    TwoWayDataBindable by TwoWayDataBindableViewModel() {
    // TODO: Extract TwoWayDataBindable + username + description + tags (maybe) in interface + class delegate for two-way data binding
    @Bindable
    val name: MutableLiveData<String> = MutableLiveData()

    @Bindable
    val description: MutableLiveData<String> = MutableLiveData()
}
