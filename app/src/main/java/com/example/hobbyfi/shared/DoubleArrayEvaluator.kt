package com.example.hobbyfi.shared

import android.animation.TypeEvaluator

class DoubleArrayEvaluator(private val array: DoubleArray? = null) : TypeEvaluator<DoubleArray> {
    override fun evaluate(
        fraction: Float,
        startValue: DoubleArray,
        endValue: DoubleArray
    ): DoubleArray {
        var array = this.array
        if (array == null) {
            array = DoubleArray(startValue.size)
        }
        for (i in array.indices) {
            val start = startValue[i]
            val end = endValue[i]
            array[i] = start + fraction * (end - start)
        }
        return array
    }
}