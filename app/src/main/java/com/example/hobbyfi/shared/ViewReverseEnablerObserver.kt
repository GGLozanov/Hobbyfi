package com.example.hobbyfi.shared

import android.util.Log
import android.view.View
import androidx.lifecycle.Observer

class ViewReverseEnablerObserver(private val view: View) : Observer<Boolean> {
    override fun onChanged(t: Boolean?) {
        Log.i("ViewReverseEnabler", "should be enabled: ${t == false}")
        view.isEnabled = t == false
    }
}