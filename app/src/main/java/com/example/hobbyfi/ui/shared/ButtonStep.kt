package com.example.hobbyfi.ui.shared

import android.view.View
import com.example.hobbyfi.models.ui.StepperButtonInput
import com.google.android.material.button.MaterialButton
import ernestoyaquello.com.verticalstepperform.Step

class ButtonStep(
    title: String,
    private val onButtonClick: () -> Unit,
    private val viewInput: StepperButtonInput,
    private val optional: Boolean = true,
    private val error: String = "Button not pressed!",
    private val emptyHint: String = "(None)",
) : Step<Unit>(title) {
    private var buttonClicked: Boolean = false
    private lateinit var button: MaterialButton

    override fun getStepData() {}

    override fun getStepDataAsHumanReadableString(): String {
        return if(buttonClicked) "" else emptyHint
    }

    override fun restoreStepData(data: Unit?) {}

    override fun isStepDataValid(stepData: Unit?): IsDataValid =
        IsDataValid(optional, error)

    override fun createStepContentLayout(): View {
        button = MaterialButton(context).apply {
            icon = viewInput.icon
            text = viewInput.text
        }
        button.setOnClickListener {
            onButtonClick()
            buttonClicked = true
        }
        return button
    }

    override fun onStepOpened(animated: Boolean) {}

    override fun onStepClosed(animated: Boolean) {}

    override fun onStepMarkedAsCompleted(animated: Boolean) {}

    override fun onStepMarkedAsUncompleted(animated: Boolean) {}
}