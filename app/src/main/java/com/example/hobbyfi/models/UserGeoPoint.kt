package com.example.hobbyfi.models

import com.google.firebase.firestore.GeoPoint

data class UserGeoPoint(
    val username: String,
    val chatroomId: Long,
    val eventIds: List<Long>,
    val geoPoint: GeoPoint
)