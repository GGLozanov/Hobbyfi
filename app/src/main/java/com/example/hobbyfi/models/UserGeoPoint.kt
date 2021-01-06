package com.example.hobbyfi.models

import com.google.firebase.firestore.GeoPoint

data class UserGeoPoint(
    val chatroomId: Long,
    val geoPoint: GeoPoint
)