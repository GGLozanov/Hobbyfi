package com.example.hobbyfi.models.ui

import android.graphics.drawable.Drawable
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.shared.PredicateMutableLiveData

data class StepperFormInput(
    val hint: String,
    val startDrawable: Drawable,
    val endIconMode: Int,
    val inputType: Int,
    val valueTracker: PredicateMutableLiveData<String?>,
)