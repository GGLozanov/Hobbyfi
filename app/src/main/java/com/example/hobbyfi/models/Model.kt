package com.example.hobbyfi.models

import androidx.room.Entity
import androidx.room.PrimaryKey

// FIXME: This is not a very good abstraction...
interface Model {
    val id: Long // FIXME: find a way to work around this being 'Long' since it's only required for the huge facebook user Id
}
// TODO: Add id here (make it a class) without overriding it in child and making Room confused