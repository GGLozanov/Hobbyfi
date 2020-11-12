package com.example.hobbyfi.models

interface ExpandedModel : Model {
    val name: String
    val description: String
    val hasImage: Boolean
}