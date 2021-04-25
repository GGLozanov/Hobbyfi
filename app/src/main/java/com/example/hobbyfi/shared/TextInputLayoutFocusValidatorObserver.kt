package com.example.hobbyfi.shared

import android.content.res.Resources
import com.example.hobbyfi.MainApplication
import com.example.hobbyfi.R
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
            get() = TextInputLayoutFocusValidatorObserver(textInputLayout,
                MainApplication.applicationContext.resources.getString(R.string.invalid_credentials)) // general error

        fun setError(errorText: String): TextInputLayoutFocusValidatorObserverBuilder {
            instance.setErrorText(errorText)
            return this
        }
    }
}