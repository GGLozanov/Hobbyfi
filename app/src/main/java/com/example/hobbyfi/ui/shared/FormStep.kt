package com.example.hobbyfi.ui.shared

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.distinctUntilChanged
import com.example.hobbyfi.databinding.StepperInputFieldBinding
import com.example.hobbyfi.models.ui.StepperFormInput
import ernestoyaquello.com.verticalstepperform.Step

class FormStep(
    title: String,
    private val viewLifecycleOwner: LifecycleOwner,
    private val error: String,
    private val viewInput: StepperFormInput,
    private val emptyHint: String = "(Empty)",
    private val readableStepDataNotForbidden: Boolean = true
) : Step<String>(title) {
    private lateinit var inputBinding: StepperInputFieldBinding

    override fun getStepData(): String = viewInput.valueTracker.value ?: ""

    override fun getStepDataAsHumanReadableString(): String {
        return if (stepData.isNotEmpty() && readableStepDataNotForbidden) stepData else emptyHint
    }

    override fun isStepDataValid(stepData: String?): IsDataValid {
        Log.i("FormStep", "step data: ${stepData}")
        Log.i("FormStep", "is data valid: ${!viewInput.valueTracker.predicate(stepData)}")
        return IsDataValid(!viewInput.valueTracker.predicate(stepData), error)
    }

    override fun createStepContentLayout(): View {
        inputBinding = StepperInputFieldBinding.inflate(LayoutInflater.from(context))
        inputBinding.inputData = viewInput
        observeInput()

        return inputBinding.root
    }

    override fun onStepOpened(animated: Boolean) {}

    override fun onStepClosed(animated: Boolean) {}

    override fun onStepMarkedAsCompleted(animated: Boolean) {}

    override fun onStepMarkedAsUncompleted(animated: Boolean) {}

    override fun restoreStepData(data: String?) {
        observeInput()
        // inputBinding.inputField.editText?.setText(data)
    }

    private fun observeInput() {
        viewInput.valueTracker.distinctUntilChanged().observe(viewLifecycleOwner, Observer {
            markAsCompletedOrUncompleted(true)
        })
    }
}