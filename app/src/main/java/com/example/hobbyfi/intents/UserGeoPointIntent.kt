package com.example.hobbyfi.intents

sealed class UserGeoPointIntent : Intent {
    object FetchAuthUserGeoPoint : UserGeoPointIntent()
    object FetchUsersGeoPoints: UserGeoPointIntent()

    object UpdateUserGeoPoint : UserGeoPointIntent()
}