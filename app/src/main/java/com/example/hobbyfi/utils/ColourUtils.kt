package com.example.hobbyfi.utils

import kotlin.random.Random

object ColourUtils {
    fun getRandomHex() : String {
        return String.format("#{0:X6}", Random.nextInt(0x1000000))
    }
}