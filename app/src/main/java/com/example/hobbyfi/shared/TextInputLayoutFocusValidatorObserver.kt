package com.example.hobbyfi.shared

import com.google.android.material.textfield.TextInputLayout

class TextInputLayoutFocusValidatorObserver(
    textInputLayout: TextInputLayout,
    private var errorText: String
) : TextInputLayoutFocusObserver<Boolean>(textInputLayout) {
    override fun onChangedWithFocusState(t: Boolean, textInputLayout: TextInputLayout) {
        textInputLayout.error = if(t) errorText else null
    }

    fun setErrorText(errorText: String) {
        this.errorText = errorText
    }

    class TextInputLayoutFocusValidatorObserverBuilder(textInputLayout: TextInputLayout) :
            TextInputLayoutFocusObserverBuilder<Boolean>(textInputLayout) {
        override val instance: TextInputLayoutFocusValidatorObserver
            get() = TextInputLayoutFocusValidatorObserver(textInputLayout, Constants.invalidCredentialsError) // general error

        fun setError(errorText: String): TextInputLayoutFocusValidatorObserverBuilder {
            instance.setErrorText(errorText)
            return this
        }
    }
}