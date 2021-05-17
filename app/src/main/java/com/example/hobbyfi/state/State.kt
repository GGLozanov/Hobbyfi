package com.example.hobbyfi.state

interface State {
    // empty interface used for generic constraints & workaround for sealed class inheritance limitations
    class InvalidStateException : Exception()
}