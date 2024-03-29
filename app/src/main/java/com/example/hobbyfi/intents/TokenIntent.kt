package com.example.hobbyfi.intents

sealed class TokenIntent : Intent {
    object FetchLoginToken : TokenIntent() // email & password are garnered with two-way databinding in viewmodel
    object FetchRegisterToken : TokenIntent() // = CreateUserIntent; user needed is created with two-way databinding in viewmodel
    object ResetPassword : TokenIntent()
    data class FetchFacebookRegisterToken(val facebookToken: String, val username: String) : TokenIntent() // rest of params garnered by viewmodel
}