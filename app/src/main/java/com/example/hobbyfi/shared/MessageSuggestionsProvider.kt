package com.example.hobbyfi.shared

import android.content.SearchRecentSuggestionsProvider

class MessageSuggestionsProvider : SearchRecentSuggestionsProvider() {
    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY = "com.example.hobbyfi.MessageSuggestionsProvider"
        const val MODE: Int = DATABASE_MODE_QUERIES
    }
}