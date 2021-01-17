package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.Event
import com.example.hobbyfi.models.UserGeoPoint
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.StartDateIdResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class EventRepository(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI,
    hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager
): CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
    private val _userGeoPoints: MutableStateFlow<List<UserGeoPoint>> = MutableStateFlow(emptyList())
    private val _userGeoPoint: MutableStateFlow<UserGeoPoint?> = MutableStateFlow(null)

    fun getEvents(chatroomId: Long): Flow<List<Event>?> {
        Log.i("EventRepository", "getEvent -> Getting current chatroom eventS!!!")
        return object : NetworkBoundFetcher<List<Event>?, CacheListResponse<Event>>() {
            override suspend fun saveNetworkResult(response: CacheListResponse<Event>) {
                saveEvents(response.modelList, replace = true)
            }

            override fun shouldFetch(cache: List<Event>?): Boolean {
                return adheresToDefaultCachePolicy(cache, R.string.pref_last_events_fetch_time)
            }

            override suspend fun loadFromDb(): Flow<List<Event>?> = hobbyfiDatabase.eventDao().getEventByChatroomId(chatroomId)

            override suspend fun fetchFromNetwork(): CacheListResponse<Event>? {
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

    // TODO: Add id to request (event delete when one-to-many is developed)
    suspend fun deleteEvent(eventId: Long): Response? {
        Log.i("EventRepository", "deleteEvent -> Deleting chatroom event with id $eventId!!!")

        return performAuthorisedRequest({
            val response = hobbyfiAPI.deleteEvent(
                prefConfig.getAuthUserToken()!!,
                eventId
            )

            firestore.collection(Constants.LOCATIONS_COLLECTION)
                .whereArrayContains(Constants.EVENT_IDS, eventId)
                .get().addOnSuccessListener {
                    it.documents.forEach { doc ->
                        val eventIds = doc.get(Constants.EVENT_IDS) as List<Long>
                        if(eventIds.isEmpty()) {
                            doc.reference.delete()
                        }

                        doc.reference.update(Constants.EVENT_IDS, eventIds.filter { id -> id != eventId })
                    }
                }.addOnFailureListener {
                    throw FirebaseException(Constants.firestoreDeletionError)
                }
            response
        }, { deleteEvent(eventId) })
    }

    suspend fun deleteOldEvents(): CacheListResponse<Long>? {
        Log.i("EventRepository", "deleteEvent -> Deleting old events!!!")

        return performAuthorisedRequest({
            hobbyfiAPI.deleteOldEvents(
                prefConfig.getAuthUserToken()!!
            )
        }, { deleteOldEvents() })
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
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_events_fetch_time)
        hobbyfiDatabase.eventDao().upsert(event)
    }

    suspend fun saveEvents(events: List<Event>, replace: Boolean = false) {
        Log.i("EventRepository", "saveEventS -> Saving eventS into cache. EventS: $events!")
        prefConfig.writeLastPrefFetchTimeNow(R.string.pref_last_events_fetch_time)

        withContext(Dispatchers.IO) {
            if(replace) {
                hobbyfiDatabase.eventDao().deleteEvents()
            }

            hobbyfiDatabase.eventDao().upsert(events)
        }
    }

    suspend fun deleteEventCache(id: Long): Boolean {
        Log.i("EventRepository", "deleteEventCache -> Deleting cached event w/ id: $id!")
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_events_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.eventDao().deleteEventById(id) > 0
        }
    }

    suspend fun deleteEventsCache(ids: List<Long>): Boolean {
        Log.i("EventRepository", "deleteEventCache -> Deleting cached events w/ id: $ids!")
        prefConfig.resetLastPrefFetchTime(R.string.pref_last_events_fetch_time)
        return withContext(Dispatchers.IO) {
            hobbyfiDatabase.eventDao().deleteEventById(ids) > 0
        }
    }

    fun getEventUserGeoPoint(username: String): StateFlow<UserGeoPoint?> {
        firestore.collection(Constants.LOCATIONS_COLLECTION).document(username)
            .addSnapshotListener { doc, e ->
                if (e != null) {
                    Log.w("EventRepository", "User GeoPoint Firestore listener error!", e)
                    throw e
                }

                val userChatroomId = doc?.getLong(Constants.CHATROOM_ID)
                val geoPoint = doc?.getGeoPoint(Constants.LOCATION)
                val eventIds = doc?.get(Constants.EVENT_IDS) as List<Long>?

                val userGeoPoint = if(userChatroomId == null || geoPoint == null || eventIds == null || eventIds.isEmpty()) null
                    else UserGeoPoint(doc.id, userChatroomId, eventIds, geoPoint)

                Log.i("EventRepository", "getEventUserGeoPoint -> Received user Geo Point: $userGeoPoint")
                _userGeoPoint.value = userGeoPoint
            }
        return _userGeoPoint
    }

    fun getEventUsersGeoPoint(eventId: Long): StateFlow<List<UserGeoPoint>> {
        firestore.collection(Constants.LOCATIONS_COLLECTION)
            .whereArrayContains(Constants.EVENT_IDS, eventId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("EventRepository", "User GeoPoint Firestore listener error!", e)
                    throw e
                }

                val geoPoints = ArrayList<UserGeoPoint?>()
                for (doc in snapshots!!) {
                    val userChatroomId = doc.getLong(Constants.CHATROOM_ID)
                    val geoPoint = doc.getGeoPoint(Constants.LOCATION)
                    val eventIds = doc.get(Constants.EVENT_IDS) as List<Long>?
                    geoPoints.add(if(userChatroomId == null || geoPoint == null || eventIds == null || eventIds.isEmpty()) null else
                        UserGeoPoint(doc.id, userChatroomId, eventIds, geoPoint))
                }

                _userGeoPoints.value = geoPoints.filterNotNull()
            }
        return _userGeoPoints
    }

    fun setEventUserGeoPoints(username: String, chatroomId: Long,
                              eventIds: List<Long>, location: GeoPoint): LiveData<UserGeoPoint> {
        val map = hashMapOf(
            Pair(Constants.CHATROOM_ID, chatroomId),
            Pair(Constants.LOCATION, location)
        )
        val observer: MutableLiveData<UserGeoPoint> = MutableLiveData()
        firestore.collection(Constants.LOCATIONS_COLLECTION).document(username)
            .set(map)
            .addOnSuccessListener {
                Log.i("EventRepository", "Added User GeoPoint record to DB (chatroom_id: $chatroomId, location: $location)")
                observer.value = UserGeoPoint(username, chatroomId, eventIds, location)
            }
            .addOnFailureListener {
                throw it
            }
        return observer
    }
}