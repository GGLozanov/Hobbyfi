package com.example.spendidly.utils

import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.RequiresApi
import androidx.core.util.Predicate
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

// TODO: Should be replaced with TextInputLayout doOnTextChanged{} calls
class PredicateTextWatcher(
    private val textInput: TextInputLayout,
    private val errorText: String,
    private val predicate: Predicate<String>
) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if(predicate.test(s.toString())) {
            textInput.error = errorText
        } else {
            textInput.error = null
        }
    }

    override fun afterTextChanged(s: Editable?) {
    }

}