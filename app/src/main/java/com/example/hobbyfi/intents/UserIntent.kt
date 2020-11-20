package com.example.hobbyfi.intents

// delete/update intents
sealed class UserIntent : Intent {
    sealed class FetchUsers(val chatroomId: Int) : UserIntent()
    sealed class UpdateUser(val userUpdateFields: Map<String, String>) : UserIntent()

    object DeleteUser : UserIntent() // always auth user => no need to send userId as argument since it's in jwt
    object FetchUser : UserIntent() // always auth user => no need to send userId as argument since it's in jwt
}