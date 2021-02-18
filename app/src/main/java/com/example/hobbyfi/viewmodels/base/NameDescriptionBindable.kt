package com.example.hobbyfi.viewmodels.base

import androidx.databinding.Bindable
import com.example.hobbyfi.shared.PredicateMutableLiveData

// putting bindable properties here only as they can be exposed and not ruing encapsulation
// due to requiring two-way databinding
interface NameDescriptionBindable : TwoWayDataBindable {
    @get:Bindable
    val name: PredicateMutableLiveData<String>

    @get:Bindable
    val description: PredicateMutableLiveData<String?>
}