package com.example.hobbyfi.repositories

import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.hobbyfi.R
import com.example.hobbyfi.api.HobbyfiAPI
import com.example.hobbyfi.fetchers.NetworkBoundFetcher
import com.example.hobbyfi.models.data.Event
import com.example.hobbyfi.models.data.UserGeoPoint
import com.example.hobbyfi.persistence.HobbyfiDatabase
import com.example.hobbyfi.responses.CacheListResponse
import com.example.hobbyfi.responses.CacheResponse
import com.example.hobbyfi.responses.Response
import com.example.hobbyfi.responses.StartDateIdResponse
import com.example.hobbyfi.shared.Constants
import com.example.hobbyfi.shared.PrefConfig
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class EventRepository(
    prefConfig: PrefConfig, hobbyfiAPI: HobbyfiAPI,
    hobbyfiDatabase: HobbyfiDatabase, connectivityManager: ConnectivityManager
): CacheRepository(prefConfig, hobbyfiAPI, hobbyfiDatabase, connectivityManager) {
    private val _userGeoPoints: MutableLiveData<List<UserGeoPoint>> = MutableLiveData(arrayListOf())
    private val _userGeoPoint: MutableStateFlow<UserGeoPoint?> = MutableStateFlow(null)

    fun getEvents(chatroomId: Long): Flow<List<Event>?> {
        Log.i("EventRepository", "getEvents -> Getting current chatroom eventS!!!")
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
                    hobbyfiAPI.fetchEvents(
                        prefConfig.getAuthUserToken()!!,
                        chatroomId
                    )
                }, ::fetchFromNetwork)
            }
        }.asFlow()
    }

    // this method shouldn't be called unless specific event refresh is needed while user is in Google Maps
    fun getEvent(eventId: Long): Flow<Event?> {
        Log.i("EventRepository", "getEvent -> Getting current chatroom event!!!")
        return object : NetworkBoundFetcher<Event?, CacheResponse<Event>>() {
            override suspend fun saveNetworkResult(response: CacheResponse<Event>) {
                saveEvent(response.model)
            }

            override fun shouldFetch(cache: Event?): Boolean = true

            override suspend fun loadFromDb(): Flow<Event?> =
                hobbyfiDatabase.eventDao().getEventById(eventId)

            override suspend fun fetchFromNetwork(): CacheResponse<Event>? {
                Log.i("EventRepository", "Fetching event from network with id: ${eventId}!")
                return performAuthorisedRequest({
                    hobbyfiAPI.fetchEvent(
                        prefConfig.getAuthUserToken()!!,
                        eventId
                    )
                }, { fetchFromNetwork() })
            }
        }.asFlow()
    }

    suspend fun createEvent(name: String, description: String?,
                    date: String, lat: Double, long: Double, chatroomId: Long): StartDateIdResponse? {
        Log.i("EventRepository", "createEvent -> Creating chatroom event w/ data. " +
                "Name: $name, description: $description, date: $date, lat: $lat, long: $long")
        return performAuthorisedRequest({
            hobbyfiAPI.createEvent(
                prefConfig.getAuthUserToken()!!,
                chatroomId,
                name,
                description,
                date,
                lat,
                long
            )
        }, { createEvent(name, description, date, lat, long, chatroomId) })
    }

    suspend fun deleteEvent(eventId: Long): Response? {
        Log.i("EventRepository", "deleteEvent -> Deleting chatroom event with id $eventId!!!")

        return performAuthorisedRequest({
            hobbyfiAPI.deleteEvent(
                prefConfig.getAuthUserToken()!!,
                eventId
            )
        }, { deleteEvent(eventId) })
    }

    suspend fun deleteOldEvents(chatroomId: Long): CacheListResponse<Long>? {
        Log.i("EventRepository", "deleteEvent -> Deleting old events!!!")

        return performAuthorisedRequest({
            hobbyfiAPI.deleteOldEvents(
                prefConfig.getAuthUserToken()!!,
                chatroomId
            )
        }, { deleteOldEvents(chatroomId) })
    }

    suspend fun editEvent(eventUpdateFields: Map<String, String?>): Response? {
        Log.i("EventRepository", "editEvent -> Editing current chatroom event with fields: $eventUpdateFields")

        return performAuthorisedRequest({
            hobbyfiAPI.editEvent(
                prefConfig.getAuthUserToken()!!,
                eventUpdateFields.filterKeys { it != Constants.IMAGE } // do not send image
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

                val userChatroomIds = doc?.get(Constants.CHATROOM_ID) as List<Long>?
                val geoPoint = doc?.getGeoPoint(Constants.LOCATION)
                val eventIds = doc?.get(Constants.EVENT_IDS) as List<Long>?

                val userGeoPoint = if(userChatroomIds == null || geoPoint == null || eventIds == null || eventIds.isEmpty()) null
                    else UserGeoPoint(doc.id, userChatroomIds, eventIds, geoPoint)

                Log.i("EventRepository", "getEventUserGeoPoint -> Received user Geo Point: $userGeoPoint")
                _userGeoPoint.value = userGeoPoint
            }
        return _userGeoPoint
    }

    fun getEventUsersGeoPoint(eventId: Long, username: String?): MutableLiveData<List<UserGeoPoint>> {
        Log.i("EventRepository", "EventUserGeoPoint FETCHER username: $username")
        val query = firestore.collection(Constants.LOCATIONS_COLLECTION)
            .whereArrayContains(Constants.EVENT_IDS, eventId)

        username?.let {
            query.whereNotEqualTo(FieldPath.documentId(), username)
        }

        query.addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("EventRepository", "User GeoPoint Firestore listener error!", e)
                    throw e
                }

                val geoPoints = mutableListOf<UserGeoPoint?>()
                for (doc in snapshots!!) {
                    if(doc.id == username) {
                        continue
                    }

                    Log.i("EventRepository", "Received docs from getEventUserGeoPoint: ${doc.data}")

                    val userChatroomIds = doc?.get(Constants.CHATROOM_ID) as List<Long>?
                    val geoPoint = doc?.getGeoPoint(Constants.LOCATION)
                    val eventIds = doc?.get(Constants.EVENT_IDS) as List<Long>?

                    geoPoints.add(if(userChatroomIds == null || geoPoint == null || eventIds == null || eventIds.isEmpty()) null else
                        UserGeoPoint(doc.id, userChatroomIds, eventIds, geoPoint)
                    )
                }
                Log.i("EventRepository", "UserGeoPoints list: ${geoPoints}")
                _userGeoPoints.value = geoPoints.filterNotNull()
            }
        return _userGeoPoints
    }

    fun setEventUserGeoPoints(username: String, chatroomIds: List<Long>,
                              eventIds: List<Long>, location: GeoPoint): LiveData<UserGeoPoint?> {
        val observer: MutableLiveData<UserGeoPoint> = MutableLiveData()
        if(eventIds.isEmpty()) {
            firestore.collection(Constants.LOCATIONS_COLLECTION).document(username)
                .delete()
            observer.value = null
            return observer
        }

        val map = hashMapOf(
            Pair(Constants.CHATROOM_ID, chatroomIds),
            Pair(Constants.LOCATION, location),
            Pair(Constants.EVENT_IDS, eventIds),
        )

        firestore.collection(Constants.LOCATIONS_COLLECTION).document(username)
            .set(map)
            .addOnSuccessListener {
                Log.i("EventRepository", "Added User GeoPoint record to DB (chatroom_id: $chatroomIds, location: $location)")
                observer.value = UserGeoPoint(username, chatroomIds, eventIds, location)
            }
            .addOnFailureListener {
                throw FirebaseException(Constants.firestoreUpdateError)
            }
        return observer
    }
}