package com.example.hobbyfi.intents

sealed class FacebookIntent {
    object FetchFacebookUserTags : FacebookIntent()
    object FetchFacebookUserEmail : FacebookIntent()

    data class ValidateFacebookUserExistence(val username: String) : FacebookIntent() // request fired off to see if FB user exists in back-end
}