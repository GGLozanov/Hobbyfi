package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// FIXME: This is not a very good abstraction...
interface Model {
    val id: Int
}
// TODO: Add id here (make it a class) without overriding it in child and making Room confused