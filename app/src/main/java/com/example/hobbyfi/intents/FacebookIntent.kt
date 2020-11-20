package com.example.hobbyfi.intents

sealed class FacebookIntent {
    object FetchFacebookUserTags : FacebookIntent()
    object FetchFacebookUserEmail : FacebookIntent()
}