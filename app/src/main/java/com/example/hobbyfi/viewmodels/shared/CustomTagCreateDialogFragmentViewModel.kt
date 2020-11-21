package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.MutableLiveData
import androidx.multidex.MultiDexApplication
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayBindable
import com.example.hobbyfi.viewmodels.base.TwoWayBindableViewModel
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import kotlinx.android.synthetic.main.fragment_custom_tag_create_dialog.*

class CustomTagCreateDialogFragmentViewModel(application: Application) : BaseViewModel(application), TwoWayBindable by TwoWayBindableViewModel() {
    // two-way db & color picker view from 3rd party library

    @Bindable
    val name: MutableLiveData<String> = MutableLiveData()

    val colour: MutableLiveData<String> = MutableLiveData()

    fun setOnColourChangedListener(colourPickerView: ColorPickerView) {
        colourPickerView.setColorListener(ColorEnvelopeListener { colorEnvelope, fromUser ->
            colour.value = "#" + colorEnvelope.hexCode
        })
    }

}