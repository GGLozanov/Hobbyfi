package com.example.hobbyfi.ui.base

import com.example.hobbyfi.models.User

interface OnAuthStateChanged {
    fun login(user: User?, token: String? = null, refreshToken: String? = null)
}