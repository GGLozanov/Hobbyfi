package com.example.hobbyfi.utils

import android.graphics.Color
import android.util.Log
import kotlin.random.Random

object ColourUtils {
    fun getRandomHex(): String {
//        val letters = "0123456789ABCDEF"
//        var colour = "#"
//
//        for(i in 0..6) {
//            colour += letters[Random.nextInt(letters.length)]
//        }
//        return colour
        return String.format("#{0:X6}", Random.nextInt(0x1000000))
    }

    fun getRandomColour(): Int {
        return try {
             Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
        } catch(ex: IllegalArgumentException) {
            Log.w("ColourUtils" , "Invalid color for tag! Reverting to default colour")
            Color.GREEN // default colour, idk
        }
    }
}