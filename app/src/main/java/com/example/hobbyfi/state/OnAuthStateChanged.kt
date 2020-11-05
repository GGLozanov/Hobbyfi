package com.example.hobbyfi.state

import com.example.hobbyfi.models.User

interface OnAuthStateChanged {
    fun performAuthChange(user: User)
}