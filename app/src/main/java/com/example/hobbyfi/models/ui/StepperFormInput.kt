package com.example.hobbyfi.models.ui

import android.graphics.drawable.Drawable
import androidx.databinding.Bindable
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.shared.PredicateMutableLiveData

open class StepperFormInput<T : String?>(
    val hint: String,
    val startDrawable: Drawable,
    val endIconMode: Int,
    val inputType: Int,
    val valueTracker: PredicateMutableLiveData<T>,
)

class NullableStepperFormInput(
    hint: String,
    startDrawable: Drawable,
    endIconMode: Int,
    inputType: Int,
    valueTracker: PredicateMutableLiveData<String?>
): StepperFormInput<String?>(hint, startDrawable, endIconMode, inputType, valueTracker)

class NonNullableStepperFormInput(
    hint: String,
    startDrawable: Drawable,
    endIconMode: Int,
    inputType: Int,
    valueTracker: PredicateMutableLiveData<String>
) : StepperFormInput<String>(hint, startDrawable, endIconMode, inputType, valueTracker)