package com.example.hobbyfi.utils

import android.graphics.Color
import android.util.Log
import kotlin.random.Random

object ColourUtils {
    fun getRandomHex(): String {
        return String.format("#{0:X6}", Random.nextInt(0x1000000))
    }

    fun getColourOrGreen(colourHex: String?): Int {
        if(colourHex == null) {
            return Color.GREEN
        }

        return try {
            Color.parseColor(colourHex)
        } catch(ex: IllegalArgumentException) {
            Log.w("ColourUtils" , "Invalid color for tag! Reverting to default colour")
            Color.GREEN
        }
    }
}