package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.Base64Image
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.StartDateIdResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.*
import java.io.Closeable
import java.lang.Exception

class EventRepository(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI,
    hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager)
: CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {

    private val firestore = Firebase.firestore

    private val _userGeoPoints: MutableStateFlow<List<UserGeoPoint>> = MutableStateFlow(emptyList())

    fun getEvent(chatroomId: Long): Flow<Event?> {
        Log.i("EventRepository", "getEvent -> Getting current chatroom event!!!")
        return object : NetworkBoundFetcher<Event, CacheResponse<Event>>() {
            override suspend fun saveNetworkResult(response: CacheResponse<Event>) {
                saveEvent(response.model)
            }

            override fun shouldFetch(cache: Event?): Boolean {
                return adheresToDefaultCachePolicy(cache, R.string.pref_last_event_fetch_time)
            }

            override suspend fun loadFromDb(): Flow<Event?> = hobbyfiDatabase.eventDao().getEventByChatroomId(chatroomId)

            override suspend fun fetchFromNetwork(): CacheResponse<Event>? {
                Log.i("EventRepository", "Fetching event from network for chatroom id $chatroomId!")
                return performAuthorisedRequest({
                    hobbyfiAPI.fetchEvent(
                        prefConfig.getAuthUserToken()!!
                    )
                }, { fetchFromNetwork() })
            }
        }.asFlow()
    }

    suspend fun createEvent(name: String, description: String?,
                    date: String, base64Image: String?, lat: Double, long: Double): StartDateIdResponse? {
        Log.i("EventRepository", "createEvent -> Creating chatroom event w/ data. " +
                "Name: $name, description: $description, date: $date, lat: $lat, long: $long")
        return performAuthorisedRequest({
            hobbyfiAPI.createEvent(
                prefConfig.getAuthUserToken()!!,
                name,
                description,
                date,
                base64Image,
                lat,
                long
            )
        }, { createEvent(name, description, date, base64Image, lat, long) })
    }

    suspend fun deleteEvent(): Response? {
        Log.i("EventRepository", "deleteEvent -> Deleting current chatroom event!!!")

        return performAuthorisedRequest({
            hobbyfiAPI.deleteEvent(
                prefConfig.getAuthUserToken()!!
            )
        }, { deleteEvent() })
    }

    suspend fun editEvent(eventUpdateFields: Map<String?, String?>): Response? {
        Log.i("EventRepository", "editEvent -> Editing current chatroom event with fields: $eventUpdateFields")

        return performAuthorisedRequest({
            hobbyfiAPI.editEvent(
                prefConfig.getAuthUserToken()!!,
                eventUpdateFields
            )
        }, { editEvent(eventUpdateFields) })
    }

    suspend fun saveEvent(event: Event) {
        Log.i("EventRepository", "saveEvent -> Saving event into cache. Event: $event!")
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_event_fetch_time)
        hobbyfiDatabase.eventDao().upsert(event)
    }

    suspend fun deleteEventCache(id: Long): Boolean {
        Log.i("EventRepository", "deleteEventCache -> Deleting cached event w/ id: $id!")
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_event_fetch_time)
        return hobbyfiDatabase.eventDao().deleteEventById(id) > 0
    }

    fun getEventUsersGeoPoints(chatroomId: Long): StateFlow<List<UserGeoPoint>> {
        firestore.collection(Constants.LOCATIONS_COLLECTION)
            .whereEqualTo(Constants.CHATROOM_ID, chatroomId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("EventRepository", "User GeoPoint Firestore listener error!", e)
                    throw e
                }

                val geoPoints = ArrayList<UserGeoPoint>()
                for (doc in snapshots!!) {
                    val userChatroomId = doc.getLong(Constants.CHATROOM_ID)
                    val geoPoint = doc.getGeoPoint(Constants.LOCATION)
                    geoPoints.add(UserGeoPoint(userChatroomId!!, geoPoint!!))
                }

                _userGeoPoints.value = geoPoints
            }
        return _userGeoPoints
    }

    fun addToEventUserGeoPoints(chatroomId: Long, location: GeoPoint): LiveData<DocumentReference> {
        val map = hashMapOf(
            Pair(Constants.CHATROOM_ID, chatroomId),
            Pair(Constants.LOCATION, location)
        )
        val observer: MutableLiveData<DocumentReference> = MutableLiveData()
        firestore.collection(Constants.LOCATIONS_COLLECTION)
            .add(map)
            .addOnSuccessListener {
                Log.i("EventRepository", "Added User GeoPoint record to DB (chatroom_id: $chatroomId, location: $location)")
                observer.value = it
            }
            .addOnFailureListener {
                throw it
            }
        return observer
    }
}