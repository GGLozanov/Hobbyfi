package com.example.hobbyfi.viewmodels.base

import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry

interface TwoWayDataBindable : Observable {
    val callBacks: PropertyChangeRegistry

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.remove(callback)
}