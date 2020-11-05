package com.example.hobbyfi.models

import androidx.room.Entity

@Entity
data class User(
    override val id: Int
) : Model(id) {
}