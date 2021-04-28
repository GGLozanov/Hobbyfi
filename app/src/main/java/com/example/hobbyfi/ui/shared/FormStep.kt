package com.example.hobbyfi.ui.shared

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.example.hobbyfi.databinding.StepperInputFieldBinding
import com.example.hobbyfi.models.ui.StepperFormInput
import com.example.hobbyfi.viewmodels.base.TwoWayDataBindable
import ernestoyaquello.com.verticalstepperform.Step

class FormStep(
    title: String,
    private val viewLifecycleOwner: LifecycleOwner,
    private val error: String,
    private val viewInput: StepperFormInput,
    private val emptyHint: String = "(Empty)"
) : Step<String>(title) {
    private lateinit var inputBinding: StepperInputFieldBinding

    override fun getStepData(): String = viewInput.valueTracker.value ?: ""

    override fun getStepDataAsHumanReadableString(): String {
        return if (stepData.isNotEmpty()) stepData else emptyHint
    }

    override fun isStepDataValid(stepData: String?): IsDataValid =
        IsDataValid(!viewInput.valueTracker.predicate(stepData), error)

    override fun createStepContentLayout(): View {
        inputBinding = StepperInputFieldBinding.inflate(LayoutInflater.from(context))
        inputBinding.inputData = viewInput
        viewInput.valueTracker.observe(viewLifecycleOwner, Observer {
            markAsCompletedOrUncompleted(true)
        })

        return inputBinding.root
    }

    override fun onStepOpened(animated: Boolean) {}

    override fun onStepClosed(animated: Boolean) {}

    override fun onStepMarkedAsCompleted(animated: Boolean) {}

    override fun onStepMarkedAsUncompleted(animated: Boolean) {}

    override fun restoreStepData(data: String?) {
        inputBinding.inputField.editText?.setText(data)
    }
}