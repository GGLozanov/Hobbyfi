package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import android.graphics.PointF
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class CustomTagCreateDialogFragmentViewModel(application: Application) : BaseViewModel(application), TwoWayDataBindable by TwoWayDataBindableViewModel() {
    // two-way db & color picker view from 3rd party library

    @Bindable
    val name: MutableLiveData<String> = MutableLiveData()

    val colour: MutableLiveData<String> = MutableLiveData()

    fun setOnColourChangedListener(colourPickerView: ColorPickerView) {
        colourPickerView.setColorListener(ColorEnvelopeListener { colorEnvelope, _ ->
            colour.value = "#" + colorEnvelope.hexCode
        })
    }

}