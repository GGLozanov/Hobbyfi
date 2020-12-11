package com.example.hobbyfi.viewmodels.base

import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData

class NameDescriptionBindableViewModel : NameDescriptionBindable, TwoWayDataBindable by TwoWayDataBindableViewModel() {
    override val name: MutableLiveData<String> = MutableLiveData()
    override val description: MutableLiveData<String?> = MutableLiveData()
}