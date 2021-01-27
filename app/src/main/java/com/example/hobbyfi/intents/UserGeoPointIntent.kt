package com.example.hobbyfi.intents

import com.google.firebase.firestore.GeoPoint

sealed class UserGeoPointIntent : Intent {
    object FetchAuthUserGeoPoint : UserGeoPointIntent()
    object FetchUsersGeoPoints: UserGeoPointIntent()

    data class UpdateUserGeoPoint(
        val username: String,
        val chatroomId: Long,
        val eventIds: List<Long>,
        val geoPoint: GeoPoint
    ) : UserGeoPointIntent()
}