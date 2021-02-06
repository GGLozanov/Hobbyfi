package com.example.hobbyfi.viewmodels.chatroom

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.hobbyfi.intents.EventListIntent
import com.example.hobbyfi.intents.Intent
import com.example.hobbyfi.intents.UserGeoPointIntent
import com.example.hobbyfi.intents.UserListIntent
import com.example.hobbyfi.models.*
import com.example.hobbyfi.viewmodels.base.AuthChatroomHolderViewModel
import com.example.hobbyfi.repositories.EventRepository
import com.example.hobbyfi.shared.*
import com.example.hobbyfi.state.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kodein.di.generic.instance
import java.lang.IllegalStateException

@ExperimentalCoroutinesApi
class ChatroomActivityViewModel(
    application: Application,
    user: User?,
    chatroom: Chatroom?
) : AuthChatroomHolderViewModel(application, user, chatroom) {
    private val eventRepository: EventRepository by instance(tag = "eventRepository")
    private val prefConfig: PrefConfig by instance(tag = "prefConfig")

    private var _chatroomUsers: MutableLiveData<List<User>> = MutableLiveData(emptyList())
    val chatroomUsers: LiveData<List<User>> get() = _chatroomUsers

    private var _authEvents: MutableLiveData<List<Event>> = MutableLiveData()
    val authEvents: LiveData<List<Event>> get() = _authEvents

    private var _authUserGeoPoint: StateFlow<UserGeoPoint?> = MutableStateFlow(null)
    val authUserGeoPoint: StateFlow<UserGeoPoint?> get() = _authUserGeoPoint

    fun setAuthEvents(events: List<Event>?) {
        _authEvents.value = events
    }

    private val eventsStateIntent: StateIntent<EventListState, EventListIntent> = object : StateIntent<EventListState, EventListIntent>() {
        override val _state: MutableStateFlow<EventListState> = MutableStateFlow(EventListState.Idle)
    }
    val eventsState get() = eventsStateIntent.state

    suspend fun sendEventsIntent(intent: EventListIntent) = eventsStateIntent.sendIntent(intent)
    
    private val usersStateIntent: StateIntent<UserListState, UserListIntent> = object : StateIntent<UserListState, UserListIntent>() {
        override val _state: MutableStateFlow<UserListState> = MutableStateFlow(UserListState.Idle)
    }
    val usersState get() = usersStateIntent.state

    suspend fun sendUsersIntent(intent: UserListIntent) = usersStateIntent.sendIntent(intent)

    private val userGeoPointStateIntent: StateIntent<UserGeoPointState, UserGeoPointIntent> = object : StateIntent<UserGeoPointState, UserGeoPointIntent>() {
        override val _state: MutableStateFlow<UserGeoPointState> = MutableStateFlow(UserGeoPointState.Idle)
    }
    val userGeoPointState get() = userGeoPointStateIntent.state

    suspend fun sendUserGeoPointIntent(intent: UserGeoPointIntent) = userGeoPointStateIntent.sendIntent(intent)

    fun resetUserListState() = usersStateIntent.setState(UserListState.Idle)

    fun resetChatroomState() = chatroomStateIntent.setState(ChatroomState.Idle)

    fun resetEventListState() = eventsStateIntent.setState(EventListState.Idle)

    override fun handleIntent() {
        super.handleIntent()
        viewModelScope.launch {
            eventsStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is EventListIntent.AddAnEventCache -> {
                        Log.i("ChatroomActivityVM", "Add Event to List Intent caught")
                        saveEvent(it.event)
                        setAuthEvents((_authEvents.value ?: emptyList()) + it.event)
                        updateAndSaveChatroom(mapOf(Pair(Constants.EVENT_IDS, Constants.tagJsonConverter
                            .toJson(authChatroom.value!!.eventIds?.plus(it.event.id)))))
                    }
                    is EventListIntent.DeleteAnEventCache -> {
                        eventsStateIntent.setState(EventListState.Idle)
                        if(eventRepository.deleteEventCache(it.eventId)) {
                            setAuthEvents(_authEvents.value!!.filter { event -> event.id != it.eventId })
                            updateAndSaveChatroom(mapOf(Pair(Constants.EVENT_IDS, Constants.tagJsonConverter
                                .toJson(authChatroom.value!!.eventIds?.filter { eventId -> eventId != it.eventId }))))
                            eventsStateIntent.setState(EventListState.OnData.DeleteAnEventCacheResult(it.eventId))
                        }
                    }
                    is EventListIntent.DeleteOldEvents -> {
                        // TODO: wire up this with delete_old events in repo and API
                        deleteOldEvents()
                    }
                    is EventListIntent.DeleteEventsCache -> {
                        Log.i("ChatroomActivityVM", "Delete events cache intent received!")
                        deleteOldEventsCache(it.eventIds, true)
                    }
                    is EventListIntent.UpdateAnEventCache -> {
                        updateAndSaveEvent(it.eventUpdateFields)
                    }
                    is EventListIntent.FetchEvents -> {
                        fetchEvents()
                    }
                }
            }
        }
        viewModelScope.launch {
            usersStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is UserListIntent.AddAUserCache -> {
                        saveUser(it.user, false)
                        setCurrentUsers(chatroomUsers.value!! + it.user)
                    }
                    is UserListIntent.DeleteAUserCache -> {
                        deleteUserCache(it.userId, false)
                        setCurrentUsers(chatroomUsers.value!!.filter { user -> user.id != it.userId })
                    }
                    is UserListIntent.UpdateAUserCache -> {
                        // TODO: Update lastUsersFetchTime or something similar for Glide signature caching
                        // TODO: Check if user has image updated? That'd retrigger every image to be refetched on next VH onBind call
                        saveUser(chatroomUsers.value!!.find { user -> user.id ==
                                (it.userUpdateFields[Constants.ID] ?: error("User ID must not be null in saveUser call!")).toLong() }!!
                            .updateFromFieldMap(it.userUpdateFields), false)
                        setCurrentUsers(chatroomUsers.value!!)
                    }
                    is UserListIntent.FetchUsers -> {
                        fetchUsers()
                    }
                }
            }
        }
        viewModelScope.launch {
            userGeoPointStateIntent.intentAsFlow().collectLatest {
                when(it) {
                    is UserGeoPointIntent.FetchAuthUserGeoPoint -> {
                        fetchAuthUserGeoPoint()
                    }
                    else -> throw Intent.InvalidIntentException()
                }
            }
        }
    }

    init {
        handleIntent()
    }

    private fun fetchAuthUserGeoPoint() {
        userGeoPointStateIntent.setState(UserGeoPointState.Loading)

        userGeoPointStateIntent.setState(try {
            _authUserGeoPoint = eventRepository.getEventUserGeoPoint(authUser.value!!.name)

            UserGeoPointState.OnData.OnAuthUserGeoPointResult(
                _authUserGeoPoint
            ) // TODO: Do something with state (collect it, at least)
        } catch(ex: Exception) {
            ex.printStackTrace()
            UserGeoPointState.Error(
                ex.message, shouldReauth = ex.isCritical
            )
        })
    }

    private suspend fun fetchUsers() {
        usersStateIntent.setState(UserListState.Loading)

        userRepository.getChatroomUsers(
            authChatroom.value!!.id,
            prefConfig.getAuthUserIdFromToken()
        ).catch { e ->
            usersStateIntent.setState(
                UserListState.Error(
                    e.message,
                    e.isCritical
                )
            )
        }.collectLatest {
            if(it != null) {
                Log.i("ChatroomActivityVM", "Collecting new users from SSOT cache!!! $it")
                setCurrentUsers(it)
                usersStateIntent.setState(
                    UserListState.OnData.UsersResult(
                        it
                    )
                )
            }
        }
    }

    private suspend fun fetchEvents() {
        eventsStateIntent.setState(EventListState.Loading)

        // run collection on another coroutine to avoid conflicts with intent collection
        // that NEEDS observation but will OVERSHADOW it in the case of an incoming intent
        // this SHOULD be the practice for ALL flow observations in VM, so TODO to avoid future mishaps
        // (there haven't been any as of now but it's good to keep it in mind)
        viewModelScope.launch {
            eventRepository.getEvents(_authChatroom.value!!.id).catch { e ->
                eventsStateIntent.setState(
                    EventListState.Error(
                        e.message,
                        e.isCritical
                    )
                )
            }.collectLatest {
                Log.i("ChatroomActivityVM", "Events collected: ${it}")
                if(it != null) {
                    _authEvents.value = it
                    eventsStateIntent.setState(
                        EventListState.OnData.EventsResult(
                            it
                        )
                    )
                }
            }
        }
    }

    private suspend fun updateAndSaveEvent(eventFields: Map<String?, String?>) {
        val updatedEvent = _authEvents.value!!.find { it.id == (eventFields[Constants.ID]
                ?: error("Event ID must not be null in UpdateAnEventCache Intent!")).toLong() }!!
            .updateFromFieldMap(eventFields)
        saveEvent(updatedEvent)
        _authEvents.value = _authEvents.value!!.replaceOrAdd(updatedEvent, { it.id == updatedEvent.id })
    }

    private suspend fun saveEvent(event: Event) {
        eventRepository.saveEvent(event)
    }

    private suspend fun deleteOldEvents() {
        eventsStateIntent.setState(EventListState.Loading)

        eventsStateIntent.setState(try {
            val response = eventRepository.deleteOldEvents()
                ?: throw IllegalStateException(Constants.invalidStateError)

            deleteOldEventsCache(response.modelList, false)

            EventListState.OnData.DeleteOldEventsResult(response.modelList)
        } catch(ex: Exception) {
            ex.printStackTrace()
            EventListState.Error(
                ex.message,
                shouldReauth = ex.isCritical
            )
        })
    }

    private suspend fun deleteUserCache(id: Long, shouldWritePrefTime: Boolean = true) {
        // TODO: Add setState bool?
        userRepository.deleteUserCache(id, shouldWritePrefTime)
    }

    private suspend fun deleteOldEventsCache(eventIds: List<Long>, setState: Boolean) {
        val success = eventRepository.deleteEventsCache(eventIds)
        if(success) {
            updateAndSaveChatroom(mapOf(
                Pair(Constants.EVENT_IDS,
                    Constants.tagJsonConverter.toJson(authChatroom.value!!.eventIds?.filter { !eventIds.contains(it) }))
            ))
        }

        if(setState) {
            eventsStateIntent.setState(if(success)
                EventListState.OnData.DeleteEventsCacheResult(eventIds)
            else EventListState.Error(Constants.cacheDeletionError))
        }
    }

    private fun setCurrentUsers(users: List<User>) {
        _chatroomUsers.value = users
    }

    var _consumedEventDeepLink: Boolean = false
    val consumedEventDeepLink get() = _consumedEventDeepLink

    fun setConsumedEventDeepLink(consumed: Boolean) {
        _consumedEventDeepLink = consumed
    }

    private var _currentLinkProperties: JSONObject? = null
    val currentLinkProperties get() = _currentLinkProperties
    fun setCurrentLinkProperties(linkProps: JSONObject?) {
        _currentLinkProperties = linkProps
    }

    fun forceIsAuthUserOwnerObservation() {
        isAuthUserChatroomOwner.forceObserve()
    }
}