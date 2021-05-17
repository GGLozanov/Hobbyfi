package com.example.hobbyfi.viewmodels.base

import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.LiveData

interface TwoWayDataBindable : Observable {
    val callBacks: PropertyChangeRegistry

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.remove(callback)

    val combinedObserversInvalidity: LiveData<Boolean> // two way bindable => some form of validation
}