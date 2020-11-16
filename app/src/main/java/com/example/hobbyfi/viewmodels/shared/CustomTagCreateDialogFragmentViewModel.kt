package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.fragment_custom_tag_create_dialog.*

class CustomTagCreateDialogFragmentViewModel(application: Application) : BaseViewModel(application), Observable {
    // two-way db & color picker view from 3rd party library

    @delegate:Transient
    private val callBacks: PropertyChangeRegistry by lazy { PropertyChangeRegistry() }

    @Bindable
    val name: MutableLiveData<String> = MutableLiveData()

    val colour: MutableLiveData<String> = MutableLiveData()

    fun setOnColourChangedListener(colourPickerView: ColorPickerView) {
        colourPickerView.setColorListener(ColorEnvelopeListener { colorEnvelope, fromUser ->
            colour.value = "#" + colorEnvelope.hexCode
        })
    }

    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.add(callback)

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback?) = callBacks.remove(callback)
}