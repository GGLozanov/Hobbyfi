package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Message(
    @PrimaryKey
    val id: Int,
    val message: String,
    val createTime: String // iso string?
) : Model() {
}