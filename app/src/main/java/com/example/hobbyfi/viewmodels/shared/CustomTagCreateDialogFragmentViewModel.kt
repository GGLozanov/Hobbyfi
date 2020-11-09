package com.example.hobbyfi.viewmodels.shared

import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.BaseViewModel

class CustomTagCreateDialogFragmentViewModel(application: MultiDexApplication) : BaseViewModel(application), Observable {
    // two-way db & color picker view from 3rd party library

    @delegate:Transient
    private val callBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    @Bindable
    val name: MutableLiveData<String> = MutableLiveData()

    // TODO: Color hex livedata?

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.remove(callback)
}