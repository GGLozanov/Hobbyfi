package com.example.hobbyfi.utils

import androidx.core.view.isEmpty
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

object FieldUtils {
    fun isTextFieldInvalid(inputLayout: TextInputLayout, errorText: String): Boolean {
        inputLayout.editText!!.text = if(inputLayout.editText!!.text.isNullOrBlank())
            null else inputLayout.editText!!.text // trigger textwatcher at least once
        val error = inputLayout.error != null && inputLayout.error!!.isNotBlank()

        if(error) {
            inputLayout.error = errorText
        }

        return error
    }
}