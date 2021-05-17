package com.example.hobbyfi.viewmodels.base

import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class TwoWayDataBindableViewModel : TwoWayDataBindable {
    @delegate:Transient
    override val callBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    override val combinedObserversInvalidity: LiveData<Boolean> = MutableLiveData(false) // valid by default
}