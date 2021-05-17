package com.example.hobbyfi.viewmodels.shared

import android.app.Application
import android.graphics.PointF
import androidx.databinding.Bindable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.shared.PredicateMutableLiveData
import com.example.hobbyfi.viewmodels.base.BaseViewModel
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindableViewModel
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener

class CustomTagCreateDialogFragmentViewModel(application: Application) : BaseViewModel(application),
        TwoWayDataBindable by TwoWayDataBindableViewModel() {
    // two-way db & color picker view from 3rd party library

    @Bindable
    val name: PredicateMutableLiveData<String> = PredicateMutableLiveData { it == null || it.isEmpty() || it.length >= 25 }

    val colour: MutableLiveData<String> = MutableLiveData()

    fun setOnColourChangedListener(colourPickerView: ColorPickerView) {
        colourPickerView.setColorListener(ColorEnvelopeListener { colorEnvelope, _ ->
            colour.value = "#" + colorEnvelope.hexCode
        })
    }

    override val combinedObserversInvalidity: LiveData<Boolean>
        get() = name.invalidity
}