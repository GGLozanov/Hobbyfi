package com.example.hobbyfi.intents

import com.example.hobbyfi.models.Tag

sealed class TokenIntent : Intent {
    object FetchLoginToken : TokenIntent() // email & password are garnered with two-way databinding in viewmodel
    object FetchRegisterToken : TokenIntent() // = CreateUserIntent; user needed is created with two-way databinding in viewmodel

    data class FetchFacebookRegisterToken(val username: String, val image: String) : TokenIntent() // rest of params garnered by viewmodel
}