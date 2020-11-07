package com.example.hobbyfi.models

import androidx.room.Entity

data class Token(
    val token: String?,
    val refreshToken: String?
) : Model