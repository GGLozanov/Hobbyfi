package com.example.hobbyfi.intents

sealed class TokenIntent : Intent {
    // TODO: Facebook access token intent here?
    object FetchLoginToken : TokenIntent() // email & password are garnered with two-way databinding in viewmodel
    object FetchRegisterToken : TokenIntent() // = CreateUserIntent; user needed is created with two-way databinding in viewmodel
}