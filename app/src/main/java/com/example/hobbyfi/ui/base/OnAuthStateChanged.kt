package com.example.hobbyfi.ui.base

import androidx.navigation.NavDirections

interface OnAuthStateChanged {
    fun login(action: NavDirections, token: String? = null, refreshToken: String? = null)
}