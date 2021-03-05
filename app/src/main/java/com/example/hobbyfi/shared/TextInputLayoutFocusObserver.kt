package com.example.hobbyfi.shared

import androidx.lifecycle.Observer
import com.google.android.material.textfield.TextInputLayout

// observers trigger only whenever edittext isn't focused OR is focused AND has an error set
abstract class TextInputLayoutFocusObserver<T>(val textInputLayout: TextInputLayout) : Observer<T> {
    private var focusState: Boolean = textInputLayout.isFocused
    private var _lastValue: T? = null

    // bit retarded to constrain to only T type dependents but the add() method doesn't work w/
    // TextInputLayoutFocusValidatorObserver otherwise
    private val dependents: MutableList<TextInputLayoutFocusObserver<T>> = mutableListOf()

    abstract fun onChangedWithFocusState(t: T, textInputLayout: TextInputLayout)
    
    init {
        textInputLayout.editText!!.setOnFocusChangeListener { _, focused ->
            focusState = focused
            if(!focusState) {
                _lastValue?.let {
                    onChangedWithFocusState(it, textInputLayout)
                }
            }
        }
    }

    override fun onChanged(t: T) {
        _lastValue = t

        // edittext error should be the same as inputlayout error
        if(focusState && textInputLayout.error != null) { // there's an error and focus => mandatory propagation
            onChangedWithFocusState(t, textInputLayout)
        }

        dependents.forEach {
            it._lastValue?.let { lastVal ->
                it.onChangedWithFocusState(lastVal, it.textInputLayout)
            }
        }
    }

    fun addDependent(obs: TextInputLayoutFocusObserver<T>) = dependents.add(obs)

    fun removeDependent(obs: TextInputLayoutFocusObserver<T>) = dependents.remove(obs)

    fun removeDependentAt(idx: Int) = dependents.removeAt(idx)

    fun clearDependents() = dependents.clear()

    abstract class TextInputLayoutFocusObserverBuilder<T>(protected val textInputLayout: TextInputLayout) {
        protected abstract val instance: TextInputLayoutFocusObserver<T>

        fun build() = instance

        fun addDependent(obs: TextInputLayoutFocusObserver<T>): TextInputLayoutFocusObserverBuilder<T> {
            instance.addDependent(obs)
            return this
        }
    }
}