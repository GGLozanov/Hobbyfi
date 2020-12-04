package com.example.hobbyfi.intents

sealed class UserListIntent : Intent {
    object FetchUsers : UserListIntent()
}
