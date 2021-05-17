package com.example.hobbyfi.intents

sealed class FacebookIntent : Intent {
    object FetchFacebookUserTags : FacebookIntent()
    object FetchFacebookUserEmail : FacebookIntent()

    data class ValidateFacebookUserExistence(val id: Long) : FacebookIntent() // request fired off to see if FB user exists in back-end
}