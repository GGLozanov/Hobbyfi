package com.example.hobbyfi.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlin.properties.Delegates

class PredicateMutableLiveData<T> : MutableLiveData<T> {
    // T is nullable here because lack of LiveData type safety due to being written in Java
    private var _predicate: (T?) -> Boolean by Delegates.notNull()
    val predicate: (T?) -> Boolean get()  = _predicate

    private val _invalidity: MutableLiveData<Boolean>
    val invalidity: LiveData<Boolean> get() = _invalidity

    constructor(value: T, predicate: (T?) -> Boolean) : super(value) {
        this._predicate = predicate
        _invalidity = MutableLiveData(predicate(value))
    }

    constructor(predicate: (T?) -> Boolean) : super() {
        this._predicate = predicate
        _invalidity = MutableLiveData(predicate(value))
    }

    override fun postValue(value: T?) {
        _invalidity.postValue(_predicate.invoke(value))
        super.postValue(value)
    }

    override fun setValue(value: T?) {
        _invalidity.value = _predicate.invoke(value)
        super.setValue(value)
    }
}