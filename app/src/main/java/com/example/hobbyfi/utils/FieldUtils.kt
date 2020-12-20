package com.example.hobbyfi.utils

import androidx.core.view.isEmpty
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

object FieldUtils {
    fun isTextFieldInvalid(inputLayout: TextInputLayout, errorText: String): Boolean {
        val error = inputLayout.error?.isNotBlank() == true

        if(error) {
            inputLayout.error = errorText
        }

        return error
    }
}