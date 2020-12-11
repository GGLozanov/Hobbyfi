package com.example.hobbyfi.viewmodels.base

import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData

// TODO: Update name and properties if schema needs to change
interface NameDescriptionBindable : TwoWayDataBindable {
    // putting bindable properties here only as they can be exposed and not ruing encapsulation
    // due to requiring two-way databinding

    @get:Bindable
    val name: MutableLiveData<String>

    @get:Bindable
    val description: MutableLiveData<String?>
}