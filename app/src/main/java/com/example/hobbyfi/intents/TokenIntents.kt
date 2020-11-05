package com.example.hobbyfi.intents

sealed class TokenIntents {
    sealed class FetchLoginToken : TokenIntents()
    sealed class FetchRegisterToken : TokenIntents()
}