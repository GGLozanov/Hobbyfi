package com.example.hobbyfi.intents

interface Intent {
    // empty interface used for generic constraints & workaround for sealed class inheritance limitations
    class InvalidIntentException : Exception()
}