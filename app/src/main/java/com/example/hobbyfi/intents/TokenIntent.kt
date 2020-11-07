package com.example.hobbyfi.intents

sealed class TokenIntent : Intent {
    sealed class FetchLoginToken : TokenIntent() // email & password are garnered with two-way databinding in viewmodel
    sealed class FetchRegisterToken : TokenIntent() // = CreateUserIntent; user needed is created with two-way databinding in viewmodel
}