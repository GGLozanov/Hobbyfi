package com.example.hobbyfi.viewmodels.base

import androidx.databinding.PropertyChangeRegistry

class TwoWayDataBindableViewModel : TwoWayDataBindable {
    @delegate:Transient
    override val callBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }
}