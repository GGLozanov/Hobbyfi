package com.example.spendidly.utils

import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputEditText
import java.util.function.Predicate

class PredicateTextWatcher(
    private val editText: TextInputEditText,
    private val errorText: String,
    private val predicate: Predicate<String>
) : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        if(predicate.test(s.toString())) {
            editText.error = errorText
        }
    }

    override fun afterTextChanged(s: Editable?) {
    }

}