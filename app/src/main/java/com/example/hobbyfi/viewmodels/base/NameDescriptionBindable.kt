package com.example.hobbyfi.viewmodels.base

import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData

// putting bindable properties here only as they can be exposed and not ruing encapsulation
// due to requiring two-way databinding
interface NameDescriptionBindable : TwoWayDataBindable {
    @get:Bindable
    val name: MutableLiveData<String>

    @get:Bindable
    val description: MutableLiveData<String?>
}