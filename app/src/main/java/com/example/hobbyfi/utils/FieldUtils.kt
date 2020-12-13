package com.example.hobbyfi.utils

import com.google.android.material.textfield.TextInputEditText

object FieldUtils {
    fun isTextFieldInvalid(editText: TextInputEditText): Boolean {
        return editText.error?.isNotBlank() == true
    }
}