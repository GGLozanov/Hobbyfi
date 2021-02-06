package com.example.hobbyfi.intents

import com.google.firebase.firestore.GeoPoint

sealed class UserGeoPointIntent : Intent {
    object FetchAuthUserGeoPoint : UserGeoPointIntent()
    data class FetchUsersGeoPoints(val authGeoPointUsername: String?) : UserGeoPointIntent()

    data class UpdateUserGeoPoint(
        val username: String,
        val chatroomId: List<Long>,
        val eventIds: List<Long>,
        val geoPoint: GeoPoint
    ) : UserGeoPointIntent()
}