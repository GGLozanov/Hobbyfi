package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Model

interface Intent {
    // empty interface used for generic constraints & workaround for sealed class inheritance limitations
    class InvalidIntentException : Exception()
}