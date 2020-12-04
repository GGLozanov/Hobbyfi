package com.example.hobbyfi.ui.base

import androidx.navigation.NavDirections
import com.example.hobbyfi.models.User

interface OnAuthStateChanged {
    fun login(action: NavDirections, token: String? = null, refreshToken: String? = null)
}