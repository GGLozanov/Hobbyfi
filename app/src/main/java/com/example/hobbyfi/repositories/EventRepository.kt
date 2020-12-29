package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.StartDateIdResponse
import com.example.hobbyfi.shared.PrefConfig
import kotlinx.coroutines.flow.Flow
import java.lang.Exception

class EventRepository(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI,
    hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
    : CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {

        fun getEvent(): Flow<Event?> {
            TODO("Not implemented /shrug")
        }

        fun createEvent(): StartDateIdResponse? {
            TODO("Not implemented /shrug")
        }

        fun deleteEvent(): Response? {
            TODO("Not implemented /shrug")
        }

        fun editEvent(eventUpdateFields: Map<String?, String?>): Response? {
            TODO("Not implemented /shrug")
        }

        fun saveEvent(event: Event) {
            TODO("Not implemented /shrug")
        }

        fun deleteEventCache(id: Long) {
            TODO("Not implemented /shrug")
        }

        // TODO: Cloud Firestore records here
}