package com.example.hobbyfi.viewmodels.create

import android.app.Application
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.*

class ChatroomCreateFragmentViewModel(application: Application) : BaseViewModel(application),
    NameDescriptionBindable by NameDescriptionBindableViewModel() {

}
